package com.pagesofatlas.mixin;
import com.pagesofatlas.ShaderBindings;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderTextureSlot;
import net.caffeinemc.mods.sodium.client.util.TextureUtil;
import org.lwjgl.opengl.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface", remap = false)
public abstract class SodiumShaderMixin {
    @Inject(method = "setupState", at = @At("RETURN"), remap = false)
    private void pagesofatlas$bind(CallbackInfo ci) {
        ShaderBindings.bind(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM), "u_BlockTex",
                TextureUtil.getBlockTextureId(), ChunkShaderTextureSlot.values().length);
    }
}
