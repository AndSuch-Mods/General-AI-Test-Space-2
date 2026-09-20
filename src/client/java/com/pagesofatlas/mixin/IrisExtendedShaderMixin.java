package com.pagesofatlas.mixin;
import com.pagesofatlas.ShaderBindings;
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExtendedShader.class)
public abstract class IrisExtendedShaderMixin {
    @Inject(method = "apply", at = @At("RETURN"))
    private void pagesofatlas$grid(CallbackInfo ci) { ShaderBindings.irisGrids(); }
}
