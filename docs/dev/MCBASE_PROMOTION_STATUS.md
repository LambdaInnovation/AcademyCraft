# Minecraft-base promotion status (Phase 3a)

## Already in `cn.li.mcbase` (pre-existing + this pass)

### Java (safe, no `net.minecraft` / loader coupling) — promoted this pass
- `clj/ClojureInterop.java`
- `datagen/DataGeneratorInterop.java`
- `client/audio/OggMetadata.java`
- `client/font/msdf/MsdfGlowAnimator.java`
- `client/font/msdf/MsdfGlyphFlags.java`
- `client/font/msdf/MsdfTextFx.java`

### Pre-existing base Java (runtime adapters / entity specs / shims)
- `entity/hook/AbstractHookRegistry.java`
- `entity/spec/Scripted*Spec.java` (5)
- `runtime/*Adapter.java`, `RuntimeAccessorRegistry.java`
- `shim/FnConsumer|FnPredicate|FnSupplier.java`

### Pre-existing base Clojure
~48 namespaces under `cn.li.mcbase.*` (runtime SPI, datagen cores, gui reactive shells, platform ops, etc.)

## Deferred (must stay versioned)

Identical-across-1.20.1/1.21.1 files that still **import Minecraft / version entity types** cannot live in
`minecraft-base` without redesigning them onto opaque handles or new `mcver` members.
Examples that failed an earlier lift attempt:

- `block/IScriptedBlock` + `block/logic/TileLogicBundle` + `ITile*Logic` (depend on versioned BE / MC types)
- `entity/logic/IMob*Logic` + `MobLogicBundle` + `FnMobTickLogic` (depend on `ScriptedMobEntity`)
- `entity/hook/{effect,marker,ray}/Scripted*Hook(s)` (method signatures take versioned entity + MC client types)
- All ~170 “byte-identical modulo ns” files that reference `net.minecraft.*` — keep in
  `mc-1.20.1` / `mc-1.21.1` / port forks into `mc-26.2` instead of forcing a base lift.

Promotion rule going forward: only lift a file to `mcbase` when (1) no MC/loader imports,
(2) no references to versioned concrete types, (3) both loom and mdg targets compile after the move.
