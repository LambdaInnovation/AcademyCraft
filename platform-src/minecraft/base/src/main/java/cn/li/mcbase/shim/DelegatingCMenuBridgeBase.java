package cn.li.mcbase.shim;

import cn.li.mcbase.gui.CMenuBridge;
import clojure.lang.IFn;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/**
 * Shared DelegatingCMenuBridge skeleton. Versioned subclasses only fork
 * {@code clicked}/{@code callSuperClicked} (ClickType vs ContainerInput).
 */
public abstract class DelegatingCMenuBridgeBase extends CMenuBridge {

    private IFn stillValidFn;
    private IFn removedFn;
    private IFn broadcastChangesFn;
    private IFn clickedFn;
    private IFn quickMoveStackFn;

    protected DelegatingCMenuBridgeBase(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    protected final IFn clickedFn() {
        return clickedFn;
    }

    public DelegatingCMenuBridgeBase withStillValid(IFn fn) {
        this.stillValidFn = fn;
        return this;
    }

    public DelegatingCMenuBridgeBase withRemoved(IFn fn) {
        this.removedFn = fn;
        return this;
    }

    public DelegatingCMenuBridgeBase withBroadcastChanges(IFn fn) {
        this.broadcastChangesFn = fn;
        return this;
    }

    public DelegatingCMenuBridgeBase withClicked(IFn fn) {
        this.clickedFn = fn;
        return this;
    }

    public DelegatingCMenuBridgeBase withQuickMoveStack(IFn fn) {
        this.quickMoveStackFn = fn;
        return this;
    }

    /**
     * Versioned subclasses cast {@code clickType} to ClickType or ContainerInput.
     */
    public abstract void callSuperClicked(int slotIndex, int button, Object clickType, Player player);

    @Override
    public boolean stillValid(Player player) {
        if (stillValidFn != null) {
            Object r = stillValidFn.invoke(this, player);
            return r instanceof Boolean ? (Boolean) r : true;
        }
        return true;
    }

    @Override
    public void removed(Player player) {
        if (removedFn != null) {
            removedFn.invoke(this, player);
        } else {
            super.removed(player);
        }
    }

    @Override
    public void broadcastChanges() {
        if (broadcastChangesFn != null) {
            broadcastChangesFn.invoke(this);
        } else {
            super.broadcastChanges();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (quickMoveStackFn != null) {
            return (ItemStack) quickMoveStackFn.invoke(this, player, slotIndex);
        }
        return ItemStack.EMPTY;
    }
}
