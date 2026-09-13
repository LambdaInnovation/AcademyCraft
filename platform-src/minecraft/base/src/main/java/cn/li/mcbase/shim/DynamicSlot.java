package cn.li.mcbase.shim;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Universal Slot subclass — replaces all 9 per-feature proxy sites.
 *  One class serves energy/plate/core/output/conditional slot variants. */
public class DynamicSlot extends Slot {
    private Predicate<ItemStack> mayPlaceFn = s -> true;
    private Supplier<Boolean> mayPickupFn = () -> true;
    private int stackLimit = 64;

    public DynamicSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    public DynamicSlot withMayPlace(Predicate<ItemStack> fn) { this.mayPlaceFn = fn; return this; }
    public DynamicSlot withMayPickup(Supplier<Boolean> fn) { this.mayPickupFn = fn; return this; }
    public DynamicSlot withMaxStackSize(int n) { this.stackLimit = n; return this; }

    @Override public boolean mayPlace(ItemStack stack) { return mayPlaceFn.test(stack); }
    @Override public boolean mayPickup(Player player) { return mayPickupFn.get(); }
    @Override public int getMaxStackSize() { return stackLimit; }
}
