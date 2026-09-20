package com.pagesofatlas;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.SpriteLoader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import java.util.*;
import java.util.function.IntConsumer;

/** Render-thread-owned GL resources. Preparations cross the reload worker boundary. */
public final class PagedTextures {
    private static final Map<SpriteLoader.Preparations, PageLayout> PENDING = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Integer, Family> FAMILIES = new HashMap<>();
    private static final Map<Integer, String> PBR_FAILURES = new HashMap<>();
    public static boolean internal;
    public record Family(int root, PageLayout layout, int[] ids) {}
    private PagedTextures() {}
    public static void stage(SpriteLoader.Preparations preparations, PageLayout layout) { PENDING.put(preparations, layout); }
    public static void begin(int root, SpriteLoader.Preparations preparations) {
        associate(root, PENDING.remove(preparations));
    }
    public static Family family(int id) { return FAMILIES.get(id); }
    public static int page(int root, int page) {
        checkPbr(root);
        Family f = family(root);
        return f == null || page >= f.ids.length ? root : f.ids[page];
    }
    public static void associate(int root, PageLayout layout) {
        RenderSystem.assertOnRenderThreadOrInit();
        release(root);
        if (layout != null) {
            int[] ids = new int[layout.pages()];
            ids[0] = root;
            for (int i = 1; i < ids.length; i++) ids[i] = TextureUtil.generateTextureId();
            Family family = new Family(root, layout, ids);
            for (int id : ids) FAMILIES.put(id, family);
        }
    }
    public static void release(int root) {
        PBR_FAILURES.remove(root);
        Family f = family(root);
        if (f == null || f.root != root) return;
        for (int id : f.ids) FAMILIES.remove(id);
        for (int id : f.ids) if (id != root) TextureUtil.releaseTextureId(id);
    }
    public static void pbrFailed(int diffuse) {
        PBR_FAILURES.put(diffuse, "Pages of Atlas: Iris could not upload aligned PBR pages for atlas " + diffuse + "; see preceding Iris error. Rendering with incomplete materials is refused.");
    }
    public static void checkPbr(int diffuse) {
        if (PBR_FAILURES.containsKey(diffuse)) throw new IllegalStateException(PBR_FAILURES.get(diffuse));
    }
    public static boolean allocate(NativeImage.InternalGlFormat format, int id, int mip) {
        Family f = family(id);
        if (internal || f == null) return false;
        internal = true;
        try {
            int previousError = GL11.glGetError();
            if (previousError != GL11.GL_NO_ERROR) {
                PagesOfAtlasClient.LOGGER.warn("GL error 0x{} existed before atlas {} allocation", Integer.toHexString(previousError), id);
                while (GL11.glGetError() != GL11.GL_NO_ERROR) { /* clear pre-existing flags once at allocation */ }
            }
            for (int physical : f.ids) {
                TextureUtil.prepareImage(format, physical, mip, f.layout.width(), f.layout.height());
                AllocationChecks.validate(f.root, physical, f.layout, mip, (level, axis) ->
                        GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, level, axis == 0 ? GL11.GL_TEXTURE_WIDTH : GL11.GL_TEXTURE_HEIGHT), GL11::glGetError);
            }
            GlStateManager._bindTexture(id);
        } catch (RuntimeException | Error failure) {
            PagesOfAtlasClient.LOGGER.error("Atlas {} allocation failed; releasing extra pages", id, failure);
            release(f.root);
            throw failure;
        } finally { internal = false; }
        PagesOfAtlasClient.LOGGER.info("Allocated atlas {}: {} physical {}x{} pages, mip {}, logical {}x{}, fragment sampler limit {}",
                id, f.ids.length, f.layout.width(), f.layout.height(), mip, f.layout.logicalWidth(), f.layout.logicalHeight(), GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS));
        return true;
    }
    /** Iris DSA and fallback paths both bypass GlStateManager's parameter methods. */
    public static void irisParameter(int root, int target, IntConsumer apply) {
        Family f = family(root);
        if (internal || target != GL11.GL_TEXTURE_2D || f == null) return;
        int saved = GlStateManager._getInteger(GL11.GL_TEXTURE_BINDING_2D);
        internal = true;
        try { for (int id : f.ids) if (id != root) apply.accept(id); }
        finally { GlStateManager._bindTexture(saved); internal = false; }
    }
    public static boolean upload(int target, int level, int x, int y, int w, int h, int format, int type, long address) {
        if (target != GL11.GL_TEXTURE_2D) return false;
        int bound = GlStateManager._getInteger(GL11.GL_TEXTURE_BINDING_2D);
        Family f = family(bound);
        if (f == null) return false;
        PageLayout.Upload upload = f.layout.upload(level, x, y, w, h);
        GlStateManager._bindTexture(f.ids[upload.page()]);
        GL11.glTexSubImage2D(target, level, upload.x(), upload.y(), w, h, format, type, address);
        // Restore the original family binding, including between mip levels.
        GlStateManager._bindTexture(bound);
        return true;
    }
    public static void parameter(int target, int name, Number value) {
        if (internal || target != GL11.GL_TEXTURE_2D) return;
        int bound = GlStateManager._getInteger(GL11.GL_TEXTURE_BINDING_2D);
        Family f = family(bound);
        if (f == null) return;
        internal = true;
        try {
            for (int id : f.ids) if (id != bound) {
                GlStateManager._bindTexture(id);
                if (value instanceof Float) GlStateManager._texParameter(target, name, value.floatValue());
                else GlStateManager._texParameter(target, name, value.intValue());
            }
            GlStateManager._bindTexture(bound);
        } finally { internal = false; }
    }
}
