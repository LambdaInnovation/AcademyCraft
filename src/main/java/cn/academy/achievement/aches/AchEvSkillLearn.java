package cn.academy.achievement.aches;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.event.SkillLearnEvent;
import cn.academy.achievement.DispatcherAch;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

/**
 * @author EAirPeter
 */
public final class AchEvSkillLearn<Cat extends Category> extends AchAbility<Cat> implements IAchEventDriven<SkillLearnEvent> {
    
    public AchEvSkillLearn(Skill skill, String id, int x, int y, Item display, Achievement parent) {
        super((Cat) skill.getCategory(), id, x, y, display, parent);
        cSkill = skill;
    }
    
    public AchEvSkillLearn(Skill skill, String id, int x, int y, Block display, Achievement parent) {
        super((Cat) skill.getCategory(), id, x, y, display, parent);
        cSkill = skill;
    }
    
    public AchEvSkillLearn(Skill skill, String id, int x, int y, ItemStack display, Achievement parent) {
        super((Cat) skill.getCategory(), id, x, y, display, parent);
        cSkill = skill;
    }
    
    public AchEvSkillLearn(Skill skill, int x, int y, Item display, Achievement parent) {
        super((Cat) skill.getCategory(), skill.getName(), x, y, display, parent);
        cSkill = skill;
    }
    
    public AchEvSkillLearn(Skill skill, int x, int y, Block display, Achievement parent) {
        super((Cat) skill.getCategory(), skill.getName(), x, y, display, parent);
        cSkill = skill;
    }
    
    public AchEvSkillLearn(Skill skill, int x, int y, ItemStack display, Achievement parent) {
        super((Cat) skill.getCategory(), skill.getName(), x, y, display, parent);
        cSkill = skill;
    }
    
    public AchEvSkillLearn(Skill skill, String id, int x, int y, Achievement parent) {
        super(skill, id, x, y, parent);
        cSkill = skill;
    }
    
    public AchEvSkillLearn(Skill skill, int x, int y, Achievement parent) {
        super(skill, x, y, parent);
        cSkill = skill;
    }
    
    private final Skill cSkill;
    
    @Override
    public void registerAll() {
        DispatcherAch.INSTANCE.rgSkillLearn(cSkill, this);
    }

    @Override
    public void unregisterAll() {
        DispatcherAch.INSTANCE.urSkillLearn(cSkill);
    }
    
    @Override
    public boolean accept(SkillLearnEvent event) {
        return event.skill == cSkill;
    }

}