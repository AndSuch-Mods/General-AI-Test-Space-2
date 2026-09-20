package com.pagesofatlas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GlslTest {
    @TempDir Path directory;
    @Test void generatedShaderOperationsCompileWithoutGraphicsContext() throws Exception {
        String compiler = System.getProperty("poa.glslang");
        assumeTrue(compiler != null, "Set GLSLANG_VALIDATOR for the optional Khronos GLSL compiler check");
        String fragment = "#version 330 core\nuniform sampler2D gtexture; uniform sampler2D normals; uniform sampler2D specular; in vec2 uv; out vec4 color;\nvoid main() { color = texture(gtexture, uv) + texture(gtexture,uv,1.0) + textureLod(normals,uv,1.0) + textureGrad(specular,uv,dFdx(uv),dFdy(uv)) + texelFetch(gtexture,ivec2(10),0); color.xy += textureQueryLod(normals,uv); color.xy += vec2(textureSize(gtexture,0)); }";
        String vertex = "#version 150\nuniform sampler2D Sampler0; in vec4 position; void main(){gl_Position=position + texture(Sampler0,vec2(0.5));}";
        for (String stage : List.of("frag","vert")) {
            Path file = directory.resolve("paged." + stage);
            Files.writeString(file, ShaderPaging.patch(stage.equals("frag") ? fragment : vertex, stage.equals("frag"), ShaderPaging.SAMPLERS));
            Process process = new ProcessBuilder(compiler, file.toString()).redirectErrorStream(true).start();
            String log = new String(process.getInputStream().readAllBytes());
            assertEquals(0, process.waitFor(), log);
        }
    }
}
