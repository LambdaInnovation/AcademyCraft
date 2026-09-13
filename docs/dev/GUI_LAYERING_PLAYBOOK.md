# GUI Layering Playbook

Use this when adding or changing GUI code.

## Placement

| Change | Location |
|--------|----------|
| Pure GUI spec / slot schema / metadata | `mcmod/src/main/clojure/cn/li/mcmod/gui/` |
| Business GUI presenter or screen factory | `ac/src/main/clojure/cn/li/ac/**/gui.clj` |
| Minecraft 1.20.1 menu/screen runtime | `platform-src/minecraft/mc-1.20.1/gui/` |
| Forge lifecycle/registration/network glue | `platform-src/loader/forge/` |
| Fabric lifecycle/registration/network glue | `platform-src/loader/fabric/` |

## Checklist

- Keep `ac` and `mcmod` free of Minecraft / Loader imports.
- Keep Loader components limited to registration, lifecycle and transport glue.
- Use `cn.li.mcmod.gui.registry` and metadata instead of scanning `ac`.
- Use selected target compile to verify Loader-specific changes.

```powershell
.`\\scripts\\target-gradle.ps1 forge-1.20.1`
.`\\scripts\\target-gradle.ps1 fabric-1.20.1`
```
