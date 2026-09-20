package com.pagesofatlas;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PagingTest {
    private static boolean containsCode(String source, String snippet) { return source.replaceAll("\\s+", "").contains(snippet.replaceAll("\\s+", "")); }
    record Sprite(String name, int width, int height) implements PagesOfAtlasPager.Entry {}
    @Test void deterministicAcrossInputOrderAndNoOverlaps() {
        var random = new Random(1211);
        List<Sprite> sprites = new ArrayList<>();
        for (int i = 0; i < 400; i++) sprites.add(new Sprite("sprite" + i, 16 << random.nextInt(4), 16 << random.nextInt(4)));
        var first = PagesOfAtlasPager.pack(sprites, 512, 512, 4, 0);
        Collections.shuffle(sprites, random);
        var second = PagesOfAtlasPager.pack(sprites, 512, 512, 4, 0);
        assertEquals(first.pages().stream().map(PagesOfAtlasPager.Page::placements).toList(), second.pages().stream().map(PagesOfAtlasPager.Page::placements).toList());
        Set<String> seen = new HashSet<>();
        for (var page : first.pages()) {
            assertTrue(page.width() <= 512 && page.height() <= 512);
            boolean[][] occupied = new boolean[512][512];
            for (var p : page.placements()) {
                assertTrue(seen.add(p.name()));
                assertEquals(0, p.x() % 16);
                assertEquals(0, p.y() % 16);
                for (int y = p.y(); y < p.y() + p.entry().height(); y++) for (int x = p.x(); x < p.x() + p.entry().width(); x++) {
                    assertTrue(x < page.width() && y < page.height());
                    assertFalse(occupied[y][x]); occupied[y][x] = true;
                }
            }
        }
        assertEquals(400, seen.size());
    }
    @Test void exactBoundariesAndOversizedSprites() {
        var sprites = List.of(new Sprite("a", 512, 512), new Sprite("b", 512, 512));
        assertEquals(2, PagesOfAtlasPager.pack(sprites, 512, 512, 4, 0).pages().size());
        assertThrows(IllegalArgumentException.class, () -> PagesOfAtlasPager.pack(List.of(new Sprite("x", 513, 1)), 512, 512, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> PagesOfAtlasPager.pack(List.of(sprites.getFirst(), sprites.getFirst()), 512, 512, 0, 0));
    }
    @Test void diffuseNormalSpecularUseIdenticalCoordinatesAtEveryMip() {
        for (int pages = 2; pages <= 4; pages++) {
            var layout = new PageLayout(2048, 1024, pages);
            for (int page = 0; page < pages; page++) for (int mip = 0; mip <= 4; mip++) {
                int x = layout.x(page, 64) >> mip, y = layout.y(page, 128) >> mip;
                var upload = layout.upload(mip, x, y, 128 >> mip, 64 >> mip);
                assertEquals(page, upload.page());
                assertEquals(64 >> mip, upload.x()); assertEquals(128 >> mip, upload.y());
            }
        }
    }
    @Test void rejectCrossPageAndUnallocatedCell() {
        var layout = new PageLayout(256, 256, 3);
        assertThrows(IllegalArgumentException.class, () -> layout.upload(0, 250, 0, 16, 16));
        assertThrows(IllegalArgumentException.class, () -> layout.upload(0, 256, 256, 16, 16));
    }
    @Test void pageRoutingSupportsPbrPomAndExplicitGradients() {
        String shader = "#version 330\nuniform sampler2D gtexture; uniform sampler2D normals; uniform sampler2D specular;\nvoid main(){ vec2 u=vec2(0.7); vec4 d=texture(gtexture,u); vec4 n=textureGrad(normals,u,vec2(.01),vec2(.01)); vec4 s=textureLod(specular,u,2.); ivec2 z=textureSize(gtexture,0); }";
        String patched = ShaderPaging.patch(shader);
        assertTrue(containsCode(patched, "poa_gtexture_texture(u)"));
        assertTrue(containsCode(patched, "poa_normals_textureGrad(u,"));
        assertTrue(containsCode(patched, "poa_specular_textureLod(u,"));
        assertTrue(containsCode(patched, "textureSize(gtexture, lod) * poa_gtexture_grid"));
        assertTrue(containsCode(patched, "dFdx(uv) * vec2(poa_gtexture_grid)"));
        assertEquals(patched, ShaderPaging.patch(patched));
    }
    @Test void vertexShaderDoesNotAcquireFragmentOnlyOperations() {
        String shader = "#version 150\nuniform sampler2D Sampler0;void main(){gl_Position=texture(Sampler0,vec2(0.5));}";
        String patched = ShaderPaging.patch(shader, false, List.of("Sampler0"));
        assertFalse(containsCode(patched, "dFdx")); assertFalse(containsCode(patched, "float bias"));
    }
    @Test void unsupportedSamplerOperationsFailExplicitly() {
        assertThrows(IllegalArgumentException.class, () -> ShaderPaging.patch("uniform sampler2D gtexture; void main(){vec4 x=textureGather(gtexture,vec2(0));}"));
        assertThrows(IllegalArgumentException.class, () -> ShaderPaging.patch("uniform sampler2D gtexture;void main(){vec4 x=texture(gtexture,vec2(0));foo(gtexture);}"));
        assertTrue(ShaderPaging.patch("uniform sampler2D normals; vec4 sampleMaterial(sampler2D s, vec2 uv){return texture(s,uv);} void main(){vec4 x=sampleMaterial(normals,vec2(0));}").replaceAll("\\s+", "").contains("poa_normals_texture(uv)"));
        assertThrows(IllegalArgumentException.class, () -> ShaderPaging.patch("uniform sampler2D normals; void main(){foo(normals);}"));
        assertThrows(IllegalArgumentException.class, () -> ShaderPaging.patch("uniform sampler2D normals[2]; void main(){vec4 x=texture(normals[0],vec2(0));}"));
        assertEquals("void main(){}", ShaderPaging.patch("void main(){}"));
    }
    @Test void specializeNestedHelpersWithoutChangingNonAtlasOrOceanSamplers() {
        String source = "uniform sampler2D gtexture; uniform sampler2D normals; uniform sampler2D physics_waviness; "
                + "vec4 read(sampler2D tex,vec2 uv){return textureGrad(tex,uv,vec2(.01),vec2(.02));} "
                + "vec4 material(vec2 uv,sampler2D s){return read(s,uv);}"
                + "void main(){vec4 a=material(vec2(.5),gtexture)+material(vec2(.5),normals)+material(vec2(0.5f),physics_waviness);}";
        String result = ShaderPaging.patch(source);
        assertTrue(containsCode(result, "poa_gtexture_textureGrad"));
        assertTrue(containsCode(result, "poa_normals_textureGrad"));
        assertTrue(containsCode(result, "material(vec2(0.5f),physics_waviness)"), result);
        assertFalse(containsCode(result, "poa_physics_waviness"));
    }
    @Test void genericTexParameterDoesNotAliasGlobalTexSampler() {
        String source = "uniform sampler2D tex; uniform sampler2D noisetex; vec4 read(sampler2D tex,vec2 uv){return texture(tex,uv);} void main(){vec4 x=read(tex,vec2(0))+read(noisetex,vec2(0));}";
        String result = ShaderPaging.patch(source);
        assertTrue(containsCode(result, "texture(poa_local_"));
        assertTrue(containsCode(result, "poa_tex_texture"));
        assertTrue(containsCode(result, "read(noisetex,vec2(0))"));
    }
}
