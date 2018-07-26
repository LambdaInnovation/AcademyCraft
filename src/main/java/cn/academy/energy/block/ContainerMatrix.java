package cn.academy.energy.block;

import cn.academy.core.container.TechUIContainer;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.lambdalib2.template.container.CleanContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * @author WeAthFolD
 */
public class ContainerMatrix extends TechUIContainer<TileMatrix> {
    
    public ContainerMatrix(TileMatrix _tile, EntityPlayer _player) {
        super(_player, _tile);

        initInventory();
    }
    
    private void initInventory() {
        this.addSlotToContainer(new SlotPlate(tile, 0, 78, 11));
        this.addSlotToContainer(new SlotPlate(tile, 1, 53, 60));
        this.addSlotToContainer(new SlotPlate(tile, 2, 104, 60));
        
        this.addSlotToContainer(new SlotCore(tile, 3, 78, 36));

        mapPlayerInventory();

        SlotGroup invGroup = gRange(4, 4 + 36);

        addTransferRule(invGroup, stack -> stack.getItem() == ModuleCrafting.constPlate, gSlots(0, 1, 2));
        addTransferRule(invGroup, stack -> stack.getItem() == ModuleEnergy.matrixCore, gSlots(3));
        addTransferRule(gRange(0, 4), invGroup);
    }
    
    public static class SlotCore extends Slot {

        public SlotCore(IInventory inv, int slot, int x, int y) {
            super(inv, slot, x, y);
        }
        
        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack != null && stack.getItem() == ModuleEnergy.matrixCore;
        }
        
    }
    
    public static class SlotPlate extends Slot {

        public SlotPlate(IInventory inv, int slot, int x, int y) {
            super(inv, slot, x, y);
        }
        
        @Override
        public boolean isItemValid(ItemStack stack) {
            return stack != null && stack.getItem() == ModuleCrafting.constPlate;
        }
        
    }

}