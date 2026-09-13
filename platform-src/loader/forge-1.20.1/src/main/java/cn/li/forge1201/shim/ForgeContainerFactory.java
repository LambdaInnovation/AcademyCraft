package cn.li.forge1201.shim;

import clojure.lang.IFn;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.IContainerFactory;

/**
 * Java skeleton for {@link IContainerFactory} — delegates menu creation to a Clojure IFn.
 */
public class ForgeContainerFactory implements IContainerFactory<AbstractContainerMenu> {
    private final IFn createFn;

    public ForgeContainerFactory(IFn createFn) {
        this.createFn = createFn;
    }

    @Override
    public AbstractContainerMenu create(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        return (AbstractContainerMenu) createFn.invoke(windowId, playerInventory, buf);
    }
}
