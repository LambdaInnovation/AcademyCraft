package cn.academy.misc.achievements.aches;

import cn.academy.misc.achievements.DispatcherAch;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

public class AchEvItemPickup extends ACAchievement implements IAchEventDriven<PlayerEvent.ItemPickupEvent> {
    
    ItemStack stack;

    public AchEvItemPickup(String id, int x, int y, Item display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    public AchEvItemPickup(String id, int x, int y, Block display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    public AchEvItemPickup(String id, int x, int y, ItemStack display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    @Override
    public void registerAll() {
        if(stack != null)
            DispatcherAch.INSTANCE.rgPlayerPickup(stack, this);
    }

    @Override
    public void unregisterAll() {}
    
    public AchEvItemPickup setTrigger(ItemStack s) {
        stack = s;
        return this;
    }

    @Override
    public boolean accept(ItemPickupEvent event) {
        if(stack == null)
            return false;
        Item item = stack.getItem();
        int dmg = stack.getItemDamage();
        ItemStack cmp = event.pickedUp.getEntityItem();
        return cmp.getItem() == item && dmg == cmp.getItemDamage();
    }

}