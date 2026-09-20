package com.pagesofatlas.mixin;
import com.pagesofatlas.*;
import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

// These public Blaze3D entry points retain their names in the 1.21.1 client JAR.
@Mixin(value = GlStateManager.class, remap = false)
public abstract class GlStateManagerMixin {
    @Inject(method = "_texSubImage2D", at = @At("HEAD"), cancellable = true)
    private static void pagesofatlas$upload(int target, int level, int x, int y, int w, int h, int format, int type, long data, CallbackInfo ci) {
        if (PagedTextures.upload(target, level, x, y, w, h, format, type, data)) ci.cancel();
    }
    @Inject(method = "_deleteTexture", at = @At("HEAD"))
    private static void pagesofatlas$delete(int id, CallbackInfo ci) { PagedTextures.release(id); }
    @Inject(method = "_deleteTextures", at = @At("HEAD"))
    private static void pagesofatlas$deleteMany(int[] ids, CallbackInfo ci) { for (int id : ids) PagedTextures.release(id); }
    @Inject(method = "_texParameter(III)V", at = @At("RETURN"))
    private static void pagesofatlas$parameterInt(int target, int name, int value, CallbackInfo ci) { PagedTextures.parameter(target, name, value); }
    @Inject(method = "_texParameter(IIF)V", at = @At("RETURN"))
    private static void pagesofatlas$parameterFloat(int target, int name, float value, CallbackInfo ci) { PagedTextures.parameter(target, name, value); }
    @ModifyVariable(method = "glShaderSource", at = @At("HEAD"), argsOnly = true)
    private static List<String> pagesofatlas$shader(List<String> sources, int shader, List<String> original) {
        boolean fragment = org.lwjgl.opengl.GL20.glGetShaderi(shader, org.lwjgl.opengl.GL20.GL_SHADER_TYPE) == org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
        return List.of(ShaderPaging.patch(String.join("", sources), fragment, List.of("Sampler0", "u_BlockTex")));
    }
}
