package com.pagesofatlas.mixin;
import com.pagesofatlas.IrisPagingSamplers;
import net.irisshaders.iris.pbr.TextureTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TextureTracker.class, remap = false)
public abstract class IrisTextureChangeMixin {
    @Shadow private boolean lockBindCallback;
    @Inject(method = "onSetShaderTexture", at = @At("RETURN"), remap = false)
    private void pagesofatlas$changed(int unit, int texture, CallbackInfo ci) {
        if (unit == 0 && !lockBindCallback) IrisPagingSamplers.textureChanged(texture);
    }
}
