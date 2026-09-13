package cn.li.mcbase.runtime;

import cn.li.mcmod.ModId;
import cn.li.mcver.RegistryLookups;
import cn.li.mcver.ResourceLocations;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Loader-agnostic damage source resolution using direct typed MC API (Loom-remappable).
 */
public final class DamageSourceAccess {
    public static final ResourceKey<DamageType> VEC_REFLECTION = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocations.of(ModId.ID, "vec_reflection"));

    private DamageSourceAccess() {
    }

    private static DamageSource vecReflection(Level level, Object attacker) {
        Holder<DamageType> type = RegistryLookups.holderOrThrow(level, VEC_REFLECTION);
        return attacker instanceof Entity entity
                ? new DamageSource(type, entity)
                : new DamageSource(type);
    }

    public static boolean isVecReflection(DamageSource source) {
        return source != null && source.is(VEC_REFLECTION);
    }

    public static DamageSource resolve(Level level, String kind) {
        if (level == null || kind == null) {
            return null;
        }
        return switch (kind) {
            case "magic" -> level.damageSources().magic();
            case "lightningBolt" -> level.damageSources().lightningBolt();
            case "explosion" -> level.damageSources().explosion(null, null);
            case "generic" -> level.damageSources().generic();
            default -> level.damageSources().generic();
        };
    }

    public static DamageSource resolveKeyword(Level level, Object sourceType) {
        if (level != null && ":vec-reflection".equals(String.valueOf(sourceType))) {
            return vecReflection(level, null);
        }
        String kind = switch (String.valueOf(sourceType)) {
            case ":magic" -> "magic";
            case ":lightning" -> "lightningBolt";
            case ":explosion" -> "explosion";
            case ":generic" -> "generic";
            default -> "generic";
        };
        return resolve(level, kind);
    }

    /**
     * Resolve a source that may need an attacking entity. AcademyCraft's
     * SkillDamageSource is an EntityDamageSource owned by the caster; on
     * modern Minecraft, playerAttack is the matching armor-respecting,
     * attacker-attributed vanilla source.
     */
    public static DamageSource resolveKeyword(Level level, Object sourceType, Object attacker) {
        if (level != null && ":vec-reflection".equals(String.valueOf(sourceType))) {
            return vecReflection(level, attacker);
        }
        if (level != null && ":skill".equals(String.valueOf(sourceType)) && attacker instanceof Player player) {
            return level.damageSources().playerAttack(player);
        }
        if (level != null && ":magic".equals(String.valueOf(sourceType)) && attacker instanceof Player player) {
            // Upstream TPSkillHelper.attackIgnoreArmor uses
            // SkillDamageSource(player).setDamageBypassesArmor(): an
            // armor-bypassing source attributed to the caster. MAGIC is in
            // the BYPASSES_ARMOR tag, so attributing it to the player keeps
            // the bypass while vanilla's LivingEntity.hurt applies the
            // player-attack knockback (0.4) it otherwise would not for an
            // unattributed magic source.
            return new DamageSource(RegistryLookups.holderOrThrow(level, DamageTypes.MAGIC), player);
        }
        return resolveKeyword(level, sourceType);
    }
}
