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
import net.irisshaders.iris.gl.state.ValueUpdateNotifier;

@Mixin(value = IrisSamplers.class, remap = false)
public abstract class IrisSamplersMixin {
    @Redirect(method = "addLevelSamplers", at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/gl/sampler/SamplerHolder;addDynamicSampler(Ljava/util/function/IntSupplier;Lnet/irisshaders/iris/gl/state/ValueUpdateNotifier;[Ljava/lang/String;)Z"), require = 2, allow = 2, remap = false)
    private static boolean pagesofatlas$materials(SamplerHolder samplers, IntSupplier root, ValueUpdateNotifier notifier, String[] names) {
        return IrisPagingSamplers.addMaterialFamily(samplers, root, notifier, names);
    }
    @Inject(method = "addLevelSamplers", at = @At("RETURN"), remap = false)
    private static void pagesofatlas$samplers(SamplerHolder samplers, WorldRenderingPipeline pipeline, AbstractTexture white,
                                             boolean texture, boolean lightmap, boolean overlay, CallbackInfo ci) {
        for (String name : ShaderPaging.SAMPLERS) {
            if (name.equals("normals") || name.equals("specular")) continue;
            IrisPagingSamplers.addDiffusePages(samplers, () -> texture ? IrisPagingSamplers.diffuse() : white.getId(), name);
        }
    }
}
