package com.pagesofatlas;

import com.google.gson.*;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Use Fabric's actual version predicate rules on the downloaded manifest evidence. No game or loader launch. */
class DependencyContractTest {
    private JsonArray evidence() throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("validation/dependency-manifests.json"))).getAsJsonArray();
    }
    private boolean accepts(JsonElement range, String version) throws Exception {
        if (range.isJsonArray()) {
            for (var item : range.getAsJsonArray()) if (accepts(item, version)) return true;
            return false;
        }
        return VersionPredicate.parse(range.getAsString()).test(Version.parse(version));
    }
    @Test void pinnedCoreAndBundledManifestConstraintsAcceptEachOther() throws Exception {
        var selected = Set.of("sodium", "iris", "fabric-api", "continuity");
        List<JsonObject> manifests = new ArrayList<>();
        for (var entry : evidence()) {
            var object = entry.getAsJsonObject();
            if (!selected.contains(object.get("name").getAsString())) continue;
            manifests.add(object.getAsJsonObject("manifest"));
            for (var child : object.getAsJsonArray("bundled")) manifests.add(child.getAsJsonObject());
        }
        Map<String, String> installed = new HashMap<>(Map.of("minecraft","1.21.1","java","21","fabricloader","0.18.4"));
        for (var m : manifests) installed.put(m.get("id").getAsString(), m.get("version").getAsString());
        for (var m : manifests) {
            String id = m.get("id").getAsString();
            if (m.get("depends") instanceof JsonObject deps) for (var dep : deps.entrySet()) {
                assertTrue(installed.containsKey(dep.getKey()), id + " missing " + dep.getKey());
                assertTrue(accepts(dep.getValue(), installed.get(dep.getKey())), id + " requires " + dep);
            }
            if (m.get("breaks") instanceof JsonObject breaks) for (var conflict : breaks.entrySet()) {
                if (installed.containsKey(conflict.getKey())) assertFalse(accepts(conflict.getValue(), installed.get(conflict.getKey())), id + " breaks " + conflict);
            }
        }
    }
    @Test void distinguishScreenshotVersionsFromSupportedReplacementRanges() throws Exception {
        Map<String, JsonObject> mods = new HashMap<>();
        for (var e : evidence()) { var o=e.getAsJsonObject(); mods.put(o.get("name").getAsString(),o.getAsJsonObject("manifest")); }
        var sodiumBreaks=mods.get("sodium").getAsJsonObject("breaks");
        for (String id : List.of("sodium-extra","reeses-sodium-options")) {
            assertTrue(accepts(sodiumBreaks.get(id),mods.get(id+"-original").get("version").getAsString()));
            assertFalse(accepts(sodiumBreaks.get(id),mods.get(id).get("version").getAsString()));
        }
        // Published Cull Leaves 3.4.0 omits '-fabric'; semver puts stable 3.4.0 above 3.4.0-fabric.
        assertFalse(accepts(sodiumBreaks.get("cullleaves"),mods.get("cull-leaves-original").get("version").getAsString()));
        assertFalse(accepts(sodiumBreaks.get("cullleaves"),mods.get("cull-leaves").get("version").getAsString()));
        assertTrue(accepts(mods.get("sodium-extra").getAsJsonObject("depends").get("fabricloader"),"0.18.4"));
        assertTrue(accepts(mods.get("dynamictrees").getAsJsonObject("depends").get("fabricloader"),"0.18.4"));
        assertFalse(accepts(mods.get("dynamictrees").getAsJsonObject("depends").get("fabricloader"),"0.16.14"));
        assertTrue(accepts(mods.get("reeses-sodium-options").getAsJsonObject("depends").get("sodium"),"0.8.12+mc1.21.1"));
        assertFalse(accepts(mods.get("sodium-extra").getAsJsonObject("breaks").get("reeses-sodium-options"),"2.0.5+mc1.21.1"));
    }
}
