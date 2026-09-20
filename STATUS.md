# Backport.3 checkpoint

Target: Minecraft **1.21.1**, Java **21**, Sodium **0.8.12+mc1.21.1**, Iris **1.8.14-beta.1+mc1.21.1**, Fabric API **0.116.17+1.21.1**, Loader **0.18.4**, optional Continuity **3.0.0+1.21**. No runtime or build-tool pins changed in this correction; Gradle 9.4.0 / Loom 1.16.3 remain as previously required. Parser compile/test dependencies match the libraries already bundled by Iris; no extra runtime installation.

Base/history preserved: **6e32a27c19e7a856af33326a9b104cf25ced03b2**. Backport.2 JAR and CI ZIP are preserved in ignored `.reference/checkpoint-backport2`; its reports are in `validation/backport2`. Earlier 0.6.13/1.8.8 source checkpoint **5f882c4b976056b7809f525e83448720e6aef537** and backport.1 evidence remain retrievable. No reset, force-push, or other repository/profile modification.

Implemented all six reviewed fixes:

1. Scope-aware atlas sampler references using Iris's exact bundled GLSL AST parser. Real BSL GGX lighting with a local `specular` value now compiles.
2. Iris integer/float/vector material texture settings broadcast to every physical page, including LabPBR swizzles and DSA/fallback paths.
3. Replace only the stitcher, preserving normal SpriteLoader preparation/return and Continuity's emissive-linking hook.
4. Use fresh packed dimensions on each reload, including multiple pages back to one. Allocate only the one to four pages needed.
5. Fan out single-listener Iris PBR notifications to base/extra page bindings and notify diffuse pages on texture changes. Listener detachment follows Iris program lifecycle; grids use each actual material family.
6. Check dimensions and GL errors at every requested mip during allocation, then log a detailed failure and release extra pages if allocation fails.

Optimization: cache uniform locations/unchanged values and invalidate on supported program deletion/relink paths. Shader AST work is at transformation time; logs are at resource setup/failure, not every frame. No FPS or memory benchmark was run; extra sampler and sampling costs remain.

Offline validation passed: clean Java 21 compilation/remap; **25 JUnit tests, zero failures/errors/skips**; real pinned Bliss, Photon, BSL POM/material and BSL GGX functions compiled by **glslang 16.6.0**; synthetic oversized-atlas/UV/mip tests; material notifier transition from three/four pages to ordinary texture without shader reapplication; all **34 static mixin target methods** and shadow descriptors; exact redirect callsites and non-cancelling stitch hook; Python synthetic generator test and production artifact validation (**44 classes, 19 required mixins, Java class version 65**). Dependency verification remains enabled. Historical test assertion/verification-file issues were corrected; no remaining local offline failure. Actual Mixin application is not exercised by static target checks.

Production JAR: `build/libs/pages-of-atlas-0.3.15-backport.3+1.21.1.jar`.
SHA-256: `de75f15190faa617bba0401e15ef212e3600f053c122e72040b01e6ff3379d72`.
Remote CI/download: pending publication of this checkpoint. See `validation/local-validation.json` for the current local evidence.

Read-only MultiMC inspection found Physics Pro v188b (internal 3.0.33), EMF 2.2.6, ETF 6.2.5, ImmediatelyFast 1.2.21+1.21.1 and ModernFix 5.19.3+mc1.21.1. Physics's 146 direct Iris/Sodium callsites resolve; one legacy ETF adapter targets an absent old Sodium class and remains an entity-rendering test concern. The instance still has old core rendering mods/Loader; required test-instance changes and shader boundaries are in COMPATIBILITY.md. No paid JAR/source/assets are distributed.

Not performed: runtime Mixin application, GPU allocations/parameter propagation, whole-shader/all-option compilation, Patrix 128x/256x visual checks, full modpack validation, FPS/VRAM measurements. Ocean support, POM and material channels were not disabled. Shader helper/operation limitations remain explicit in README.md. The user launches Minecraft using USER_TESTS.md.

Resume with JDK 21 and `GLSLANG_VALIDATOR` pointing to glslang 16.6.0: `python tools/fetch_shader_fixtures.py`, `./gradlew build`, `python tools/validate_artifact.py`, `python tools/test_generator.py`, `python tools/generate_test_pack.py`. On this Windows host use `.reference/java` and `.reference/glslang/bin`; `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` uses Windows certificates without disabling TLS. Fresh logs and private inspection notes are in ignored `.reference/fixes`. Never publish that directory or `build/shader-inputs`.
