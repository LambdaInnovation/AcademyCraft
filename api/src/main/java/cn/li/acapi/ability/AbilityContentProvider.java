package cn.li.acapi.ability;

import java.util.List;
import java.util.Map;

/**
 * Discovered via Java {@link java.util.ServiceLoader} to register third-party
 * skills into the ability system.
 *
 * <p>Declare an implementation in
 * {@code META-INF/services/cn.li.acapi.ability.AbilityContentProvider} in
 * your mod's jar. Providers are collected once, during host mod bootstrap,
 * strictly before the skill/category registries are frozen — after that
 * point registration is rejected, so this must not be deferred.
 *
 * <p><b>Client-side note:</b> the host mod's skill tree, keybinds, network
 * sync, and player-state persistence are all data-driven off the registered
 * spec, so a correctly-built {@link SkillDefinition} gets those "for free".
 * FX, translations, and config are not managed by the host mod for
 * third-party skills — bring your own. Your mod jar must be present on
 * <b>both</b> the server and the client; the client needs the spec to build
 * skill-tree/HUD/keybind UI even though gameplay logic only runs server-side.
 */
public interface AbilityContentProvider {

    /** Stable provider id, e.g. your mod id. Used only for logging/diagnostics. */
    String providerId();

    /**
     * Categories this provider contributes. Most providers register their
     * skills into an existing host-mod category (electromaster, meltdowner,
     * teleporter, vecmanip) and can leave this empty.
     */
    default List<Map<String, Object>> categories() {
        return List.of();
    }

    /** Skills this provider contributes. */
    List<SkillDefinition> skills();
}
