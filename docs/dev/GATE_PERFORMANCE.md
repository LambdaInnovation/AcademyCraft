# Gate Performance

This document keeps the verification set small and predictable.

| Gate | Scope | Command |
|------|-------|---------|
| Architecture | repository layout, target catalog, source-derived AOT inputs, residue guards | `.\gradlew.bat verifyCurrentPlatforms` |
| Core unit tests | `ac` / `mcmod` platform-neutral tests | `.\gradlew.bat :ac:test :mcmod:test` |
| Forge target compile | Forge source components | `.`\\scripts\\target-gradle.ps1 forge-1.20.1`` |
| Fabric target compile | Fabric source components | `.`\\scripts\\target-gradle.ps1 fabric-1.20.1`` |
| DataGen parity | generated hash manifests | `.\gradlew.bat compareDatagenParityManifests` |

CI should fan out target-specific work as a matrix. Local development should run the smallest gate that covers the changed area, then `verifyCurrentPlatforms` before handoff.
