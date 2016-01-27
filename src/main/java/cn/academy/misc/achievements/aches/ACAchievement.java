package cn.academy.misc.achievements.aches;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

import java.util.HashMap;

/**
 * @author EAirPeter
 */
public abstract class ACAchievement extends Achievement {
    
    //Ach
    //AchEv
    
    protected ACAchievement(String id, int x, int y, Item display, Achievement parent) {
        this(id, x, y, new ItemStack(display), parent);
    }
    
    protected ACAchievement(String id, int x, int y, Block display, Achievement parent) {
        this(id, x, y, new ItemStack(display), parent);
    }
    
    protected ACAchievement(String id, int x, int y, ItemStack display, Achievement parent) {
        super("achievement.ac_" + id, "ac_" + id, x, y, display, parent);
        if (parent == null)
            initIndependentStat();
        registerStat();
        map.put(id, this);
    }
    
    public abstract void registerAll();
    public abstract void unregisterAll();
    
    private static HashMap<String, ACAchievement> map = new HashMap<String, ACAchievement>();
    
    /**
     * Get the achievement according to the id
     * @param id The id
     * @return The achievement according to the id
     */
    public static ACAchievement getById(String id) {
        return map.get(id);
    }
    
}
