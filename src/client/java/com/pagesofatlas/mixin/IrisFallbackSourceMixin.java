package com.pagesofatlas.mixin;
import com.pagesofatlas.ShaderPaging;
import net.irisshaders.iris.pipeline.fallback.ShaderSynthesizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = ShaderSynthesizer.class, remap = false)
public abstract class IrisFallbackSourceMixin {
    @Inject(method = "fsh", at = @At("RETURN"), cancellable = true, remap = false)
    private static void pagesofatlas$source(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ShaderPaging.patch(cir.getReturnValue(), true, List.of("gtexture")));
    }
}
