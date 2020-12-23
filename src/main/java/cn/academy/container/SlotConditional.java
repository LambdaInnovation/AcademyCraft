package cn.academy.container;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import scala.Function1;

import java.util.function.Predicate;

public class SlotConditional extends Slot {

    public static SlotConditional apply(Predicate<ItemStack> pred, IInventory inv, int slot, int x, int y) {
        return new SlotConditional(pred, inv, slot, x, y);
    }

    public static SlotConditional apply(Function1<ItemStack, Boolean> pred, IInventory inv, int slot, int x, int y) {
        return new SlotConditional(pred::apply, inv, slot, x, y);
    }

    private final Predicate<ItemStack> pred;

    public SlotConditional(Predicate<ItemStack> pred, IInventory inv, int slot, int x, int y) {
        super(inv, slot, x, y);
        this.pred = pred;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return pred.test(stack);
    }
}
