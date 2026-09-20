package com.pagesofatlas.mixin;
import com.pagesofatlas.PagedTextures;
import com.mojang.blaze3d.platform.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureUtil.class)
public abstract class TextureUtilMixin {
    @Inject(method = "prepareImage(Lcom/mojang/blaze3d/platform/NativeImage$InternalGlFormat;IIII)V", at = @At("HEAD"), cancellable = true)
    private static void pagesofatlas$allocate(NativeImage.InternalGlFormat format, int id, int mip, int w, int h, CallbackInfo ci) {
        if (PagedTextures.allocate(format, id, mip)) ci.cancel();
    }
}
