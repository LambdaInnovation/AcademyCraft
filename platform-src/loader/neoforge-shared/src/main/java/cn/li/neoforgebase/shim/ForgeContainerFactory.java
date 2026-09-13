package cn.li.neoforgebase.shim;

import clojure.lang.IFn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.IContainerFactory;

/**
 * Java skeleton for {@link IContainerFactory} — delegates menu creation to a Clojure IFn.
 */
public class ForgeContainerFactory implements IContainerFactory<AbstractContainerMenu> {
    private final IFn createFn;

    public ForgeContainerFactory(IFn createFn) {
        this.createFn = createFn;
    }

    @Override
    public AbstractContainerMenu create(int windowId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return (AbstractContainerMenu) createFn.invoke(windowId, playerInventory, buf);
    }
}
