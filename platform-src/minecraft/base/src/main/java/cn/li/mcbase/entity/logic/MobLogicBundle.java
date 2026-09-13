package cn.li.mcbase.entity.logic;

import javax.annotation.Nullable;

public final class MobLogicBundle {
    public static final MobLogicBundle EMPTY = new MobLogicBundle(null, null, null, null);

    public final @Nullable IMobTickLogic tick;
    public final @Nullable IMobHurtLogic hurt;
    public final @Nullable IMobDeathLogic death;
    public final @Nullable IMobLootLogic loot;

    public MobLogicBundle(@Nullable IMobTickLogic tick,
                          @Nullable IMobHurtLogic hurt,
                          @Nullable IMobDeathLogic death,
                          @Nullable IMobLootLogic loot) {
        this.tick = tick;
        this.hurt = hurt;
        this.death = death;
        this.loot = loot;
    }
}
