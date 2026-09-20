package com.pagesofatlas;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.gl.state.ValueUpdateNotifier;
import java.util.function.IntSupplier;

public final class IrisPagingSamplers {
    private static final ListenerGroup DIFFUSE = new ListenerGroup();
    private static int changingTexture = -1;
    private IrisPagingSamplers() {}
    public static int diffuse() { return changingTexture >= 0 ? changingTexture : RenderSystem.getShaderTexture(0); }
    public static void textureChanged(int texture) {
        int previous = changingTexture;
        changingTexture = texture;
        try { DIFFUSE.fire(); ShaderBindings.irisGrids(texture); }
        finally { changingTexture = previous; }
    }
    public static void addDiffusePages(SamplerHolder samplers, IntSupplier root, String name) {
        addPages(samplers, root, name, DIFFUSE);
    }
    public static boolean addMaterialFamily(SamplerHolder samplers, IntSupplier root, ValueUpdateNotifier notifier, String[] names) {
        // Iris notifiers each hold ONE listener. Separate slots fan out the base and
        // extra pages together, and are detached by Iris's normal program lifecycle.
        ListenerGroup group = new ListenerGroup(notifier::setListener);
        boolean used = samplers.addDynamicSampler(root, group.slot()::accept, names);
        for (String name : names) addPages(samplers, root, name, group);
        return used;
    }
    private static void addPages(SamplerHolder samplers, IntSupplier root, String name, ListenerGroup group) {
        for (int page = 1; page < 4; page++) {
            final int index = page;
            samplers.addDynamicSampler(() -> PagedTextures.page(root.getAsInt(), index), group.slot()::accept, ShaderPaging.prefix(name) + page);
        }
    }
}
