package cn.academy.ability.api;

import cn.academy.ability.SkillDamageSource;
import cn.academy.ability.api.cooldown.CooldownData;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.event.CalcEvent;
import cn.academy.ability.api.event.ReflectEvent;
import cn.academy.core.config.ACConfig;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.mc.WorldUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.function.Predicate;

/**
 * A context for skill usage to abstract over common operations.
 */
public class AbilityContext {

    public static AbilityContext of(EntityPlayer player, Skill skill) {
        return new AbilityContext(player, skill);
    }

    public final EntityPlayer player;
    public final Skill skill;

    public final AbilityData aData;
    public final CPData cpData;
    public final CooldownData cdData;

    private AbilityContext(EntityPlayer p, Skill s) {
        player = p;
        skill = s;

        aData = AbilityData.get(player);
        cpData = CPData.get(player);
        cdData = CooldownData.of(player);
    }


    /**
     * Make the player fire the attack with the given skill. The damage will be re-calculated according to the damage
     *  scale specified globally and skill-locally, and influenced by passive skills.
     * @param target The entity to attack
     * @param damage The amount of damage applied (raw)
     */
    public void attack(Entity target, float damage) {
        damage = CalcEvent.calc(new CalcEvent.SkillAttack(player, skill, target, damage));

        if (damage > 0 && (AbilityPipeline.canAttackPlayer() || (!(target instanceof EntityPlayer))) && canAttack(target)) {
            target.attackEntityFrom(new SkillDamageSource(player, skill), getFinalDamage(damage));
        }
    }

    public void attackReflect(Entity target, float damage, Runnable reflectCallback) {
        if (MinecraftForge.EVENT_BUS.post(new ReflectEvent(player, skill, target))) {
            reflectCallback.run();
        } else {
            attack(target, damage);
        }
    }

    public boolean canAttack(Entity entity) {
        return canBreakBlock(entity.worldObj) || (!(entity instanceof EntityPainting) && !(entity instanceof EntityItemFrame));
    }

    public void attackRange(double x, double y, double z, double range,
                            float damage, Predicate<Entity> entitySelector) {
        List<Entity> list = WorldUtils.getEntities(player.worldObj, x, y, z, range, entitySelector);
        for(Entity ent : list) {
            double dist = MathUtils.distance(x, y, z, ent.posX, ent.posY, ent.posZ);
            float factor = 1 - MathUtils.clampf(0, 1, (float) (dist / range));
            float appliedDamage = MathUtils.lerpf(0, damage, factor);
            attack(ent, appliedDamage);
        }
    }

    public boolean canConsumeCP(float cp) {
        return cpData.canPerform(cp);
    }

    public boolean consume(float overload, float cp) {
        return cpData.perform(
                getFinalConsO(overload),
                getFinalConsCP(cp));
    }

    public void consumeWithForce(float overload, float cp) {
        cpData.performWithForce(overload, cp);
    }

    public float getSkillExp() {
        return aData.getSkillExp(skill);
    }

    public void addSkillExp(float amt) {
        aData.addSkillExp(skill, getFinalExpIncr(amt));
    }

    public void setCooldown(int ticks) {
        cdData.set(skill, ticks);
    }

    public void setCooldownSub(int subID, int ticks) {
        cdData.setSub(skill, subID, ticks);
    }

    private float g_getDamageScale() {
        return (float) ACConfig.instance().getDouble(
                "ac.ability.calc_global.damage_scale"
        );
    }

    public boolean canBreakBlock(World world, int x, int y, int z) {
        return skill.shouldDestroyBlocks() && AbilityPipeline.canBreakBlock(world, player, x, y, z);
    }

    public boolean canBreakBlock(World world) {
        return skill.shouldDestroyBlocks() && AbilityPipeline.canBreakBlock(world);
    }

    public boolean isEntirelyDisableBreakBlock() {
        return AbilityPipeline.isAllWorldDisableBreakBlock();
    }

    private float getFinalDamage(float damage) {
        return g_getDamageScale() * skill.getDamageScale() * damage;
    }

    private float getFinalExpIncr(float expincr) {
        return skill.getExpIncrSpeed() * expincr;
    }

    private float getFinalConsCP(float cp) {
        return skill.getCPConsumeSpeed() * cp;
    }

    private float getFinalConsO(float overload) {
        return skill.getOverloadConsumeSpeed() * overload;
    }

}
