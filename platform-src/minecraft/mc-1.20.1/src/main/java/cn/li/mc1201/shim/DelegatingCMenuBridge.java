package cn.li.mc1201.shim;

import cn.li.mcbase.shim.DelegatingCMenuBridgeBase;
import clojure.lang.IFn;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;

/** Versioned clicked/callSuperClicked for 1.20.1 ClickType. */
public class DelegatingCMenuBridge extends DelegatingCMenuBridgeBase {

    public DelegatingCMenuBridge(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        IFn fn = clickedFn();
        if (fn != null) {
            fn.invoke(this, slotIndex, button, clickType, player);
        } else {
            super.clicked(slotIndex, button, clickType, player);
        }
    }

    @Override
    public void callSuperClicked(int slotIndex, int button, Object clickType, Player player) {
        super.clicked(slotIndex, button, (ClickType) clickType, player);
    }
}
