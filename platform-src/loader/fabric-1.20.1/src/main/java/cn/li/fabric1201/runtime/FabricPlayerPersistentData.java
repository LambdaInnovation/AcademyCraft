package cn.li.fabric1201.runtime;

import cn.li.fabric1201.access.PlayerPersistentDataAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class FabricPlayerPersistentData {
    private FabricPlayerPersistentData() {
    }

    public static CompoundTag get(Player player) {
        return ((PlayerPersistentDataAccess) player).academy$getPersistentData();
    }
}
