package com.pagesofatlas;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Compile actual selected POM/material functions in a minimal harness, not a full shader pack or GPU test. */
class ShaderPackSourceTest {
    @TempDir Path temp;
    private String source(String name) throws Exception {
        Path p = Path.of("build/shader-inputs/" + name);
        assumeTrue(Files.exists(p), "Run python tools/fetch_shader_fixtures.py to enable real-source checks");
        return Files.readString(p);
    }
    private void compile(String body) throws Exception {
        String compiler = System.getProperty("poa.glslang");
        assumeTrue(compiler != null, "GLSLANG_VALIDATOR is required");
        // Same legacy operation renames made by pinned Iris CommonTransformer before our hook.
        body = body.replaceAll("\\btexture2DGradARB\\b", "textureGrad").replaceAll("\\btexture2DLod\\b", "textureLod").replaceAll("\\btexture2D\\b", "texture");
        String text = "#version 330 core\nuniform sampler2D gtexture; uniform sampler2D normals; uniform sampler2D specular; out vec4 outputColor;\n" + body;
        Path input = temp.resolve("input.frag"); Files.writeString(input, text);
        // Preprocess active LabPBR/POM defines before routing, as Iris does.
        Process pp = new ProcessBuilder(compiler, "-E", input.toString()).redirectErrorStream(true).start();
        String preprocessed = new String(pp.getInputStream().readAllBytes()); assertEquals(0, pp.waitFor(), preprocessed);
        String patched = ShaderPaging.patch(preprocessed);
        assertTrue(patched.contains("poa_normals"), "Height/normal sampling must actually be routed");
        Path file = temp.resolve("paged.frag"); Files.writeString(file, patched);
        Process process = new ProcessBuilder(compiler, file.toString()).redirectErrorStream(true).start();
        String log = new String(process.getInputStream().readAllBytes()); assertEquals(0, process.waitFor(), log);
    }
    @Test void blissActualIndirectPomSwitch() throws Exception {
        String source = source("bliss.glsl");
        int start = source.indexOf("vec4 texture2D_POMSwitch("), open = source.indexOf('{', start), end = open + 1, depth = 1;
        for (; depth > 0; end++) { char c = source.charAt(end); if(c=='{') depth++; if(c=='}') depth--; }
        String helper = source.substring(start, end);
        compile(helper + "\nvoid main(){vec2 uv=vec2(.75); vec4 grad=vec4(.01); outputColor=texture2D_POMSwitch(gtexture,uv,grad,true,0.) + texture2D_POMSwitch(normals,uv,grad,true,0.) + texture2D_POMSwitch(specular,uv,grad,false,1.);}");
    }
    @Test void photonActualPomAndSelfShadow() throws Exception {
        compile("""
            #define POM_SAMPLES 32
            #define POM_SHADOW_SAMPLES 16
            #define POM_DISTANCE 64.0
            #define POM_DEPTH 0.1
            vec2 atlas_tile_offset=vec2(.5), atlas_tile_scale=vec2(.125), atlas_tile_coord=vec2(.5), uv=vec2(.75);
            vec3 light_dir=vec3(0,0,1); mat3 tbn=mat3(1); const float eps=1e-6;
            #define rcp(x) (1.0 / (x))
            float linear_step(float a,float b,float x){return clamp((x-a)/(b-a),0.,1.);}
            """ + source("photon.glsl") + "\nvoid main(){vec3 previous;float depth;vec2 p=get_parallax_uv(vec3(.1,.1,-1),mat2(.01),5.,.5,previous,depth);bool shadow=get_parallax_shadow(previous,mat2(.01),5.,.5);outputColor=vec4(p,depth,shadow?1.:0.);}");
    }
    @Test void bslActualLabPbrPomEmissionAndSubsurface() throws Exception {
        compile("""
            #define PARALLAX
            #define PARALLAX_QUALITY 32
            #define PARALLAX_DEPTH 0.1
            #define SELF_SHADOW_ANGLE 1.0
            #define SELF_SHADOW_QUALITY 16
            #define SELF_SHADOW_STRENGTH 1.0
            #define MATERIAL_FORMAT 1
            #define EMISSIVE 2
            #define NORMAL_DAMPENING
            vec4 vTexCoordAM=vec4(.5,.5,.125,.125),vTexCoord=vec4(.5);
            vec2 dcdx=vec2(.01,0),dcdy=vec2(0,.01),texCoord=vec2(.5),atlasSize=vec2(4096);
            vec3 viewVector=vec3(.1,.1,-1);
            """ + source("bsl-parallax.glsl") + source("bsl-material.glsl") + """
            void main(){float depth;vec2 p=GetParallaxCoord(texCoord,0.,depth);float shadow=GetParallaxShadow(depth,0.,p,vec3(0,1,0),mat3(1));
            float smoothness,metalness,f0,emission=0.,subsurface=0.,porosity,ao;vec3 normal;
            GetMaterials(smoothness,metalness,f0,emission,subsurface,porosity,ao,normal,p,dcdx,dcdy);outputColor=vec4(normal*shadow,emission+subsurface);}
            """);
    }
}
