package cn.li.ac.ability.integration.fixture;

import cn.li.acapi.ability.AbilityContentProvider;
import cn.li.acapi.ability.SkillActionContext;
import cn.li.acapi.ability.SkillDefinition;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal third-party ability provider used by external_providers_test.clj to
 * exercise the real ServiceLoader discovery path end to end.
 *
 * <p>Declared in
 * src/test/resources/META-INF/services/cn.li.acapi.ability.AbilityContentProvider.
 */
public final class FixtureAbilityProvider implements AbilityContentProvider {

    /** Bumped by the fixture skill's onPerform handler; tests assert on it. */
    public static final AtomicInteger PERFORM_COUNT = new AtomicInteger(0);

    @Override
    public String providerId() {
        return "fixture-provider";
    }

    @Override
    public List<SkillDefinition> skills() {
        return List.of(
            SkillDefinition.builder("fixture-provider-test-strike", "electromaster")
                .nameKey("fixture.skill.test_strike")
                .level(2)
                .controllable(true)
                .pattern(SkillDefinition.Pattern.INSTANT)
                .onPerform(this::onPerform)
                .build());
    }

    private void onPerform(SkillActionContext ctx) {
        PERFORM_COUNT.incrementAndGet();
        ctx.addSkillExp(0.01);
        ctx.setMainCooldown(20);
    }
}
