# AC Module Layering

`ac` owns AcademyCraft business logic and content. It must stay independent from Minecraft and Loader APIs.

## Layers

| Layer | Typical namespace | Responsibility |
|-------|-------------------|----------------|
| Content | `cn.li.ac.content.*` | Block/item/ability declarations and metadata registration. |
| Domain | `cn.li.ac.<system>.domain.*` | Pure rules and transformations. |
| Service | `cn.li.ac.<system>.service.*` | Application orchestration and transactions. |
| Data | `cn.li.ac.<system>.data.*` | Records, persistence codecs, world/runtime storage boundaries. |
| Runtime effects | `cn.li.ac.<system>.runtime.*` or `effects.*` | Explicit side-effect boundary behind protocols/API. |
| GUI presenter | `cn.li.ac.<system>.gui.*` | Business GUI state, presenters, factory wiring through `mcmod`. |

## Rules

- No `net.minecraft.*`, Forge or Fabric imports in `ac`.
- Cross-system access goes through documented public APIs, such as `cn.li.ac.wireless.api` and `cn.li.ac.energy.operations`.
- Public APIs should be direct and meaningful; do not add pass-through namespaces.
- Platform behavior is injected through `mcmod` contracts or explicit platform adapters.
- For input timing, the platform/input-protocol layer owns the decision of short press vs. long hold; the `ac` business layer only consumes the already-filtered event and executes the corresponding action.

## Verification

```powershell
.\gradlew.bat :ac:test
.\gradlew.bat verifyCurrentPlatforms
```
