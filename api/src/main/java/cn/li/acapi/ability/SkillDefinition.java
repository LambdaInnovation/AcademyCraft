package cn.li.acapi.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable, builder-constructed definition of one ability skill contributed
 * by a third-party {@link AbilityContentProvider}.
 *
 * <p>A builder (rather than a bare {@code Map}) so that field names are
 * discoverable at compile time, invalid combinations fail fast in
 * {@link Builder#build()} instead of surfacing as an obscure registration
 * error deep in the host mod, and new optional fields can be added later
 * without breaking existing callers.
 *
 * <p><b>level/controllable are required</b> — unlike in-tree skills (which
 * source those two fields from a central config table), third-party skills
 * have no entry there and must supply both directly.
 */
public final class SkillDefinition {

    /** Skill activation lifecycle shapes, mirroring the in-tree skill schema. */
    public enum Pattern {
        INSTANT("instant"),
        HOLD_CHARGE_RELEASE("hold-charge-release"),
        HOLD_CHANNEL("hold-channel"),
        TOGGLE("toggle"),
        RELEASE_CAST("release-cast"),
        CHARGE_WINDOW("charge-window"),
        PASSIVE("passive");

        final String id;

        Pattern(String id) {
            this.id = id;
        }
    }

    /** Cost/cooldown stages a resource amount can be attached to. */
    public enum CostStage {
        DOWN("down"), TICK("tick"), UP("up");

        final String id;

        CostStage(String id) {
            this.id = id;
        }
    }

    @FunctionalInterface
    public interface ActionHandler {
        void handle(SkillActionContext ctx);
    }

    @FunctionalInterface
    public interface CostFn {
        /** Resource amount to charge for one invocation of this stage. */
        double compute(UUID playerId, String skillId, double exp);
    }

    @FunctionalInterface
    public interface CooldownFn {
        /** Cooldown length, in ticks, for the given invocation. */
        int compute(UUID playerId, String skillId, double exp);
    }

    public static final class Prerequisite {
        private final String skillId;
        private final double minExp;

        public Prerequisite(String skillId, double minExp) {
            this.skillId = Objects.requireNonNull(skillId, "skillId");
            this.minExp = minExp;
        }

        public String skillId() {
            return skillId;
        }

        public double minExp() {
            return minExp;
        }
    }

    private final String id;
    private final String categoryId;
    private final int level;
    private final boolean controllable;
    private final String nameKey;
    private final String descriptionKey;
    private final String icon;
    private final int uiPositionX;
    private final int uiPositionY;
    private final String ctrlId;
    private final Pattern pattern;
    private final Map<CostStage, Map<String, CostFn>> costs;
    private final Object cooldownTicks; // Integer or CooldownFn
    private final List<Prerequisite> prerequisites;
    private final Map<String, ActionHandler> actionHandlers;

    private SkillDefinition(Builder b) {
        this.id = b.id;
        this.categoryId = b.categoryId;
        this.level = b.level;
        this.controllable = b.controllable;
        this.nameKey = b.nameKey;
        this.descriptionKey = b.descriptionKey;
        this.icon = b.icon;
        this.uiPositionX = b.uiPositionX;
        this.uiPositionY = b.uiPositionY;
        this.ctrlId = b.ctrlId != null ? b.ctrlId : b.id;
        this.pattern = b.pattern;
        this.costs = deepCopyCosts(b.costs);
        this.cooldownTicks = b.cooldownTicks;
        this.prerequisites = List.copyOf(b.prerequisites);
        this.actionHandlers = Collections.unmodifiableMap(new LinkedHashMap<>(b.actionHandlers));
    }

    private static Map<CostStage, Map<String, CostFn>> deepCopyCosts(Map<CostStage, Map<String, CostFn>> src) {
        Map<CostStage, Map<String, CostFn>> out = new LinkedHashMap<>();
        for (Map.Entry<CostStage, Map<String, CostFn>> e : src.entrySet()) {
            out.put(e.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(e.getValue())));
        }
        return Collections.unmodifiableMap(out);
    }

    public String id() {
        return id;
    }

    public String categoryId() {
        return categoryId;
    }

    public int level() {
        return level;
    }

    public boolean controllable() {
        return controllable;
    }

    public String nameKey() {
        return nameKey;
    }

    public String descriptionKey() {
        return descriptionKey;
    }

    public String icon() {
        return icon;
    }

    public int uiPositionX() {
        return uiPositionX;
    }

    public int uiPositionY() {
        return uiPositionY;
    }

    public String ctrlId() {
        return ctrlId;
    }

    public String pattern() {
        return pattern.id;
    }

    /** {@code {"down"|"tick"|"up" -> {"cp"|"overload" -> CostFn}}}. */
    public Map<String, Map<String, CostFn>> costs() {
        Map<String, Map<String, CostFn>> out = new LinkedHashMap<>();
        for (Map.Entry<CostStage, Map<String, CostFn>> e : costs.entrySet()) {
            out.put(e.getKey().id, e.getValue());
        }
        return out;
    }

    /** {@code Integer} for a fixed value, {@code CooldownFn} for a computed one, or {@code null}. */
    public Object cooldownTicks() {
        return cooldownTicks;
    }

    public List<Prerequisite> prerequisites() {
        return prerequisites;
    }

    /** Keyed by the in-tree action key: "perform!", "activate!", "deactivate!",
     * "down!", "tick!", "up!", "abort!", "cost-fail!". */
    public Map<String, ActionHandler> actionHandlers() {
        return actionHandlers;
    }

    public static Builder builder(String id, String categoryId) {
        return new Builder(id, categoryId);
    }

    public static final class Builder {
        private final String id;
        private final String categoryId;
        private int level = 1;
        private boolean controllable = true;
        private String nameKey;
        private String descriptionKey;
        private String icon;
        private int uiPositionX;
        private int uiPositionY;
        private String ctrlId;
        private Pattern pattern = Pattern.INSTANT;
        private final Map<CostStage, Map<String, CostFn>> costs = new LinkedHashMap<>();
        private Object cooldownTicks;
        private final List<Prerequisite> prerequisites = new ArrayList<>();
        private final Map<String, ActionHandler> actionHandlers = new LinkedHashMap<>();

        private Builder(String id, String categoryId) {
            this.id = Objects.requireNonNull(id, "id");
            this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        }

        public Builder level(int level) {
            this.level = level;
            return this;
        }

        public Builder controllable(boolean controllable) {
            this.controllable = controllable;
            return this;
        }

        public Builder nameKey(String nameKey) {
            this.nameKey = nameKey;
            return this;
        }

        public Builder descriptionKey(String descriptionKey) {
            this.descriptionKey = descriptionKey;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder uiPosition(int x, int y) {
            this.uiPositionX = x;
            this.uiPositionY = y;
            return this;
        }

        /** Controllable-skill keybind slot id. Defaults to {@code id} when omitted. */
        public Builder ctrlId(String ctrlId) {
            this.ctrlId = ctrlId;
            return this;
        }

        public Builder pattern(Pattern pattern) {
            this.pattern = Objects.requireNonNull(pattern, "pattern");
            return this;
        }

        private Builder cost(CostStage stage, String resource, CostFn fn) {
            costs.computeIfAbsent(stage, k -> new LinkedHashMap<>()).put(resource, fn);
            return this;
        }

        public Builder costDownCp(CostFn fn) {
            return cost(CostStage.DOWN, "cp", fn);
        }

        public Builder costDownOverload(CostFn fn) {
            return cost(CostStage.DOWN, "overload", fn);
        }

        public Builder costTickCp(CostFn fn) {
            return cost(CostStage.TICK, "cp", fn);
        }

        public Builder costTickOverload(CostFn fn) {
            return cost(CostStage.TICK, "overload", fn);
        }

        public Builder costUpCp(CostFn fn) {
            return cost(CostStage.UP, "cp", fn);
        }

        public Builder costUpOverload(CostFn fn) {
            return cost(CostStage.UP, "overload", fn);
        }

        public Builder cooldownTicks(int ticks) {
            this.cooldownTicks = ticks;
            return this;
        }

        public Builder cooldownTicks(CooldownFn fn) {
            this.cooldownTicks = fn;
            return this;
        }

        public Builder prerequisite(String skillId, double minExp) {
            this.prerequisites.add(new Prerequisite(skillId, minExp));
            return this;
        }

        public Builder onPerform(ActionHandler handler) {
            actionHandlers.put("perform!", handler);
            return this;
        }

        public Builder onActivate(ActionHandler handler) {
            actionHandlers.put("activate!", handler);
            return this;
        }

        public Builder onDeactivate(ActionHandler handler) {
            actionHandlers.put("deactivate!", handler);
            return this;
        }

        public Builder onKeyDown(ActionHandler handler) {
            actionHandlers.put("down!", handler);
            return this;
        }

        public Builder onKeyTick(ActionHandler handler) {
            actionHandlers.put("tick!", handler);
            return this;
        }

        public Builder onKeyUp(ActionHandler handler) {
            actionHandlers.put("up!", handler);
            return this;
        }

        public Builder onAbort(ActionHandler handler) {
            actionHandlers.put("abort!", handler);
            return this;
        }

        public Builder onCostFail(ActionHandler handler) {
            actionHandlers.put("cost-fail!", handler);
            return this;
        }

        /**
         * Validate and build. Throws {@link IllegalStateException} when a
         * required field is missing or the action handlers registered don't
         * satisfy {@link #pattern(Pattern)}'s requirements — mirrors the
         * in-tree skill-schema pattern/action contract so a misconfigured
         * skill fails at provider-build time, not at first cast.
         */
        public SkillDefinition build() {
            if (nameKey == null) {
                throw new IllegalStateException("SkillDefinition " + id + ": nameKey is required");
            }
            requireActions(requiredActionsFor(pattern));
            return new SkillDefinition(this);
        }

        private static List<String> requiredActionsFor(Pattern pattern) {
            switch (pattern) {
                case INSTANT:
                case HOLD_CHARGE_RELEASE:
                    return List.of("perform!");
                case TOGGLE:
                    return List.of("activate!", "deactivate!");
                default:
                    return List.of();
            }
        }

        private void requireActions(List<String> required) {
            List<String> missing = new ArrayList<>();
            for (String key : required) {
                if (!actionHandlers.containsKey(key)) {
                    missing.add(key);
                }
            }
            if (!missing.isEmpty()) {
                throw new IllegalStateException(
                    "SkillDefinition " + id + ": pattern " + pattern.id + " requires action handler(s) " + missing);
            }
        }
    }
}
