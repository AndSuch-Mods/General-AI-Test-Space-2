package com.pagesofatlas.mixin;
import com.pagesofatlas.ShaderPaging;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

@Mixin(value = TransformPatcher.class, remap = false)
public abstract class IrisTransformMixin {
    // Composite and DH programs are deliberately not rewritten: they have different samplers.
    @Inject(method = {"patchSodium", "patchVanilla"}, at = @At("RETURN"), cancellable = true, remap = false)
    private static void pagesofatlas$patch(CallbackInfoReturnable<Map<?, String>> cir) {
        Map<Object, String> output = new HashMap<>();
        cir.getReturnValue().forEach((stage, source) -> output.put(stage, source == null ? null :
                ShaderPaging.patch(source, stage.toString().contains("FRAGMENT"), ShaderPaging.SAMPLERS)));
        cir.setReturnValue(output);
    }
}
