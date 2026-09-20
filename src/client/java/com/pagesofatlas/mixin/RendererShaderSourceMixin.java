package com.pagesofatlas.mixin;
import com.pagesofatlas.ShaderPaging;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderWorkarounds", remap = false)
public abstract class RendererShaderSourceMixin {
    @ModifyVariable(method = "safeShaderSource", at = @At("HEAD"), argsOnly = true, remap = false)
    private static CharSequence pagesofatlas$source(CharSequence source, int shader, CharSequence original) {
        boolean fragment = org.lwjgl.opengl.GL20.glGetShaderi(shader, org.lwjgl.opengl.GL20.GL_SHADER_TYPE) == org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
        return ShaderPaging.patch(source.toString(), fragment, java.util.List.of("u_BlockTex"));
    }
}
