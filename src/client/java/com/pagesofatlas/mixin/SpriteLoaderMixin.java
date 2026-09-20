package com.pagesofatlas.mixin;
import com.pagesofatlas.AtlasStitcher;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.concurrent.Executor;

@Mixin(SpriteLoader.class)
public abstract class SpriteLoaderMixin {
    @Shadow @Final private ResourceLocation location;
    @Shadow @Final private int maxSupportedTextureSize;
    @Inject(method = "stitch", at = @At("HEAD"), cancellable = true)
    private void pagesofatlas$stitch(List<SpriteContents> sprites, int mip, Executor executor, CallbackInfoReturnable<SpriteLoader.Preparations> cir) {
        var result = AtlasStitcher.stitch(location, maxSupportedTextureSize, sprites, mip, executor);
        if (result != null) cir.setReturnValue(result);
    }
}
