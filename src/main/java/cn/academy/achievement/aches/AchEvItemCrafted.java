package cn.academy.achievement.aches;

import cn.academy.achievement.conds.CondItemCrafted;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

/**
 * @author EAirPeter
 */
public abstract class AchEvItemCrafted extends ACAchievement implements IAchEventDriven<ItemCraftedEvent> {

    //AchCr
    
    protected AchEvItemCrafted(String id, int x, int y, Item display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    protected AchEvItemCrafted(String id, int x, int y, Block display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    protected AchEvItemCrafted(String id, int x, int y, ItemStack display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    public final ACAchievement adItemCrafted(Block block) {
        return adItemCrafted(new CondItemCrafted(block));
    }
    public final ACAchievement adItemCrafted(Item item) {
        return adItemCrafted(new CondItemCrafted(item));
    }
    public final ACAchievement adItemCrafted(Block block, int meta) {
        return adItemCrafted(new CondItemCrafted(block, meta));
    }
    public final ACAchievement adItemCrafted(Item item, int meta) {
        return adItemCrafted(new CondItemCrafted(item, meta));
    }
    public final ACAchievement adItemCrafted(Block block, int meta, int amount) {
        return adItemCrafted(new CondItemCrafted(block, meta, amount));
    }
    public final ACAchievement adItemCrafted(Item item, int meta, int amount) {
        return adItemCrafted(new CondItemCrafted(item, meta, amount));
    }
    public abstract ACAchievement adItemCrafted(CondItemCrafted cit);
    
}