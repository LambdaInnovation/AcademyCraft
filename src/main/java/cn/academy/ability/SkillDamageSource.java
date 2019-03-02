package cn.academy.ability;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class SkillDamageSource extends EntityDamageSource {

    public final Skill skill;

    public SkillDamageSource(EntityPlayer player, Skill skill) {
        super("ac_skill", player);
        this.skill = skill;
    }

    // Chat display
    @Override
    public ITextComponent getDeathMessage(EntityLivingBase target) {
        return new TextComponentTranslation("death.attack.ac_skill",
                target.getName(),
                this.damageSourceEntity.getName(),
                skill.getDisplayName());
    }

}