# Upstream attribution

Unofficial Minecraft Java 1.21.1 Fabric backport of [Pages of Atlas](https://github.com/mattmcbeardface/pages-of-atlas).

- Frozen source: `d4db5c0fc619474018cf2eca4cfd4889a7ae973f` (tag `v0.3.15`).
- Upstream targets Minecraft 26.2/Java 25. Its packing algorithm is reused; version-specific integrations were rewritten against 1.21.1.
- Additional cherry-picked commits: none.
- Upstream MIT license and copyright are retained verbatim in LICENSE.
- Destination branch starts at its existing main commit `ba3dfc54d1b00e264f4f9059cfca331f25cee31f`; the tree was empty when cloned.

## API reference provenance

- Exact 1.21.1 client/server artifacts and official mappings resolved by Loom from Mojang metadata. Decompiled sources stayed in ignored local caches and are not distributed.
- Sodium source tag `mc1.21.1-0.6.13`, commit `8672650501117e72f5a809867092378fb5bc908a`.
- Iris source commit `25d756f9c773879fb50e59626e5dd7f5bba1348f` declares release 1.8.8 for 1.21.1. Actual downloaded 1.8.8 artifact bytecode is the authority for target signatures. The newer 1.21.1 branch was inspected briefly, then frozen to this release source; it was not used as the compiled dependency.
- Published Continuity 3.0.0+1.21 sources and artifact metadata were resolved for 1.21.1. Its sprite lookup receives the logical atlas and unique UVs; full CTM rendering still needs user testing.
- No source from Sodium or Iris is redistributed here. Integrations use their pinned APIs. The shader-routing implementation is new backport code; this is a logical-grid adaptation of upstream physical paging, not a claim that upstream's 26.2 renderer hooks work on 1.21.1.
- Gradle wrapper JAR and scripts come from Gradle v8.12.0; upstream's 9.5.1 wrapper launchers could not be mixed with the 8.12 JAR and were replaced with matching official launchers.
