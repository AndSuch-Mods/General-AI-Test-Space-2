# Pages of Atlas — unofficial Fabric 1.21.1 backport

Experimental **0.3.15-backport.2+1.21.1**, targeting Minecraft **1.21.1 exactly**, Java 21. Compilation and offline checks pass; actual Mixin application, GPU rendering, and full-pack compatibility remain unverified. MIT attribution to [mattmcbeardface](https://github.com/mattmcbeardface/pages-of-atlas) is preserved.

## Required runtime stack

| Component | Exact selection / internal Fabric version |
|---|---|
| Minecraft | 1.21.1, Fabric |
| Java | 21; class-file version 65 |
| Fabric Loader | Build/test pin **0.18.4**; runtime minimum 0.18.4 |
| Fabric API | 0.116.17+1.21.1; Modrinth Mys3P7lK |
| Sodium | mc1.21.1-0.8.12-fabric; **0.8.12+mc1.21.1**; KIRFiWG4 |
| Iris | 1.8.14-beta.1+1.21.1-fabric; **1.8.14-beta.1+mc1.21.1**; bAo1Qhte |

These are the user's exact rendering targets, not a provisional/latest selection. Continuity **3.0.0+1.21** (kSPJ4hQv) is optional for Pages of Atlas, but needed for Patrix connected textures. Do not add Indium or Sodium Options API. No gameplay mod is a hard dependency.

Loader 0.16.14 satisfies the core artifacts, but the requested Dynamic Trees target requires >=0.18.4; Sodium Extra 0.8.7 requires >=0.18. Thus this release pins 0.18.4 for the intended combination. See [COMPATIBILITY.md](COMPATIBILITY.md) for optional mods, old screenshot blockers, and additional optional-mod dependencies. Manifests, bundled dependencies, immutable IDs, filenames, source references and hashes are recorded in DEPENDENCIES.json and validation/dependency-manifests.json. Fabric's version parser checks the core dependency graph offline.

## Build and install

Use JDK 21. Run:

```text
python tools/fetch_shader_fixtures.py
./gradlew build
python tools/validate_artifact.py
python tools/test_generator.py
python tools/generate_test_pack.py
```

On Windows use `gradlew.bat`. Set `GLSLANG_VALIDATOR` to Khronos glslang 16.6.0 to run the offline compiler checks; without it those tests are explicitly skipped. Shader input downloads are public, checksum-pinned, ignored by Git and excluded from artifacts. No game launch is performed.

The production JAR is `build/libs/pages-of-atlas-0.3.15-backport.2+1.21.1.jar`, with an adjacent SHA-256 file. The `-sources.jar` is not installable. Use a separate Fabric test instance with the exact stack above and only one Pages of Atlas JAR. No installer or profile modifications are performed.

**Build-tool change:** selected Sodium/Iris JARs carry Loom 1.16.1/1.16.3 metadata. Loom 1.10.5 rejects them; Loom 1.16.3 requires Gradle >=9.4.0. The wrapper is therefore pinned to **Gradle 9.4.0 / Loom 1.16.3**, while Java remains 21. Loom now statically remaps mixin selectors instead of producing a refmap; the artifact validator checks the resulting intermediary selectors. Dependency checksum verification and required mixin checks remain enabled.

CI uses Java 21, fetches the pinned public shader inputs, compiles/remaps/tests, and uploads the JAR, checksum, test reports and original synthetic pack for 30 days. See [STATUS.md](STATUS.md) for the tested commit, final CI outcome and download. The previous 0.6.13/1.8.8 source/build checkpoint remains retrievable at **5f882c4b976056b7809f525e83448720e6aef537**; validation/backport1 records its build/run/checksum. Local preserved binaries are in ignored `.reference/checkpoint-backport1`.

## Rendering and material handling

The upstream deterministic packer assigns unique logical UVs across up to four physical textures. Physical uploads, mip levels and animation updates share one layout. Sodium binding now derives its reserved slots from the selected API. Both Sodium 0.8.12 and Iris's terrain encoder retain 32768 UV quantization; tests verify that contract. Physical pages remain <=16384 and within the hardware limit.

Iris's normal/height and specular atlases use the same layout as diffuse. Full RGBA values are retained, including LabPBR normal/height, reflectance, emission, porosity and subsurface data. Iris still supplies material-aware mip generation and animation. `atlasSize`, `gtextureSize` and routed `textureSize()` expose logical sizes for POM. The mod does not reduce resolution, drop textures, or disable PBR/POM/Continuity to fit.

The shader adapter specializes concrete atlas arguments passed to GLSL helper functions, including Bliss's `texture2D_POMSwitch`, while leaving non-atlas callers intact. Direct implicit/bias, LOD, gradient, integer-fetch, size and LOD-query sampling remain routed. Legacy gradient/LOD aliases are handled. Shader-owned ocean/noise/lightmap samplers are not treated as atlas pages. The synthetic pack includes height, reflection, emission and SSS patterns, plus animated materials.

## Limits and validation boundary

- Four pages maximum; each sprite must fit one page. Paging does not reduce VRAM use. Allocation failures and sampler exhaustion retain explicit errors; three materials can require nine extra samplers.
- Actual selected Bliss POMSwitch, Photon POM/self-shadow, and BSL LabPBR/POM/emission/SSS functions compile in minimal offline harnesses. This is not compilation of every shader program/option or a GPU test. See SHADERS.json and COMPATIBILITY.md.
- Generic helpers require concrete atlas arguments and an unambiguous signature. Recursive/overlapping helper expansion, sampler arrays/compound expressions, gather/offset/projected atlas sampling and unknown aliases need explicit adapters. Detected unsupported operations fail; arbitrary shader support is not claimed.
- Composite and DH programs use different samplers and are not rewritten. Physics ocean integration is preserved in shader sources; Physics Pro's local JAR was unavailable for inspection. DH, Physics, Dynamic Trees, EMF/ETF, ImmediatelyFast and ModernFix runtime behavior is unverified.
- CTM/custom models, transparent terrain, items, particles, paintings, reload/resource lifetime, mobs/death effects and both Patrix resolutions require the user's tests. Texture-dump reconstruction remains unimplemented.

No Patrix assets, paid Physics files, shader sources, game binaries, caches or credentials are distributed. Follow [USER_TESTS.md](USER_TESTS.md); test 128x and 256x separately.
