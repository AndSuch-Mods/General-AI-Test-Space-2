package com.pagesofatlas.mixin;
import com.pagesofatlas.ShaderBindings;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlProgram.class, remap = false)
public abstract class SodiumProgramMixin {
    @Inject(method = "delete", at = @At("HEAD"), remap = false)
    private void pagesofatlas$forget(CallbackInfo ci) { ShaderBindings.forget(((GlProgram<?>)(Object)this).handle()); }
}
