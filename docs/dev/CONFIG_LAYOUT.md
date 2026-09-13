# Player-facing config layout

AcademyCraft config is organized by player/admin-facing domains. Each key has one canonical descriptor; alternate aliases are not kept.

## Domains and generated files

| Domain | Purpose | Example generated file |
| --- | --- | --- |
| `:cn.li.ac/wireless` | Core wireless network/node/matrix/search/performance rules | `cn.li.ac-wireless.toml` / JSON equivalent |
| `:cn.li.ac/wireless-devices` | Wireless-capable device balance and server performance knobs | `cn.li.ac-wireless-devices.toml` / JSON equivalent |
| `:cn.li.ac/gameplay` | General gameplay options outside ability/wireless | `cn.li.ac-gameplay.toml` / JSON equivalent |
| `:cn.li.ac/ability` | Global ability resource/progression/combat/runtime hooks | `cn.li.ac-ability.toml` / JSON equivalent |
| `:cn.li.ac/ability-devices` | Developer and Ability Interferer device balance knobs | `cn.li.ac-ability-devices.toml` / JSON equivalent |
| `:cn.li.ac/ability-skills-*` | Per-category skill balance tunables | `cn.li.ac-ability-skills-<category>.toml` / JSON equivalent |

Forge groups descriptors by `:section`; Fabric writes nested JSON from dotted `:path`.

## What belongs in player config

- Balance/rule values: damage, range, cost, cooldown, capacity, bandwidth, energy generation, recovery rates, growth coefficients.
- Server/admin performance intervals when they meaningfully affect load, such as machine validation or sync cadence.
- Lists that server owners can reason about, such as target block/entity ID lists.

## What should stay in code/content data

- Registry IDs, NBT keys, slot indexes, GUI coordinates, packet details, and schema shape.
- Rendering, particle, sound, animation, and purely client-side FX sync details.
- Raycast epsilon/step constants and other implementation-only geometry helpers unless they become intentional gameplay rules.
- Skill prerequisites/skill graphs. The current descriptor bridge has scalar/list types only; if the skill graph becomes data-driven, use dedicated content data rather than TOML scalar descriptors.

## Guardrails

- Descriptor types must stay within `:int`, `:double`, `:boolean`, `:string`, `:string-list`, `:int-list`, and `:double-list`.
- Paths must be dotted player-facing names such as `network.update-interval-ticks` or `generators.wind.energy.max-energy`.
- Do not add a descriptor unless production runtime reads it through a getter or the value is intentionally diagnostic-only and documented.
