# Wireless Matrix GUI

Wireless Matrix GUI is an `ac` business GUI. It shares the same GUI protocol, XML runtime, slot schema and platform menu runtime as Wireless Node GUI.

## Ownership

| Concern | Location |
|---------|----------|
| Matrix GUI spec and handlers | `ac/src/main/clojure/cn/li/ac/block/wireless-matrix/gui.clj` |
| Wireless domain state | `ac/src/main/clojure/cn/li/ac/wireless/` |
| GUI protocol and registry | `mcmod/src/main/clojure/cn/li/mcmod/gui/` |
| Minecraft 1.20.1 runtime | `platform-src/minecraft/mc-1.20.1/gui/` |
| Loader registration | `platform-src/loader/<loader>/` |

## Rules

- Use `cn.li.ac.wireless.api` for topology and snapshot access.
- Keep presenter formatting in `ac`.
- Keep Loader code limited to registration and network/menu glue.
- Keep generated resources under `platform/build/targets/<target-id>/platform/`, not source directories.
