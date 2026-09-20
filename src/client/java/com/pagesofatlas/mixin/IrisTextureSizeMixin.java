package com.pagesofatlas.mixin;
import com.pagesofatlas.PagedTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.uniforms.CommonUniforms;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CommonUniforms.class, remap = false)
public abstract class IrisTextureSizeMixin {
    // Exact Iris 1.8.8 supplier for gtextureSize; atlasSize already reads logical TextureAtlas fields.
    @Inject(method = "lambda$addDynamicUniforms$2", at = @At("RETURN"), cancellable = true, remap = false)
    private static void pagesofatlas$logicalSize(CallbackInfoReturnable<Vector2i> cir) {
        var family = PagedTextures.family(RenderSystem.getShaderTexture(0));
        if (family != null) cir.setReturnValue(new Vector2i(family.layout().logicalWidth(), family.layout().logicalHeight()));
    }
}
