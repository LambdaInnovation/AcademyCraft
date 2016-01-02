package cn.academy.misc.achievements.aches;

import java.util.HashMap;
import java.util.HashSet;

import cn.academy.misc.achievements.DispatcherAch;
import cn.academy.misc.achievements.conds.CondItemCrafted;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

/**
 * @author EAirPeter
 */
public final class AchCrAnd extends AchEvItemCrafted {

    public AchCrAnd(String id, int x, int y, Item display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    public AchCrAnd(String id, int x, int y, Block display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    public AchCrAnd(String id, int x, int y, ItemStack display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    private HashMap<Item, CondItemCrafted> cIT = new HashMap<Item, CondItemCrafted>();
    
    @Override
    public void registerAll() {
        for (CondItemCrafted cit : cIT.values())
            DispatcherAch.INSTANCE.rgItemCrafted(cit.item, this);
    }
    
    @Override
    public void unregisterAll() {
        for (CondItemCrafted cit : cIT.values())
            DispatcherAch.INSTANCE.urItemCrafted(cit.item, this);
    }
    
    @Override
    public ACAchievement adItemCrafted(CondItemCrafted cit) {
        cIT.put(cit.item, cit);
        return this;
    }

    @Override
    public boolean accept(ItemCraftedEvent event) {
        CondItemCrafted cit = cIT.get(event.crafting.getItem());
        if (cit == null || !cit.acItemStack(event.crafting))
            return false;
        int ac = 1;
        HashSet<CondItemCrafted> set = new HashSet<CondItemCrafted>();
        set.add(cit);
        for (ItemStack is : event.player.inventory.mainInventory) {
            CondItemCrafted cur = cIT.get(is.getItem());
            if (cur != null && cur.acItemStack(is))
                set.add(cur);
        }
        return set.size() == cIT.size();
    }

}
