package cn.academy.achievement.aches;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.event.LevelChangeEvent;
import cn.academy.achievement.DispatcherAch;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

/**
 * @author EAirPeter
 */
public final class AchEvLevelChange<Cat extends Category> extends AchAbility<Cat> implements IAchEventDriven<LevelChangeEvent> {

    private final int level;
    
    public AchEvLevelChange(int lv, Cat cat, String id, int x, int y, Item display, Achievement parent) {
        super(cat, id, x, y, display, parent);
        level = lv;
    }
    
    public AchEvLevelChange(int lv, Cat cat, String id, int x, int y, Block display, Achievement parent) {
        super(cat, id, x, y, display, parent);
        level = lv;
    }
    
    public AchEvLevelChange(int lv, Cat cat, String id, int x, int y, ItemStack display, Achievement parent) {
        super(cat, id, x, y, display, parent);
        level = lv;
    }
    
    public AchEvLevelChange(int lv, Skill skill, String id, int x, int y, Achievement parent) {
        super(skill, id, x, y, parent);
        level = lv;
    }
    
    @Override
    public void registerAll() {
        DispatcherAch.INSTANCE.rgLevelChange(category, level, this);
    }
    
    @Override
    public void unregisterAll() {
        DispatcherAch.INSTANCE.urLevelChange(category, level);
    }
    
    @Override
    public boolean accept(LevelChangeEvent event) {
        return event.getAbilityData().getLevel() == level;
    }

}