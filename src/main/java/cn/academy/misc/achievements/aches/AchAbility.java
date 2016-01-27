package cn.academy.misc.achievements.aches;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.misc.achievements.ItemAchievement;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

/**
 * @author EAirPeter
 */
public class AchAbility<Cat extends Category> extends ACAchievement {

    //Ach
    //AchEv
    
    protected final Cat category;
    
    public AchAbility(Cat cat, String id, int x, int y, Item display, Achievement parent) {
        super(cat.getName() + "." + id, x, y, display, parent);
        category = cat;
    }
    
    public AchAbility(Cat cat, String id, int x, int y, Block display, Achievement parent) {
        super(cat.getName() + "." + id, x, y, display, parent);
        category = cat;
    }
    
    public AchAbility(Cat cat, String id, int x, int y, ItemStack display, Achievement parent) {
        super(cat.getName() + "." + id, x, y, display, parent);
        category = cat;
    }
    
    public AchAbility(Cat cat, String id, int x, int y, Achievement parent) {
        super(cat.getName() + "." + id, x, y, ItemAchievement.getStack(cat.getIcon()), parent);
        category = cat;
    }
    
    public AchAbility(Skill skill, String id, int x, int y, Achievement parent) {
        this((Cat) skill.getCategory(), id, x, y, ItemAchievement.getStack(skill.getHintIcon()), parent);
    }
    
    public AchAbility(Skill skill, int x, int y, Achievement parent) {
        this(skill, skill.getName(), x, y, parent);
    }
    
    @Override
    public void registerAll() {
    }
    
    @Override
    public void unregisterAll() {
    }
    
}
