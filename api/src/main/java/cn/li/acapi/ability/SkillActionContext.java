package cn.li.acapi.ability;

import java.util.Map;
import java.util.UUID;

/**
 * Read-only skill invocation state plus a whitelisted set of side-effect
 * entry points, handed to a {@link SkillDefinition.ActionHandler} when it
 * runs.
 *
 * <p>Deliberately does not expose the underlying Player/Level objects —
 * every mutation goes through this whitelist, which forwards to the same
 * reducer-command / effects-interpreter pipeline that in-tree skills use.
 * Third-party skills cannot bypass the single-writer state model this way.
 */
public interface SkillActionContext {

    /** Id of the context driving this invocation (opaque, stable for the gesture's lifetime). */
    String ctxId();

    /** Caster's player id. */
    UUID playerId();

    /** This skill's id, as registered via {@link SkillDefinition#builder(String, String)}. */
    String skillId();

    /** Current skill experience/proficiency in [0.0, 1.0]. */
    double exp();

    /** Whether the resource cost for this invocation stage was successfully paid. */
    boolean costOk();

    /** Ticks the input has been held for hold/charge-style patterns (0 for instant patterns). */
    long holdTicks();

    /** Add skill experience (clamped/accumulated by the same path in-tree skills use). */
    void addSkillExp(double amount);

    /** Set (raise, never lower) this skill's main cooldown, in ticks. */
    void setMainCooldown(int ticks);

    /**
     * Send an FX event to the caster's client (and nearby players, depending
     * on the registered channel) under the given topic.
     */
    void sendFx(String topic, Map<String, Object> payload);

    /**
     * Apply direct damage to target, if the target and damage system are
     * available. damageType is a platform damage-type id, e.g. "generic".
     */
    void damageEntity(UUID target, double amount, String damageType);

    /**
     * Raycast from the caster's eye out to range and classify the hit.
     *
     * <p>Returned map mirrors cn.li.ac.ability.util.attack/resolve-attack-data:
     * keys "world-id", "eye", "look", "hit-kind" (one of "entity"/"block"/"miss"),
     * "target-uuid" (only for entity hits), "impact" (a {"x" "y" "z"} map).
     */
    Map<String, Object> resolveAttack(double range);
}
