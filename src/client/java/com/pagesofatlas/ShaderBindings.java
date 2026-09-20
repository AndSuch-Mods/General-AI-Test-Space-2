package com.pagesofatlas;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.Iris;
import org.lwjgl.opengl.*;
import java.util.*;

public final class ShaderBindings {
    private static final Map<Integer, Map<String, Locations>> PROGRAMS = new HashMap<>();
    private static int samplerLimit;
    private static final class Locations {
        final int grid;
        final int[] pages = new int[3], units = {-1, -1, -1};
        int lastGrid = -1;
        Locations(int program, String sampler) {
            String prefix = ShaderPaging.prefix(sampler);
            grid = GL20.glGetUniformLocation(program, prefix + "_grid");
            for (int i = 0; i < 3; i++) pages[i] = grid < 0 ? -1 : GL20.glGetUniformLocation(program, prefix + (i + 1));
        }
    }
    private ShaderBindings() {}
    public static void forget(int program) { PROGRAMS.remove(program); }
    public static int samplerLimit() {
        if (samplerLimit == 0) samplerLimit = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
        return samplerLimit;
    }
    private static Locations locations(int program, String sampler) {
        return PROGRAMS.computeIfAbsent(program, id -> new HashMap<>()).computeIfAbsent(sampler, name -> new Locations(program, name));
    }
    public static void grid(int program, String sampler, int texture) {
        if (program == 0) return;
        Locations locations = locations(program, sampler);
        if (locations.grid < 0) return;
        var f = PagedTextures.family(texture);
        int x = f == null ? 1 : f.layout().columns(), y = f == null ? 1 : f.layout().rows();
        int key = x * 4 + y;
        if (locations.lastGrid != key) {
            GL20.glUniform2i(locations.grid, x, y);
            locations.lastGrid = key;
        }
    }
    public static void irisGrids() { irisGrids(RenderSystem.getShaderTexture(0)); }
    public static void irisGrids(int diffuse) {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (program == 0) return;
        PagedTextures.checkPbr(diffuse);
        var pipeline = Iris.getPipelineManager().getPipelineNullable();
        for (String sampler : ShaderPaging.SAMPLERS) {
            int root = pipeline != null && sampler.equals("normals") ? pipeline.getCurrentNormalTexture()
                    : pipeline != null && sampler.equals("specular") ? pipeline.getCurrentSpecularTexture() : diffuse;
            grid(program, sampler, root);
        }
    }
    public static int bind(int program, String sampler, int texture, int firstUnit) {
        Locations locations = locations(program, sampler);
        if (locations.grid < 0) return firstUnit;
        grid(program, sampler, texture);
        int saved = GlStateManager._getActiveTexture();
        try {
            for (int page = 1; page < 4; page++) {
                int uniform = locations.pages[page - 1];
                if (uniform < 0) continue;
                if (firstUnit >= samplerLimit()) throw new IllegalStateException("Pages of Atlas: shader " + program + " sampler " + sampler + " exceeds fragment texture-unit limit " + samplerLimit());
                GlStateManager._activeTexture(GL13.GL_TEXTURE0 + firstUnit);
                GlStateManager._bindTexture(PagedTextures.page(texture, page));
                if (locations.units[page - 1] != firstUnit) {
                    GL20.glUniform1i(uniform, firstUnit);
                    locations.units[page - 1] = firstUnit;
                }
                firstUnit++;
            }
        } finally { GlStateManager._activeTexture(saved); }
        return firstUnit;
    }
}
