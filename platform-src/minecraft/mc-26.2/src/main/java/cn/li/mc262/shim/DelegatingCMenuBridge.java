package cn.li.mc262.shim;

import cn.li.mcbase.shim.DelegatingCMenuBridgeBase;
import clojure.lang.IFn;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;

/** Versioned clicked/callSuperClicked for 26.2 ContainerInput. */
public class DelegatingCMenuBridge extends DelegatingCMenuBridgeBase {

    public DelegatingCMenuBridge(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput clickType, Player player) {
        IFn fn = clickedFn();
        if (fn != null) {
            fn.invoke(this, slotIndex, button, clickType, player);
        } else {
            super.clicked(slotIndex, button, clickType, player);
        }
    }

    @Override
    public void callSuperClicked(int slotIndex, int button, Object clickType, Player player) {
        super.clicked(slotIndex, button, (ContainerInput) clickType, player);
    }
}
