package com.pagesofatlas.mixin;
import com.pagesofatlas.PagedTextures;
import net.irisshaders.iris.pbr.util.TextureManipulationUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TextureManipulationUtil.class, remap = false)
public abstract class IrisFillMixin {
    @Unique private static boolean pagesofatlas$filling;
    @Inject(method = "fillWithColor", at = @At("RETURN"), remap = false)
    private static void pagesofatlas$fill(int root, int mip, int rgba, CallbackInfo ci) {
        var family = PagedTextures.family(root);
        if (pagesofatlas$filling || family == null) return;
        pagesofatlas$filling = true;
        try { for (int id : family.ids()) if (id != root) TextureManipulationUtil.fillWithColor(id, mip, rgba); }
        finally { pagesofatlas$filling = false; }
    }
}
