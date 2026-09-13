# Neutral Contract Reshape (Phase 1e)

Audit-first reshape of version-flavored NBT / CompoundTag vocabulary in `mcmod`
platform contracts toward domain names (`custom-data`, `structured-data`).
No Minecraft imports were added to `mcmod` / `ac`. No thin forwarders left for
deleted ops.

## Audit counts (`ac/src`, `*.clj` only)

| Symbol / namespace | Files | Notes |
|---|---:|---|
| `cn.li.mcmod.platform.nbt` | 16 | Renamed → `platform.structured-data` |
| `cn.li.mcmod.platform.item` | 24 | Namespace kept; tag-flavored ops renamed |
| `cn.li.mcmod.nbt.dsl` | **0** | No direct `ac` requires (used via mcmod `state-schema`) |
| `get-or-create-tag` | 4 | Renamed → `ensure-custom-data` |
| `tag-compound` | 5 | Renamed → `custom-data` |
| `set-tag!` | 7 | Renamed → `set-entry!` |
| `get-tag` | 1 | Renamed → `get-entry` |
| `has-tag?` | **0** | Never existed as public API; nothing to delete |
| `save-to-nbt` (item API) | 0 in ac call sites; used in mcmod helpers | Renamed → `save-to-data` |
| `from-nbt` (item API) | 0 qualified ac call sites | Renamed → `from-data` |
| `in-tag?` / `stack-from-tag` | 1 (`metal_former/recipes`) | Renamed → `in-item-tag?` / `stack-from-item-tag` |
| `:read-nbt-fn` / `:write-nbt-fn` | ~15+ block registration files | **Deferred** (see below) |
| Schema `:nbt-key` field | many block schemas | **Deferred** |

## Changed

### `cn.li.mcmod.platform.structured-data` (was `platform.nbt`)

- File: `mcmod/.../platform/nbt.clj` **deleted**; replaced by `structured_data.clj`.
- Framework path: `[:platform :nbt-ops]` → `[:platform :structured-data-ops]`.
- Installer: `install-nbt-ops!` → `install-structured-data-ops!`;
  `install-nbt-has-key-fn!` → `install-has-key-fn!`.
- Op keys: `:nbt-*` → `:sd-*`; `:create-compound` → `:create-structured`.
- Public wrappers:
  - `set-tag!` / `get-tag` → `set-entry!` / `get-entry`
  - `create-compound` / `get-compound` / `list-compound` →
    `create-structured` / `get-structured` / `list-structured`
  - `compound->map` → `structured->map`
- Primitive accessors (`set-int!`, `has-key?`, …) keep domain-neutral names.

### `cn.li.mcmod.platform.item`

| Old | New |
|---|---|
| `get-or-create-tag` | `ensure-custom-data` |
| `tag-compound` | `custom-data` |
| `save-to-nbt` | `save-to-data` |
| `from-nbt` | `from-data` |
| `in-tag?` | `in-item-tag?` |
| `stack-from-tag` | `stack-from-item-tag` |
| `:item-get-or-create-tag` | `:item-ensure-custom-data` |
| `:item-get-tag-compound` | `:item-get-custom-data` |
| `:item-save-to-nbt` | `:item-save-to-data` |
| `:create-item-from-nbt` | `:create-item-from-data` |
| `:item-tag-checker` | `:item-in-tag?` |
| `:tag-item-resolver` | `:item-tag-stack-resolver` |

### Platform installer

- `platform-src/minecraft/mc-1.20.1/.../installer_core.clj` updated for both
  structured-data and item ops (1.20.1 still binds to `CompoundTag` /
  `.getOrCreateTag` internally — that is the version seam, not the neutral
  contract).
- Loader `forge-1.20.1` / `fabric-1.20.1` init only installs
  `mcbase.platform.item-ops` (unchanged); item/structured-data ops install
  lives in mc-1.20.1.

### Call sites

- All former `platform.nbt` requires in `ac` / `mcmod` / mc-1.20.1 bootstrap
  now require `platform.structured-data` (alias `sd` / `platform-sd`).
- Item custom-data and metal-former item-tag call sites updated.
- Test stub `ac/.../test/support/nbt.clj` installs `:sd-*` keys via
  `install-test-structured-data-ops!` (no forwarder for old name).

### `platform.be`

- Already domain-oriented (`custom-state`); no NBT-flavored ops. No change.

## Deferred (cost > benefit this phase)

| Area | Why deferred | Approx. blast radius |
|---|---|---|
| `cn.li.mcmod.nbt.dsl` macros (`defnbt`, `defworldnbt`, `write-nbt-field`, …) | 0 direct `ac` requires; internal DSL still talks “NBT” but only used through `state-schema` | mcmod-internal + tests |
| Tile hooks `:read-nbt-fn` / `:write-nbt-fn` | Wired through `tile-dsl` / block registration across many content files | **>15 `ac` block files** (+ mcmod + mc-1.20.1 logic compile) — leave for a dedicated pass → `read-persisted-fn` / `write-persisted-fn` |
| Schema field key `:nbt-key` | Appears in every block state schema | large, mechanical; pair with DSL rename |
| Domain helpers named `*-from-nbt` / `*-to-nbt` in wireless / energy | Content-level serialization names, not platform installer contracts | wireless + energy modules |
| Test/support file path `test/support/nbt.clj`, `persistence/nbt_collections.clj` | File names only; behavior already on structured-data API | cosmetic |

## Invariants

- No `net.minecraft.*` (or Forge/Fabric) imports in `mcmod` / `ac`.
- Deleted ops have **no** thin forwarders / aliases.
- 1.20.1 installer remains the CompoundTag adapter; 1.21.1 will bind the same
  neutral keys to Data Components / `CUSTOM_DATA`.
