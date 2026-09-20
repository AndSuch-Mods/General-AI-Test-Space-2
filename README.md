# Pages of Atlas — unofficial Fabric 1.21.1 backport

Experimental source backport of [Pages of Atlas](https://github.com/mattmcbeardface/pages-of-atlas), with a remapped client JAR. **Compiles and passes offline checks; in-game compatibility is unverified.** This is not an upstream release. Minecraft **1.21.1 exactly**, not 1.21.11.

The supplied profile/mod list was unavailable. This is a **provisional pinned stack**, not a replacement of the user's selected versions:

| Dependency | Exact build used |
|---|---|
| Minecraft / mappings | 1.21.1 / Mojang official 1.21.1 |
| Java | 21 bytecode; Java 21 recommended |
| Fabric Loader | 0.16.14 (minimum runtime) |
| Fabric API | 0.116.17+1.21.1 |
| Sodium | 0.6.13+mc1.21.1; Fabric release mc1.21.1-0.6.13-fabric |
| Iris | 1.8.8+mc1.21.1; Fabric release 1.8.8+1.21.1-fabric |
| Continuity | 3.0.0+1.21; published for both 1.21 and 1.21.1 |
| Build | Gradle 8.12, Fabric Loom 1.10.5 |

Sodium 0.6 supplies Fabric Renderer API support; Indium is not included. Iris's inspected artifact requires Sodium 0.6.x, so the newer 0.8.x Sodium builds advertised for 1.21.1 were **not** selected. The exact Sodium/Iris builds are enforced by mod metadata. Continuity is needed for CTM packs but not for atlas paging itself. `DEPENDENCIES.json` records artifact URLs, published version IDs and hashes; Gradle verifies dependency checksums. [UPSTREAM.md](UPSTREAM.md) records attribution and frozen source references.

## Build and install

With JDK 21, run `./gradlew build` (Windows: `gradlew.bat build`). Then run `python tools/validate_artifact.py`. The production artifact is `build/libs/pages-of-atlas-0.3.15-backport.1+1.21.1.jar`; the `-sources.jar` is not installable. The adjacent `.jar.sha256` contains its SHA-256. The wrapper distribution and wrapper JAR are pinned and checksum verified. No Minecraft launch is needed to build.

The GitHub Actions workflow builds only this project, runs tests, and uploads the production JAR, sources, checksum, reports, and synthetic test pack for 30 days. It needs only `contents: read` and uses pinned action commits. If testing a downloaded build, use the production JAR in a separate Fabric 1.21.1 test instance with the pinned dependencies. This project never installs itself into a profile. Do not enable two Pages of Atlas builds simultaneously.

For the optional offline GLSL compiler test, put Khronos glslang 16.6.0 on disk and set `GLSLANG_VALIDATOR` to its executable before building. CI does this automatically. Without that variable the GLSL test is explicitly skipped, not counted as passed. Run `python tools/test_generator.py` to check the standalone pack generator.

## Rendering implementation

The upstream deterministic packer partitions sprites into at most four physical 2D textures. A logical 2x1 or 2x2 atlas gives every sprite unique normalized coordinates, including Continuity/custom-model lookups. Shader sampling selects the physical page and converts logical UVs to local UVs. This avoids dropping textures, reducing resolution, reordering terrain passes, or changing Sodium's vertex format. Single-page atlases retain vanilla stitching and one physical texture, though shader routing helpers remain present.

The OpenGL upload adapter translates offsets at every mip level, including subsequent animation/interpolation uploads. Iris builds its normal/height and specular companions using the same logical sprite positions; those atlases inherit the diffuse page layout. Iris's LabPBR loading, default material values, mipmap generation, and animation synchronization stay in use. Native, Sodium and Iris sampling paths cover terrain, block items, particles and paintings in source. `atlasSize`, `gtextureSize` and rewritten `textureSize()` expose logical dimensions for POM coordinate calculations. A shader must itself implement LabPBR/POM to display those effects.

Physical page limits, page counts and versions are logged. Extra GL textures are released on replacement/deletion; PBR pages follow Iris's cleanup. Failed physical allocation or paged material upload is surfaced rather than treated as a successful diffuse-only result.

## Limits and unverified paths

- **No visual/GPU validation has been performed**, including full Mixin application, reloads, CTM, held/dropped/inventory blocks, transparency, particles, paintings, animated materials, and Patrix 128x/256x. Use [USER_TESTS.md](USER_TESTS.md).
- At most four pages per atlas. Physical dimensions never exceed the hardware limit or 16384; the latter keeps the virtual extent within Sodium 0.6's UV precision. A single sprite must fit one page. Total VRAM usage is not reduced.
- Up to three extra samplers per active atlas material are needed (nine with diffuse + normal + specular). Iris's existing allocator checks the GPU limit; a sampler-heavy shader may exceed it. Native paths check the limit too. This is not a guarantee that every shader fits.
- Direct `texture`/`texture2D`, LOD, explicit-gradient, integer-fetch, size and LOD-query sampling are handled. Indirect sampler parameters, texture gather/offset/projected operations, and shader-specific sampler aliases need additional adapters; detected unsupported operations fail explicitly. No private shader was inspected. POM support remains subject to the actual shader and user testing.
- Composite/Distant Horizons shaders are not rewritten. Distant Horizons, Physics Mod and Dynamic Trees custom rendering are unverified and may need their own adapters. No compatibility claim or dependency installation is made for them.
- Texture dump/debug exporters expecting one physical texture are not adapted to reconstruct a logical atlas. Shader errors, not screenshots alone, should be included in bug reports.

No textures from Patrix, Minecraft, or private shaders are distributed. Patrix 128x and 256x are alternative user-owned test packs. MIT upstream license and copyright are preserved.
