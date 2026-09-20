package com.pagesofatlas.mixin;
import com.pagesofatlas.PagedTextures;
import net.irisshaders.iris.gl.IrisRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IrisRenderSystem.class, remap = false)
public abstract class IrisTextureParametersMixin {
    @Inject(method = "texParameteri", at = @At("RETURN"), remap = false)
    private static void pagesofatlas$int(int root, int target, int name, int value, CallbackInfo ci) {
        PagedTextures.irisParameter(root, target, id -> IrisRenderSystem.texParameteri(id, target, name, value));
    }
    @Inject(method = "texParameterf", at = @At("RETURN"), remap = false)
    private static void pagesofatlas$float(int root, int target, int name, float value, CallbackInfo ci) {
        PagedTextures.irisParameter(root, target, id -> IrisRenderSystem.texParameterf(id, target, name, value));
    }
    @Inject(method = "texParameteriv", at = @At("RETURN"), remap = false)
    private static void pagesofatlas$vector(int root, int target, int name, int[] value, CallbackInfo ci) {
        PagedTextures.irisParameter(root, target, id -> IrisRenderSystem.texParameteriv(id, target, name, value));
    }
}
