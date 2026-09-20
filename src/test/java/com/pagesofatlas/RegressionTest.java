package com.pagesofatlas;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class RegressionTest {
    @Test void localBindingsAndStructMembersAreNotAtlasReferences() {
        String s = """
            #version 330 core
            uniform sampler2D specular;
            struct Material { float specular; };
            float lighting(float specular) { return specular * 2.; }
            vec4 getMaterial(vec2 uv) {
                vec4 sampled = texture(specular, uv);
                { float specular = 0.5; sampled *= lighting(specular); }
                for (int specular=0; specular<2; specular++) { sampled *= .5; }
                Material m; m.specular = .5;
                return sampled + texture(specular, uv) * m.specular;
            }
            out vec4 color;
            void main(){color=getMaterial(vec2(.75));}
            """;
        String p = ShaderPaging.patch(s).replaceAll("\\s+", "");
        assertTrue(p.contains("poa_specular_texture(uv)"));
        assertTrue(p.contains("m.specular"));
        assertTrue(p.contains("poa_local_"));
        assertFalse(p.contains("float specular"));
    }
    @Test void localSamplerShadowKeepsItsOwnBinding() {
        String s = "uniform sampler2D normals; uniform sampler2D noise; vec4 read(sampler2D normals,vec2 uv){return texture(normals,uv);} void main(){vec4 a=read(noise,vec2(0.2f))+texture(normals,vec2(.7));}";
        String p=ShaderPaging.patch(s).replaceAll("\\s+", "");
        assertTrue(p.contains("texture(poa_local_"));
        assertTrue(p.contains("read(noise,vec2(0.2f))"), p);
        assertTrue(p.contains("poa_normals_texture("));
    }
    @Test void reloadLayoutsCanShrinkFromFourPagesToOne() {
        var large=new PageLayout(16384,16384,4);
        var small=new PageLayout(4096,2048,1);
        assertEquals(32768,large.logicalWidth());
        assertEquals(4096,small.logicalWidth()); assertEquals(2048,small.logicalHeight());
        assertEquals(1,small.pages());assertEquals(1,small.columns());assertEquals(1,small.rows());
        assertEquals(new PageLayout.Upload(0,64,32),small.upload(2,64,32,32,32));
    }
    @Test void missingLowerMipAndGlErrorsFailEvenWhenBaseLevelExists() {
        var l=new PageLayout(1024,512,3);
        assertDoesNotThrow(()->AllocationChecks.validate(10,11,l,4,(m,axis)->(axis==0?1024:512)>>m,()->0));
        var error=assertThrows(IllegalStateException.class,()->AllocationChecks.validate(10,11,l,4,
                (m,axis)->m==3?0:(axis==0?1024:512)>>m,()->0));
        assertTrue(error.getMessage().contains("mip=3")); assertTrue(error.getMessage().contains("texture=11"));
        assertThrows(IllegalStateException.class,()->AllocationChecks.validate(10,11,l,4,(m,axis)->(axis==0?1024:512)>>m,()->0x505));
    }
    @Test void materialNotifierUpdatesEveryPageAndDetachesOnProgramSwitch() {
        AtomicReference<Runnable> upstream = new AtomicReference<>();
        ListenerGroup first = new ListenerGroup(upstream::set);
        var slots = new ArrayList<java.util.function.Consumer<Runnable>>();
        int[] counts = new int[4];
        for(int i=0;i<4;i++){int page=i;var slot=first.slot();slots.add(slot);slot.accept(()->counts[page]++);}
        upstream.get().run(); assertArrayEquals(new int[]{1,1,1,1},counts);
        // Re-applying a program replaces listeners rather than duplicating them.
        slots.get(2).accept(()->counts[2]++);
        upstream.get().run(); assertArrayEquals(new int[]{2,2,2,2},counts);
        slots.forEach(s->s.accept(null)); assertNull(upstream.get());
        AtomicInteger nextCount=new AtomicInteger();
        ListenerGroup second=new ListenerGroup(upstream::set);var next=second.slot();next.accept(nextCount::incrementAndGet);
        upstream.get().run(); assertEquals(1,nextCount.get());assertArrayEquals(new int[]{2,2,2,2},counts);
        next.accept(null); assertNull(upstream.get());
    }
    @Test void diffuseListenersDoNotSurviveClearOrAccumulate() {
        var group=new ListenerGroup();var slot=group.slot();AtomicInteger count=new AtomicInteger();
        slot.accept(count::incrementAndGet);slot.accept(count::incrementAndGet);group.fire();assertEquals(1,count.get());
        slot.accept(null);group.fire();assertEquals(1,count.get());
    }
}
