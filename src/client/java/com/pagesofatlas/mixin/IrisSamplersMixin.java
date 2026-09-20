package com.pagesofatlas.mixin;
import com.pagesofatlas.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.IntSupplier;

@Mixin(value = IrisSamplers.class, remap = false)
public abstract class IrisSamplersMixin {
    @Inject(method = "addLevelSamplers", at = @At("RETURN"), remap = false)
    private static void pagesofatlas$samplers(SamplerHolder samplers, WorldRenderingPipeline pipeline, AbstractTexture white,
                                             boolean texture, boolean lightmap, boolean overlay, CallbackInfo ci) {
        for (String name : ShaderPaging.SAMPLERS) {
            IntSupplier root = name.equals("normals") ? pipeline::getCurrentNormalTexture : name.equals("specular") ? pipeline::getCurrentSpecularTexture : () -> texture ? RenderSystem.getShaderTexture(0) : white.getId();
            for (int page = 1; page < 4; page++) {
                final int index = page;
                samplers.addDynamicSampler(() -> PagedTextures.page(root.getAsInt(), index), ShaderPaging.prefix(name) + page);
            }
        }
    }
}
