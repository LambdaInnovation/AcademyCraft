# Script Render Governance

Script rendering code crosses `ac`, `mcmod`, Minecraft-version components and Loader glue. Keep ownership explicit.

## Compile gates

```powershell
.\gradlew.bat :ac:compileClojure :mcmod:compileClojure
.`\\scripts\\target-gradle.ps1 forge-1.20.1`
.`\\scripts\\target-gradle.ps1 fabric-1.20.1`
.\gradlew.bat verifyCurrentPlatforms
```

## Rules

- Data model and render intent live in `ac` / `mcmod`.
- Minecraft render implementation lives in `platform-src/minecraft/mc-1.20.1/`.
- Loader lifecycle and registration live in `platform-src/loader/<loader>/`.
- Do not add direct `ac` imports to Loader components.
