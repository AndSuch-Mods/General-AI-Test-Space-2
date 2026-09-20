package com.pagesofatlas.mixin;
import com.pagesofatlas.*;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {
    @Shadow @Final private List<String> samplerNames;
    @Shadow @Final private Map<String, Object> samplerMap;
    @Shadow @Final private int programId;
    @Inject(method = "apply", at = @At("RETURN"))
    private void pagesofatlas$bind(CallbackInfo ci) {
        int next = samplerNames.size();
        for (String name : ShaderPaging.SAMPLERS) {
            Object value = samplerMap.get(name);
            int texture = value instanceof Integer id ? id : value instanceof AbstractTexture t ? t.getId() : value instanceof RenderTarget t ? t.getColorTextureId() : -1;
            if (texture >= 0) next = ShaderBindings.bind(programId, name, texture, next);
        }
    }
}
