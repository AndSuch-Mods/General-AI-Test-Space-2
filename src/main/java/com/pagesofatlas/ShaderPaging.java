package com.pagesofatlas;

import java.util.*;
import java.util.regex.*;

/** Routes atlas sampling after includes/preprocessing, without modifying pack files. */
public final class ShaderPaging {
    public static final List<String> SAMPLERS = List.of("Sampler0", "u_BlockTex", "gtexture", "tex", "texture", "normals", "specular");
    private static final Set<String> SUPPORTED = Set.of("texture", "texture2D", "textureLod", "texture2DLod", "textureGrad", "textureSize", "texelFetch", "textureQueryLod");
    private ShaderPaging() {}
    public static String prefix(String sampler) { return "poa_" + sampler; }
    public static String patch(String input) { return patch(input, true, SAMPLERS); }
    public static String patch(String input, boolean fragment, List<String> samplers) {
        if (input.contains("// POA_PAGED_SAMPLING")) return input;
        // Includes and macros have already been expanded by the originating renderer.
        String source = input.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//[^\\r\\n]*", "");
        boolean changed = false;
        for (String sampler : samplers) {
            Pattern declaration = Pattern.compile("\\buniform\\s+sampler2D\\s+" + sampler + "\\s*;");
            Matcher declared = declaration.matcher(source);
            if (!declared.find()) continue;
            String p = prefix(sampler);
            Pattern call = Pattern.compile("\\b([A-Za-z_][A-Za-z_0-9]*)\\s*\\(\\s*" + sampler + "\\s*,");
            Matcher calls = call.matcher(source);
            Set<String> functions = new TreeSet<>();
            StringBuffer replaced = new StringBuffer();
            while (calls.find()) {
                String function = calls.group(1);
                if (!SUPPORTED.contains(function)) throw new IllegalArgumentException("Pages of Atlas: unsupported sampler operation " + function + "(" + sampler + ", ...)");
                functions.add(function);
                calls.appendReplacement(replaced, Matcher.quoteReplacement(p + "_" + function + "("));
            }
            calls.appendTail(replaced);
            String rewritten = replaced.toString();
            String withoutDeclaration = declaration.matcher(rewritten).replaceFirst("");
            // The legacy alias 'texture' is also a built-in function name. Only
            // sampler-value references matter here, not calls using another sampler.
            String remainingUse = "\\b" + sampler + "\\b" + (sampler.equals("texture") ? "(?!\\s*\\()" : "");
            if (Pattern.compile(remainingUse).matcher(withoutDeclaration).find()) {
                throw new IllegalArgumentException("Pages of Atlas: sampler " + sampler + " is passed indirectly; shader needs an explicit paging adapter");
            }
            if (functions.isEmpty()) continue;
            source = rewritten;
            StringBuilder helper = new StringBuilder("uniform sampler2D " + sampler + ";\n");
            helper.append("uniform ivec2 ").append(p).append("_grid;\n");
            for (int page = 1; page < 4; page++) helper.append("uniform sampler2D ").append(p).append(page).append(";\n");
            helper.append("ivec2 ").append(p).append("_tile(vec2 uv) { return clamp(ivec2(floor(uv * vec2(").append(p).append("_grid))), ivec2(0), ").append(p).append("_grid - 1); }\n");
            for (String function : functions) appendFunction(helper, sampler, p, function, fragment);
            source = declaration.matcher(source).replaceFirst(Matcher.quoteReplacement(helper.toString()));
            changed = true;
        }
        return changed ? source + "\n// POA_PAGED_SAMPLING\n" : input;
    }
    private static void appendFunction(StringBuilder b, String sampler, String p, String function, boolean fragment) {
        if (function.equals("textureSize")) {
            b.append("ivec2 ").append(p).append("_textureSize(int lod) { return textureSize(").append(sampler).append(", lod) * ").append(p).append("_grid; }\n");
            return;
        }
        boolean fetch = function.equals("texelFetch");
        boolean query = function.equals("textureQueryLod");
        String result = query ? "vec2" : "vec4";
        String signature = fetch ? "ivec2 uv, int lod" : "vec2 uv";
        String suffix = "";
        if (function.equals("textureLod") || function.equals("texture2DLod")) { signature += ", float lod"; suffix = ", lod"; }
        if (function.equals("textureGrad")) { signature += ", vec2 dx, vec2 dy"; suffix = ", dx * vec2(" + p + "_grid), dy * vec2(" + p + "_grid)"; }
        if (fetch) suffix = ", lod";
        appendOverload(b, sampler, p, function, result, signature, suffix, fetch, fragment);
        // Fragment texture() and legacy texture2D() support the optional LOD bias.
        if (fragment && (function.equals("texture") || function.equals("texture2D"))) {
            appendOverload(b, sampler, p, function, result, signature + ", float bias", ", bias", false, true);
        }
    }
    private static void appendOverload(StringBuilder b, String sampler, String p, String fn, String result, String signature, String suffix, boolean fetch, boolean fragment) {
        b.append(result).append(' ').append(p).append('_').append(fn).append('(').append(signature).append(") {\n");
        if (fetch) b.append("ivec2 size = textureSize(").append(sampler).append(", lod); ivec2 tile = clamp(uv / size, ivec2(0), ").append(p).append("_grid - 1); ivec2 local = uv - tile * size;\n");
        else b.append("ivec2 tile = ").append(p).append("_tile(uv); vec2 local = uv * vec2(").append(p).append("_grid) - vec2(tile);\n");
        String nativeFunction = fn;
        if (fragment && (fn.equals("texture") || fn.equals("texture2D"))) {
            String scale = suffix.isEmpty() ? "" : " * exp2(bias)";
            b.append("vec2 gx = dFdx(uv) * vec2(").append(p).append("_grid)").append(scale).append("; vec2 gy = dFdy(uv) * vec2(").append(p).append("_grid)").append(scale).append(";\n");
            nativeFunction = "textureGrad";
            suffix = ", gx, gy";
        }
        b.append("int page = tile.y * ").append(p).append("_grid.x + tile.x;\n");
        for (int i = 1; i < 4; i++) b.append("if (page == ").append(i).append(") return ").append(nativeFunction).append('(').append(p).append(i).append(", local").append(suffix).append(");\n");
        b.append("return ").append(nativeFunction).append('(').append(sampler).append(", local").append(suffix).append("); }\n");
    }
}
