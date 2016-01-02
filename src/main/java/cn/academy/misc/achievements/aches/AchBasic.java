package cn.academy.misc.achievements.aches;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

/**
 * @author EAirPeter
 */
public final class AchBasic extends ACAchievement {

    public AchBasic(String id, int x, int y, Item display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    public AchBasic(String id, int x, int y, Block display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    public AchBasic(String id, int x, int y, ItemStack display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    @Override
    public void registerAll() {
    }
    
    @Override
    public void unregisterAll() {
    }

}
