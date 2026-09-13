# Wireless Node GUI

Wireless Node GUI is an `ac` business GUI backed by `mcmod` GUI spec and the Minecraft 1.20.1 platform GUI runtime.

## Ownership

| Concern | Location |
|---------|----------|
| Business GUI spec | `ac/src/main/clojure/cn/li/ac/block/wireless-node/gui.clj` |
| Wireless tab/presenter | `ac/src/main/clojure/cn/li/ac/wireless/gui/` |
| GUI protocol and slot schema | `mcmod/src/main/clojure/cn/li/mcmod/gui/` |
| Minecraft 1.20.1 menu/runtime | `platform-src/minecraft/mc-1.20.1/gui/` |
| Loader registration | `platform-src/loader/<loader>/` |

## Resource paths

GUI XML and textures live under `ac/src/main/resources/assets/academy/`.

The GUI should access wireless state through `cn.li.ac.wireless.api` and presenter helpers, not by mutating wireless world-state directly.

## Verification

```powershell
.\gradlew.bat :ac:test
.`\\scripts\\target-gradle.ps1 forge-1.20.1`
```
