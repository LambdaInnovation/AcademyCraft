package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.block.AbilityInterf;
import cn.academy.block.TileAbilityInterferer;
import cn.academy.support.EnergyItemHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;


/**
 * Created by Paindar on 2017/3/31.
 */
public class ContainAbilityInterferer extends TechUIContainer<TileAbilityInterferer>
{
    class SlotAIItem extends Slot
    {
        private int slot;

        public SlotAIItem(IInventory inv, int _slot, int x, int y) {
            super(inv, _slot, x, y);
            slot = _slot;
        }

        @Override
        public boolean isItemValid(ItemStack stack)
        {
            return slot == 0 && stack.getItem() == ACItems.energy_unit;
        }

    }

    public ContainAbilityInterferer(TileAbilityInterferer _tile, EntityPlayer _player) {
        super(_player, _tile);

        initInventory();
    }

    private void initInventory() {
        this.addSlotToContainer(new SlotAIItem(tile, 0, 139, 25));
        InventoryPlayer inv = player.inventory;

        for(int i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(inv, i, 6 + i*18, 163));
        }

        SlotGroup gInv = gRange(0, 7), gMachine = gSlots(0);

        addTransferRule(gMachine, gInv);
        addTransferRule(gInv, EnergyItemHelper::isSupported, gSlots(AbilityInterf.SLOT_BATTERY()));
    }

}