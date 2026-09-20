package com.pagesofatlas;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.*;

public final class ShaderBindings {
    private ShaderBindings() {}
    public static void grid(int program, String sampler, int texture) {
        int uniform = GL20.glGetUniformLocation(program, ShaderPaging.prefix(sampler) + "_grid");
        if (uniform < 0) return;
        var f = PagedTextures.family(texture);
        GL20.glUniform2i(uniform, f == null ? 1 : f.layout().columns(), f == null ? 1 : f.layout().rows());
    }
    public static void irisGrids() {
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int diffuse = RenderSystem.getShaderTexture(0);
        PagedTextures.checkPbr(diffuse);
        for (String sampler : ShaderPaging.SAMPLERS) grid(program, sampler, diffuse);
    }
    public static int bind(int program, String sampler, int texture, int firstUnit) {
        int location = GL20.glGetUniformLocation(program, ShaderPaging.prefix(sampler) + "_grid");
        if (location < 0) return firstUnit;
        grid(program, sampler, texture);
        int saved = GlStateManager._getActiveTexture();
        try {
            for (int page = 1; page < 4; page++) {
                int uniform = GL20.glGetUniformLocation(program, ShaderPaging.prefix(sampler) + page);
                if (uniform < 0) continue;
                if (firstUnit >= GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS)) throw new IllegalStateException("Pages of Atlas: shader exceeds fragment texture-unit limit");
                GlStateManager._activeTexture(GL13.GL_TEXTURE0 + firstUnit);
                GlStateManager._bindTexture(PagedTextures.page(texture, page));
                GL20.glUniform1i(uniform, firstUnit++);
            }
        } finally { GlStateManager._activeTexture(saved); }
        return firstUnit;
    }
}
