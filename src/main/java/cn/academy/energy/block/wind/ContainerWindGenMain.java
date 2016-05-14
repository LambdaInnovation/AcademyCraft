/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block.wind;

import cn.academy.core.container.TechUIContainer;
import cn.academy.energy.ModuleEnergy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import static sun.audio.AudioPlayer.player;

/**
 * @author WeAthFolD
 */
public class ContainerWindGenMain extends TechUIContainer<TileWindGenMain> {
    
    public ContainerWindGenMain(EntityPlayer _player, TileWindGenMain _tile) {
        super(_player, _tile);

        this.addSlotToContainer(new SlotFan(tile, 0, 78, 9));

        mapPlayerInventory();

        SlotGroup gInv = gRange(1, 1+36), gFan = gSlots(0);

        addTransferRule(gFan, gInv);
        addTransferRule(gInv, stack -> stack.getItem() == ModuleEnergy.windgenFan, gFan);
    }

}
