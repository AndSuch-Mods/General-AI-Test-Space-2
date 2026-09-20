package com.pagesofatlas.mixin;
import com.pagesofatlas.PagedTextures;
import net.irisshaders.iris.pbr.texture.PBRAtlasTexture;
import net.minecraft.client.renderer.texture.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = PBRAtlasTexture.class, remap = false)
public abstract class IrisPbrAtlasMixin extends AbstractTexture {
    @Shadow @Final protected TextureAtlas atlasTexture;
    @Inject(method = "upload", at = @At("HEAD"), remap = false)
    private void pagesofatlas$associate(int w, int h, int mip, CallbackInfo ci) {
        var diffuse = PagedTextures.family(atlasTexture.getId());
        PagedTextures.associate(getId(), diffuse == null ? null : diffuse.layout());
    }
    @Inject(method = "tryUpload", at = @At("RETURN"), remap = false)
    private void pagesofatlas$check(int w, int h, int mip, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && PagedTextures.family(atlasTexture.getId()) != null) {
            // Iris catches loader exceptions; retain an error checked again outside that catch at draw setup.
            PagedTextures.pbrFailed(atlasTexture.getId());
            throw new IllegalStateException("Pages of Atlas: Iris failed to upload a paged PBR atlas; refusing diffuse-only fallback");
        }
    }
    @Inject(method = "close", at = @At("RETURN"), remap = false)
    private void pagesofatlas$close(CallbackInfo ci) { PagedTextures.release(getId()); }
}
