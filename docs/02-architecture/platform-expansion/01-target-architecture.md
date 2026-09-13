# Target architecture

The platform architecture is target-catalog driven.

## Shape

```text
api/
mcmod/
ac/

platform-src/
  common/
  minecraft/
    base/
    version/mc-1201/
  loader/
    forge/
    fabric/
  test-support/

platform/
platform-catalog.json
build-logic/
```

## Rules

- `platform-catalog.json` is the only supported-target directory.
- Each target explicitly declares loader, Minecraft version, Java version, source components, test components, capabilities, dependencies, and artifact metadata.
- Build logic must read the target model; it must not parse behavior from target id strings.
- The repository must not generate all loader/version combinations.
- Adding a hypothetical future combination is an architecture test, not a support promise.
