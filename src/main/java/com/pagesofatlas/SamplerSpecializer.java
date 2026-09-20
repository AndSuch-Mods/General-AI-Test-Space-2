package com.pagesofatlas;

import java.util.*;
import java.util.regex.*;

/** Specializes GLSL helpers for concrete atlas arguments, leaving other callers unchanged.
 * Input is preprocessed GLSL without comments. Ambiguous overloads/recursive expansion fail closed.
 */
final class SamplerSpecializer {
    private static final Pattern DEFINITION = Pattern.compile("\\b(\\w+)\\s+(\\w+)\\s*\\(([^;{}]*)\\)\\s*\\{");
    private static final Pattern PARAMETER = Pattern.compile("(?:in\\s+|const\\s+)*sampler2D\\s+(\\w+)");
    private record Function(String result, String name, List<String> params, Map<Integer, String> samplers,
                            String body, int start, int end) {}
    private record Edit(int start, int end, String text) {}
    private SamplerSpecializer() {}

    static String specialize(String source, Collection<String> atlasNames) {
        // A helper parameter called 'tex' must never be mistaken for the global atlas alias.
        List<Edit> renames = new ArrayList<>();
        int serial = 0;
        for (Function f : functions(source)) {
            String text = source.substring(f.start, f.end);
            for (String param : f.samplers.values()) if (atlasNames.contains(param)) {
                text = text.replaceAll("\\b" + param + "\\b", "poa_argument_" + serial++);
            }
            renames.add(new Edit(f.start, f.end, text));
        }
        source = edit(source, renames);
        Set<String> globals = new HashSet<>();
        for (String name : atlasNames) if (Pattern.compile("\\buniform\\s+sampler2D\\s+" + name + "\\s*;").matcher(source).find()) globals.add(name);
        Map<String, String> specialized = new HashMap<>();
        for (int pass = 0; pass < 32; pass++) {
            List<Function> definitions = functions(source);
            List<Edit> edits = new ArrayList<>();
            Map<Integer, StringBuilder> additions = new TreeMap<>();
            for (String name : definitions.stream().map(Function::name).distinct().toList()) {
                Matcher calls = Pattern.compile("\\b" + name + "\\s*\\(").matcher(source);
                while (calls.find()) {
                    int open = source.indexOf('(', calls.start()), close = closing(source, open, '(', ')');
                    List<String> args = split(source.substring(open + 1, close));
                    if (args.stream().noneMatch(globals::contains)) continue;
                    List<Function> candidates = definitions.stream().filter(f -> f.name.equals(name) && f.params.size() == args.size()).toList();
                    if (candidates.size() != 1) throw unsupported("ambiguous sampler helper " + name);
                    Function f = candidates.getFirst();
                    Map<Integer, String> bindings = new TreeMap<>();
                    for (int i : f.samplers.keySet()) if (globals.contains(args.get(i))) bindings.put(i, args.get(i));
                    if (bindings.isEmpty()) continue;
                    String key = name + "/" + args.size() + bindings;
                    String replacement = specialized.get(key);
                    if (replacement == null) {
                        replacement = "poa_helper_" + specialized.size() + "_" + name;
                        specialized.put(key, replacement);
                        List<String> params = new ArrayList<>();
                        String body = f.body;
                        for (int i = 0; i < f.params.size(); i++) {
                            if (bindings.containsKey(i)) body = body.replaceAll("\\b" + f.samplers.get(i) + "\\b", bindings.get(i));
                            else params.add(f.params.get(i));
                        }
                        additions.computeIfAbsent(f.end, unused -> new StringBuilder()).append('\n').append(f.result)
                                .append(' ').append(replacement).append('(').append(String.join(", ", params)).append(") {").append(body).append("}\n");
                    }
                    List<String> remaining = new ArrayList<>();
                    for (int i = 0; i < args.size(); i++) if (!bindings.containsKey(i)) remaining.add(args.get(i));
                    edits.add(new Edit(calls.start(), close + 1, replacement + "(" + String.join(", ", remaining) + ")"));
                }
            }
            if (edits.isEmpty()) return source;
            additions.forEach((at, text) -> edits.add(new Edit(at, at, text.toString())));
            source = edit(source, edits);
        }
        throw unsupported("recursive sampler helpers or specialization limit exceeded");
    }

    private static List<Function> functions(String source) {
        List<Function> out = new ArrayList<>();
        Matcher m = DEFINITION.matcher(source);
        while (m.find()) {
            List<String> params = split(m.group(3));
            Map<Integer, String> samplers = new TreeMap<>();
            for (int i = 0; i < params.size(); i++) {
                Matcher p = PARAMETER.matcher(params.get(i));
                if (p.matches()) samplers.put(i, p.group(1));
            }
            int end = closing(source, m.end() - 1, '{', '}');
            if (!samplers.isEmpty()) out.add(new Function(m.group(1), m.group(2), params, samplers,
                    source.substring(m.end(), end), m.start(), end + 1));
        }
        return out;
    }
    private static int closing(String source, int open, char left, char right) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            if (source.charAt(i) == left) depth++;
            if (source.charAt(i) == right && --depth == 0) return i;
        }
        throw unsupported("unbalanced GLSL delimiters");
    }
    private static List<String> split(String text) {
        if (text.isBlank() || text.trim().equals("void")) return List.of();
        List<String> out = new ArrayList<>(); int depth = 0, start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(' || c == '[') depth++;
            if (c == ')' || c == ']') depth--;
            if (c == ',' && depth == 0) { out.add(text.substring(start, i).trim()); start = i + 1; }
        }
        out.add(text.substring(start).trim()); return out;
    }
    private static String edit(String source, List<Edit> edits) {
        edits.sort(Comparator.comparingInt(Edit::start).reversed());
        StringBuilder result = new StringBuilder(source); int boundary = source.length();
        for (Edit e : edits) {
            if (e.end > boundary) throw unsupported("nested overlapping sampler calls");
            result.replace(e.start, e.end, e.text); boundary = e.start;
        }
        return result.toString();
    }
    private static IllegalArgumentException unsupported(String reason) {
        return new IllegalArgumentException("Pages of Atlas: " + reason + "; shader needs an explicit paging adapter");
    }
}
