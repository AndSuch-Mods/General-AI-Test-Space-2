package com.pagesofatlas.mixin;
import com.pagesofatlas.PagedTextures;
import net.minecraft.client.renderer.texture.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasMixin extends AbstractTexture {
    @Inject(method = "upload", at = @At("HEAD"))
    private void pagesofatlas$begin(SpriteLoader.Preparations preparations, CallbackInfo ci) { PagedTextures.begin(getId(), preparations); }
}
