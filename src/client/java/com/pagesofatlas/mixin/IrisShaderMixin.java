package com.pagesofatlas.mixin;
import com.pagesofatlas.ShaderBindings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.irisshaders.iris.pipeline.programs.SodiumShader", remap = false)
public abstract class IrisShaderMixin {
    @Inject(method = "setupState", at = @At("RETURN"), remap = false)
    private void pagesofatlas$grid(CallbackInfo ci) { ShaderBindings.irisGrids(); }
}
