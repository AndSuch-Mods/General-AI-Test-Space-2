package com.pagesofatlas;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Reads bytecode only. Does not start Fabric, Minecraft, or an OpenGL context. */
class MixinTargetTest {
    private ClassNode read(String name) throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class")) {
            assertNotNull(input, name);
            ClassNode node = new ClassNode(); new ClassReader(input).accept(node, 0); return node;
        }
    }
    private List<AnnotationNode> annotations(List<AnnotationNode> a, List<AnnotationNode> b) {
        List<AnnotationNode> out = new ArrayList<>(); if (a != null) out.addAll(a); if (b != null) out.addAll(b); return out;
    }
    private Object value(AnnotationNode a, String key) {
        if (a.values != null) for (int i=0;i<a.values.size();i+=2) if (a.values.get(i).equals(key)) return a.values.get(i+1);
        return null;
    }
    @Test void allRequiredMixinTargetsAndShadowsExistInPinnedArtifacts() throws Exception {
        Map<String,String> versions = new HashMap<>();
        var resources = getClass().getClassLoader().getResources("fabric.mod.json");
        while (resources.hasMoreElements()) try (var stream = resources.nextElement().openStream()) {
            var metadata = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            versions.put(metadata.get("id").getAsString(),metadata.get("version").getAsString());
        }
        assertEquals("0.8.12+mc1.21.1", versions.get("sodium"));
        assertEquals("1.8.14-beta.1+mc1.21.1", versions.get("iris"));
        try (var reader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/pagesofatlas.client.mixins.json")))) {
            var config = JsonParser.parseReader(reader).getAsJsonObject();
            assertTrue(config.get("required").getAsBoolean());
            assertEquals(1, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
            int checked = 0;
            for (var entry : config.getAsJsonArray("client")) {
                var mixin = read(config.get("package").getAsString() + "." + entry.getAsString());
                var mixinAnnotation = annotations(mixin.visibleAnnotations, mixin.invisibleAnnotations).stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
                List<String> targets = new ArrayList<>();
                var classes = value(mixinAnnotation,"value");
                if (classes instanceof List<?> list) for (Object type : list) targets.add(((Type)type).getClassName());
                var names = value(mixinAnnotation,"targets");
                if (names instanceof List<?> list) for (Object name : list) targets.add(name.toString());
                assertFalse(targets.isEmpty());
                for (String target : targets) {
                    var node = read(target);
                    for (var field : mixin.fields) for (var annotation : annotations(field.visibleAnnotations, field.invisibleAnnotations)) {
                        if (annotation.desc.endsWith("/Shadow;")) assertTrue(node.fields.stream().anyMatch(f -> f.name.equals(field.name) && f.desc.equals(field.desc)), target + ":" + field.name + field.desc);
                    }
                    for (var hook : mixin.methods) for (var annotation : annotations(hook.visibleAnnotations, hook.invisibleAnnotations)) {
                        if (!(value(annotation,"method") instanceof List<?> selectors)) continue;
                        assertNull(value(annotation,"require"), "Do not relax required injections");
                        for (Object selector : selectors) {
                            String s = selector.toString();
                            var matches = node.methods.stream().filter(m -> s.equals(m.name) || s.equals(m.name+m.desc)).toList();
                            assertEquals(1, matches.size(), target + " :: " + s);
                            var method = matches.getFirst();
                            assertEquals((method.access & Opcodes.ACC_STATIC) != 0, (hook.access & Opcodes.ACC_STATIC) != 0, target + " staticness");
                            checked++;
                        }
                    }
                }
            }
            assertTrue(checked >= 20, "Expected complete hook inventory");
            System.out.println("Verified " + checked + " required target methods plus shadow descriptors. Mixin application itself remains a runtime check.");
        }
    }
    @Test void pinnedUvPrecisionAndTextureSizeSupplierMatchPagingContract() throws Exception {
        for (String name : List.of(
                "net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex",
                "net.irisshaders.iris.vertices.sodium.terrain.XHFPTerrainVertex")) {
            var node = read(name);
            assertEquals(32768, node.fields.stream().filter(f -> f.name.equals("TEXTURE_MAX_VALUE")).findFirst().orElseThrow().value);
        }
        var supplier=read("net.irisshaders.iris.uniforms.CommonUniforms").methods.stream()
                .filter(m -> m.name.equals("lambda$addDynamicUniforms$2")).findFirst().orElseThrow();
        assertEquals("()Lorg/joml/Vector2i;",supplier.desc);
        Set<String> calls=new HashSet<>();
        for(var instruction:supplier.instructions) if(instruction instanceof MethodInsnNode m) calls.add(m.name);
        assertTrue(calls.containsAll(Set.of("getWidth","getHeight","getInfo")),"Check the exact supplier body, not only its name");
    }
}
