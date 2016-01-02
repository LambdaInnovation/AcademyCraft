package cn.academy.misc.achievements.aches;

import cn.academy.crafting.api.event.MatterUnitHarvestEvent;
import cn.academy.misc.achievements.DispatcherAch;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;

/**
 * @author EAirPeter
 */
public final class AchEvMatterUnitHarvest extends ACAchievement implements IAchEventDriven<MatterUnitHarvestEvent> {

    private final Block block;
    
    public AchEvMatterUnitHarvest(String id, int x, int y, Item display, Achievement parent, Block blo) {
        super(id, x, y, display, parent);
        block = blo;
    }
    
    public AchEvMatterUnitHarvest(String id, int x, int y, Block display, Achievement parent, Block blo) {
        super(id, x, y, display, parent);
        block = blo;
    }
    
    public AchEvMatterUnitHarvest(String id, int x, int y, ItemStack display, Achievement parent, Block blo) {
        super(id, x, y, display, parent);
        block = blo;
    }
    
    @Override
    public void registerAll() {
        DispatcherAch.INSTANCE.rgMatterUnitHarvest(block, this);
    }

    @Override
    public void unregisterAll() {
        DispatcherAch.INSTANCE.urMatterUnitHarvest(block);
    }
    
    @Override
    public boolean accept(MatterUnitHarvestEvent event) {
        return event.mat.block == block;
    }

}
