package cn.li.mcbase.entity;

import cn.li.mcbase.entity.logic.MobLogicBundle;
import net.minecraft.world.entity.EntityType;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class ScriptedEntityLogicRegistry {
    private static final Map<EntityType<?>, MobLogicBundle> MOB_LOGIC =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private ScriptedEntityLogicRegistry() {
    }

    public static void installMobLogic(EntityType<?> type, MobLogicBundle bundle) {
        if (type == null || bundle == null) {
            return;
        }
        MOB_LOGIC.put(type, bundle);
    }

    public static MobLogicBundle getMobLogic(EntityType<?> type) {
        if (type == null) {
            return MobLogicBundle.EMPTY;
        }
        MobLogicBundle bundle = MOB_LOGIC.get(type);
        return bundle == null ? MobLogicBundle.EMPTY : bundle;
    }
}
