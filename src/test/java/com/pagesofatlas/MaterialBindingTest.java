package com.pagesofatlas;

import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.gl.state.ValueUpdateNotifier;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.IntSupplier;
import static org.junit.jupiter.api.Assertions.*;

class MaterialBindingTest {
    record Binding(IntSupplier texture, ValueUpdateNotifier notifier, String name) {}
    @Test void actualRegistrationUpdatesBaseAndPagesWithoutShaderReapplication() throws Exception {
        var bindings = new ArrayList<Binding>();
        SamplerHolder holder = (SamplerHolder)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{SamplerHolder.class}, (proxy, method, args) -> {
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            if (method.getName().equals("addDynamicSampler") && args.length == 5) {
                bindings.add(new Binding((IntSupplier)args[1], (ValueUpdateNotifier)args[2], ((String[])args[4])[0]));
                return true;
            }
            throw new AssertionError(method.toString());
        });
        var field = PagedTextures.class.getDeclaredField("FAMILIES"); field.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Integer,PagedTextures.Family> families=(Map<Integer,PagedTextures.Family>)field.get(null);
        try {
            families.put(100,new PagedTextures.Family(100,new PageLayout(256,256,3),new int[]{100,101,102}));
            families.put(200,new PagedTextures.Family(200,new PageLayout(512,256,4),new int[]{200,201,202,203}));
            AtomicInteger root=new AtomicInteger(100);
            AtomicReference<Runnable> notification=new AtomicReference<>();
            IrisPagingSamplers.addMaterialFamily(holder,root::get,notification::set,new String[]{"normals"});
            assertEquals(4,bindings.size());
            int[] bound=new int[4];
            for(int i=0;i<4;i++) { int slot=i; var b=bindings.get(i); bound[i]=b.texture.getAsInt(); b.notifier.setListener(()->bound[slot]=b.texture.getAsInt()); }
            assertArrayEquals(new int[]{100,101,102,100},bound);
            root.set(200); notification.get().run();
            assertArrayEquals(new int[]{200,201,202,203},bound);
            root.set(300); notification.get().run(); // ordinary entity texture, no paged family
            assertArrayEquals(new int[]{300,300,300,300},bound);
            bindings.forEach(b->b.notifier.setListener(null));assertNull(notification.get());
        } finally { families.remove(100);families.remove(200); }
    }
}
