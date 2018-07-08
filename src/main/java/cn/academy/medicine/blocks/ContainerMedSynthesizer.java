package cn.academy.medicine.blocks;

import cn.academy.core.container.SlotConditional;
import cn.academy.core.container.SlotOutput;
import cn.academy.core.container.TechUIContainer;
import cn.academy.medicine.MatExtraction;
import cn.academy.medicine.ModuleMedicine;
import cn.academy.medicine.items.ItemPowder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.function.Function;
import java.util.function.Predicate;

import static cn.academy.medicine.blocks.TileMedSynthesizer.bottleSlot;
import static cn.academy.medicine.blocks.TileMedSynthesizer.outputSlot;

public class ContainerMedSynthesizer extends TechUIContainer<TileMedSynthesizer> {

    public ContainerMedSynthesizer(EntityPlayer _player, TileMedSynthesizer _tile) {
        super(_player, _tile);

        addSlotToContainer(slotPowder(0, 34, 12));
        addSlotToContainer(slotPowder(1, 10, 32));
        addSlotToContainer(slotPowder(2, 10, 58));
        addSlotToContainer(slotPowder(3, 35, 78));

        addSlotToContainer(SlotConditional.apply((ItemStack s)->s.getItem() == ModuleMedicine.emptyBottle, tile, bottleSlot, 50, 45));
        addSlotToContainer(new SlotOutput(tile, outputSlot, 137, 44));

        SlotGroup gInv = gRange(6, 6 + 36);
        SlotGroup gPowders = gRange(0, 4);
        SlotGroup gBottle = gSlots(4);
        SlotGroup gOutput = gSlots(5);

        addTransferRule(gInv, p(stack -> stack.getItem() instanceof ItemPowder), gPowders);
        addTransferRule(gInv, p(stack -> stack.getItem() == ModuleMedicine.emptyBottle), gBottle);
        addTransferRule(gRange(0, 6), gInv);

        mapPlayerInventory();
    }

    public SlotConditional slotPowder(int id, int x, int y){
        return SlotConditional.apply((ItemStack stack) -> stack.getItem() instanceof ItemPowder, tile, id, x, y);
    }

    private Predicate<ItemStack> p(Function<ItemStack, Boolean> fn){
        return fn::apply;
    }

}
