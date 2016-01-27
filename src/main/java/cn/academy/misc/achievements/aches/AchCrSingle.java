/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.achievements.aches;

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
public final class AchCrSingle extends AchEvItemCrafted {

    public AchCrSingle(String id, int x, int y, Item display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    public AchCrSingle(String id, int x, int y, Block display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    public AchCrSingle(String id, int x, int y, ItemStack display, Achievement parent) {
        super(id, x, y, display, parent);
    }
    
    private CondItemCrafted cIT = null;
    
    @Override
    public void registerAll() {
        if (cIT != null)
            DispatcherAch.INSTANCE.rgItemCrafted(cIT.item, this);
    }
    
    @Override
    public void unregisterAll() {
        if (cIT != null)
            DispatcherAch.INSTANCE.urItemCrafted(cIT.item, this);
    }
    
    @Override
    public AchCrSingle adItemCrafted(CondItemCrafted cit) {
        cIT = cit;
        return this;
    }

    @Override
    public boolean accept(ItemCraftedEvent event) {
        return cIT.acItemStack(event.crafting);
    }

}
