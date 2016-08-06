package cn.academy.ability;

import cn.academy.ability.api.Skill;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.IChatComponent;

public class SkillDamageSource extends EntityDamageSource {

    public final Skill skill;

    public SkillDamageSource(EntityPlayer player, Skill skill) {
        super("ac_skill", player);
        this.skill = skill;
    }

    // Chat display
    @Override
    public IChatComponent func_151519_b(EntityLivingBase target) {
        return new ChatComponentTranslation("death.attack.ac_skill",
                target.getCommandSenderName(),
                this.damageSourceEntity.getCommandSenderName(),
                skill.getDisplayName());
    }

}
