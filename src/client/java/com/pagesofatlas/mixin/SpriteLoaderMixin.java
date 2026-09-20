package com.pagesofatlas.mixin;
import com.pagesofatlas.*;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.texture.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteLoader.class)
public abstract class SpriteLoaderMixin {
    @Redirect(method = "stitch", at = @At(value = "NEW", target = "net/minecraft/client/renderer/texture/Stitcher"))
    private Stitcher<SpriteContents> pagesofatlas$packer(int w, int h, int mip) { return new AtlasStitcher(w, h, mip); }

    // Both max calls preserve old atlas dimensions in vanilla. Always use this reload's
    // packed extent, including multi-page -> one-page transitions; no extra pages for small packs.
    @Redirect(method = "stitch", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"), require = 2, allow = 2)
    private int pagesofatlas$freshExtent(int packed, int previous) { return packed; }

    @Inject(method = "stitch", at = @At("RETURN"))
    private void pagesofatlas$stage(CallbackInfoReturnable<SpriteLoader.Preparations> cir,
                                   @Local Stitcher<SpriteContents> stitcher) {
        if (stitcher instanceof AtlasStitcher paged) {
            var result = cir.getReturnValue();
            if (paged.layout().pages() > 1) PagedTextures.stage(result, paged.layout());
            PagesOfAtlasClient.LOGGER.info("Planned atlas: {} sprites, {} physical {}x{} pages, logical {}x{}",
                    result.regions().size(), paged.layout().pages(), paged.layout().width(), paged.layout().height(), result.width(), result.height());
        }
    }
}
