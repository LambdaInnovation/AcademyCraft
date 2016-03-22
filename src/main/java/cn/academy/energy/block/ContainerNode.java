/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.energy.api.IFItemManager;
import cn.lambdalib.template.container.CleanContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeathFolD
 */
public class ContainerNode extends CleanContainer {

    public final TileNode node;

    public ContainerNode(TileNode _node, EntityPlayer player) {
        node = _node;
        initInventory(player.inventory);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
    }

    private void initInventory(InventoryPlayer inv) {
        this.addSlotToContainer(new SlotIFItem(node, 0, 26, 2));
        this.addSlotToContainer(new SlotIFItem(node, 1, 26, 71));

        int STEP = 18;

        for(int i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(inv, i, -10 + i * STEP, 153));
        }

        for(int i = 1; i < 4; ++i) {
            for(int j = 0; j < 9; ++j) {
                int slot = (4 - i) * 9 + j;
                addSlotToContainer(new Slot(inv, slot, -10 + j * STEP, 149 - i * STEP));
            }
        }

        SlotGroup invGroup = gRange(2, 2 + 36);
        SlotGroup batteryGroup = gSlots(0, 1);

        addTransferRule(invGroup, stack -> IFItemManager.instance.isSupported(stack), batteryGroup);
        addTransferRule(batteryGroup, invGroup);
    }

    @Override
    public boolean canInteractWith(EntityPlayer var1) {
        return var1.getDistanceSq(node.xCoord + .5, node.yCoord + .5, node.zCoord + .5) < 64.0;
    }

}
