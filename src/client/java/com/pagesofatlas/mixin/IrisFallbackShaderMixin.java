package com.pagesofatlas.mixin;
import com.pagesofatlas.ShaderBindings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.pipeline.programs.FallbackShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallbackShader.class)
public abstract class IrisFallbackShaderMixin {
    @Inject(method = "apply", at = @At("RETURN"))
    private void pagesofatlas$bind(CallbackInfo ci) {
        ShaderBindings.bind(((FallbackShader)(Object)this).getId(), "gtexture", RenderSystem.getShaderTexture(0), 3);
    }
}
