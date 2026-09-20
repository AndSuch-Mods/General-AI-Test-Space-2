# Upstream attribution

Unofficial Minecraft Java 1.21.1 Fabric backport of [Pages of Atlas](https://github.com/mattmcbeardface/pages-of-atlas).

- Frozen source: `d4db5c0fc619474018cf2eca4cfd4889a7ae973f` (tag `v0.3.15`).
- Upstream targets Minecraft 26.2/Java 25. Its packing algorithm is reused; version-specific integrations were rewritten against 1.21.1.
- Additional cherry-picked commits: none.
- Upstream MIT license and copyright are retained verbatim in LICENSE.
- Destination branch starts at its existing main commit `ba3dfc54d1b00e264f4f9059cfca331f25cee31f`; the tree was empty when cloned.

## API reference provenance

- Exact 1.21.1 client/server artifacts and official mappings resolved by Loom from Mojang metadata. Decompiled sources stayed in ignored local caches and are not distributed.
- Sodium source tag `mc1.21.1-0.8.12`, commit `53306ac4db8f9fae1655c81539ffcd79e4afc4fb`.
- Iris source commit `eb7afb99f747cc8ed5ee4072119539035d33cefd` is the 2026-06-13 Sodium update used as the 1.8.14-beta.1 source reference. Actual downloaded 1.8.14-beta.1 artifact bytecode is the authority for target signatures. The newer 1.21.1 branch was inspected briefly, then frozen to this release source; it was not used as the compiled dependency.
- Published Continuity 3.0.0+1.21 sources and artifact metadata were resolved for 1.21.1. Its sprite lookup receives the logical atlas and unique UVs; full CTM rendering still needs user testing.
- No source from Sodium or Iris is redistributed here. Integrations use their pinned APIs. The shader-routing implementation is new backport code; this is a logical-grid adaptation of upstream physical paging, not a claim that upstream's 26.2 renderer hooks work on 1.21.1.
- Backport.2 wrapper JAR/scripts come from Gradle v9.4.0. The selected artifact metadata requires Loom 1.16.3, whose plugin metadata requires Gradle >=9.4.0. Wrapper and distribution SHA-256 values are in DEPENDENCIES.json. Backport.1 used Gradle 8.12/Loom 1.10.5 and remains in history.

- Backport.1 checkpoint: 5f882c4b976056b7809f525e83448720e6aef537. Backport.2 is a continuation on the same branch, with no reset or force-push. Shader references and download hashes are recorded in SHADERS.json; no shader sources are redistributed.
