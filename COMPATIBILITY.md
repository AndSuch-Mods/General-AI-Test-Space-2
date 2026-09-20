# Compatibility evidence and pack-side changes

These are optional compatibility targets, not Pages of Atlas hard dependencies. Metadata acceptance is not runtime/rendering validation. The recently created MultiMC instance `1.21.1 Realism Revamp` was located and its manifests/JAR bytecode inspected read-only on 2026-09-20. Nothing was installed, changed or launched. These observed versions are not newly approved version locks.

## Historical screenshot blockers and candidates

| Mod | Inspected original internal version | Action / inspected replacement candidate |
|---|---|---|
| Sodium Extra | 0.6.0-beta.3+mc1.21.1 (CEAENzuT) | Sodium 0.8.12 breaks <0.8.7. Disable the old build. 0.8.7+mc1.21.1 (ECiITTpZ) accepts Sodium >=0.8.12-alpha.3 and requires Loader >=0.18. |
| Cull Leaves | 3.4.0 (JnCn3KSm) | Keep the old build disabled. Candidate 4.1.1 (b3VxU7eO), filename cullleaves-fabric-4.1.1+1.21.1.jar; includes MidnightLib 1.9.2 and accepts 1.21.1. |
| Reese's Sodium Options | 1.8.0-beta.4+mc1.21.1 (DSxKPh7H) | Sodium breaks <1.8.0, including this prerelease. Candidate 2.0.5+mc1.21.1 (24WhugSw) accepts Sodium >=0.8.12-alpha.3 and also satisfies Sodium Extra's >=1.8.4 rule. |

These candidate JAR manifests were actually downloaded and inspected; no optional mod was installed. Keep them disabled for the first rendering baseline, then test them individually. Their internal APIs and settings screens have not been exercised in game. Do not substitute 0.8.13 Sodium, add Indium/Sodium Options API, or remove declared incompatibility checks.

**Cull Leaves declaration gap:** Sodium's exact break is `<=3.4.0-fabric`, but the original JAR declares `3.4.0` internally. Fabric's actual version comparator does NOT match that stable version to the prerelease upper bound; this is tested. A loader that accepts this combination does not establish support. The newer candidate avoids relying on that gap.

## Other optional targets

- **Distant Horizons 3.3.1**, IcOcoekl, DistantHorizons-3.3.1-1.21.1-fabric-neoforge.jar: a dual-loader artifact with an inspected Fabric manifest, not a NeoForge installation. Its Minecraft/Java and Iris break ranges admit the selected core stack. Its LOD shaders/vertex format are distinct; near/far terrain, water seams and dimension transitions still need testing.
- **Dynamic Trees 1.7.2-BETA**, AejMebCy, internal version **1.7.2**: requires Minecraft 1.21.1, Java >=21, Loader >=0.18.4, Fabric API and **Forge Config API Port**. FCAP is not bundled. The local instance contains Forge Config API Port **21.1.6** (`ForgeConfigAPIPort-v21.1.6-1.21.1-Fabric.jar`); its manifest accepts Minecraft 1.21.1, Loader >=0.16.1 and Fabric API >=0.102.0. This is an observed local candidate, not a new runtime dependency or a full compatibility test. This is an optional mod dependency, not a Pages of Atlas dependency. Tree geometry, leaves and felling/death effects are unverified.
- **Physics Mod Pro v188b:** inspected the legitimately installed `physics-mod-pro-v188b-fabric-mc-1.21.1.jar`; internal version **3.0.33**. Requires Minecraft 1.21.1, Java >=21, Loader >=0.16 and Fabric API >=0.75. It is outside Iris's `physicsmod <=3.0.13` break. An offline bytecode scan resolves 146 direct Iris/Sodium callsites, including inherited Java methods, against the selected artifacts. Its sampler-reservation hook uses Iris's reserved-unit path, which our dynamic page samplers also respect. The JAR has Sodium 0.8 adapters and Physics shader-preprocessing hooks. Twenty-eight inspected shader resources use only texture, textureGrad, textureLod, textureSize and texelFetch among sampling operations; this is not full-program shader validation. Ocean injection, custom geometry, mixin application and effects remain unverified. No paid bytes or decompiled sources are published.
- **EMF, ETF, ImmediatelyFast, ModernFix:** observed **2.2.6 / 6.2.5 / 1.2.21+1.21.1 / 5.19.3+mc1.21.1** respectively. Updated versions remain unlocked. EMF's three direct Iris/Sodium method calls resolve. ETF has an old Sodium adapter calling a removed helper; that `@Pseudo` mixin targets the absent old `me.jellysquid...EntityRenderer` class, so static evidence indicates it is inactive, not a supported adapter for Sodium 0.8. Do not infer correct emissive/custom-entity rendering. ImmediatelyFast and ModernFix have no direct method calls into the scanned Iris/Sodium namespaces; that does not validate their mixins or rendering. ModernFix's fast vanilla Stitcher injection is bypassed by our overriding packer, while its other changes remain unverified.
- **Continuity 3.0.0+1.21:** selected artifact supports 1.21.1. The normal SpriteLoader return and Continuity emissive-linking hook are preserved; logical UV uniqueness is preserved for sprite lookup and custom block models; full Patrix CTM remains a visual test.

## Shader evidence

All shader inputs stayed in ignored local/CI work directories and are excluded from the build bundle. POM/material options and ocean support are not disabled as a workaround.

| Exact source | Inspected material path | Offline evidence / remaining boundary |
|---|---|---|
| Bliss Unstable 82df914cc43743e1c99df5fc74f354d7de19ced6 | dimensions/all_solid.fsh: texture2D_POMSwitch takes a sampler argument, uses texture2DGradARB or biased texture; normal alpha carries height, specular B/A carry SSS/emission. | Real helper specialized for all three atlas materials and compiled. lib/oceans.glsl retains PHYSICS_OCEAN_SUPPORT/PHYSICS_OCEAN and physics_* samplers. Whole shaders/options, ocean/terrain transitions and GPU POM remain untested. |
| Photon main 15458c0937f8647c37eb6a501bef5eb3bf3da31b | include/surface/parallax.glsl: textureGrad(normals, ...) alpha, atlas-local conversion, POM ray and self-shadow loops. | Actual complete POM file compiled with its global inputs and rcp macro semantics. Full material/water/DH programs remain untested. |
| BSL 10.1.5, yFTiE1Nc | Author's capttatsu.com distribution links Modrinth. lib/surface/parallax.glsl uses gradient height sampling; materialGbuffers.glsl uses _s RGBA, LOD and normal/height gradients. | Actual GGX lighting (including local `specular`), POM/self-shadow and LabPBR material functions compiled with PARALLAX, MATERIAL_FORMAT=1 and emission/SSS paths active. Whole program/options and ocean rendering remain untested. |

Bliss explicitly includes Physics ocean hooks in lib/oceans.glsl. No PHYSICS_OCEAN/physics_waviness/physics_ocean integration hooks were found in the selected Photon or BSL source trees; their native water code remains unchanged. The inspected v188b JAR includes its own preprocessing/injection paths; their output with these complete shader programs remains unverified. Do not interpret preserved water code as proven Physics ocean compatibility.

Iris 1.8.14-beta.1's CommonTransformer converts legacy texture2DGradARB/texture2DLod names before our hook; the fixtures perform those same operation renames. The fixtures supply shader globals and compile material functions, not stubs replacing those functions. An initial Photon fixture failed because rcp was expressed as a function in the harness; matching the source macro fixed it. No pack-side POM change was made.

Known unsupported cases: ambiguous same-arity sampler-helper overloads, recursive/overlapping specialization, sampler arrays/compound sampler expressions, projected/gather/offset atlas operations, and unrecognized atlas aliases. None of these was needed by the inspected POM functions. Shader transformation and synthetic tests cannot establish full shader-pack compatibility or actual runtime Mixin application.

## Required changes to the observed test instance

The inspected instance still has Loader **0.16.5**, Fabric API **0.105.0+1.21.1**, Sodium **0.6.0-beta.2+mc1.21.1**, Iris **1.8.0-beta.4+mc1.21.1**, and Continuity **3.0.0-beta.5+1.21**. Before testing this backport, use the exact newer pins in README.md in a separate test instance. The original Sodium Extra, Reese's Options and Cull Leaves builds are also present; leave those disabled for the baseline, or use the inspected candidates above. Do not install Indium or Sodium Options API. No automatic replacement was made.

`validation/local-mod-inspection.json` records the observed filenames, versions and hashes without containing paid code/assets. Local inspection notes and bytecode output stay in ignored `.reference/fixes`. This is a static API/manifest review only: EMF/ETF entity emission, ImmediatelyFast batching, Physics/Dynamic Trees effects and DH water transitions still require manual tests.
