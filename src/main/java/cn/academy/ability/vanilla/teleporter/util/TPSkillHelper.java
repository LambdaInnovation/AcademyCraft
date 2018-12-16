package cn.academy.ability.vanilla.teleporter.util;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.vanilla.teleporter.passiveskill.DimFoldingTheorem;
import cn.academy.ability.vanilla.teleporter.passiveskill.SpaceFluctuation;
import cn.academy.advancements.ACAdvancements;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.AbilityEvent;
import cn.academy.ability.vanilla.teleporter.CatTeleporter;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

/**
 * @author WeAthFolD
 */
@NetworkS11nType
public class TPSkillHelper {

    static final String TPC_ID = "ac_tpcount";

    public static void incrTPCount(EntityPlayer player) {
        int i = player.getEntityData().getInteger(TPC_ID) + 1;
        player.getEntityData().setInteger(TPC_ID, i);
    }

    /**
     * You should use this in SERVER only. the critical hit event will be post
     * at client if a critical hit happened.
     */
    public static void attack(AbilityContext ctx, Entity target, float damage) {
        AbilityData aData = ctx.aData;
        EntityPlayer player = ctx.player;

        // Calculate 3 levels of crit hit
        for (int i = 0; i < 3; ++i) {
            float prob = prob(aData, i);
            if (RandUtils.nextFloat() < prob) {
                damage *= rates[i];
                player.sendMessage(new TextComponentTranslation("ac.ability.teleporter.crithit", rates[i]));
                aData.addSkillExp(CatTeleporter.dimFolding, (i + 1) * 0.005f);
                aData.addSkillExp(CatTeleporter.spaceFluct, 0.0001f);

                fireCritAttack(player, target, i);
                NetworkMessage.sendTo(player, NetworkMessage.staticCaller(TPSkillHelper.class),
                        "fire", player, target, i);
                break;
            }
        }

        ctx.attack(target, damage);
    }

    public static void attackIgnoreArmor(AbilityContext ctx, Entity target, float damage) {
        AbilityData aData = ctx.aData;
        EntityPlayer player = ctx.player;

        // Calculate 3 levels of crit hit
        for (int i = 0; i < 3; ++i) {
            float prob = prob(aData, i);
            if (RandUtils.nextFloat() < prob) {
                damage *= rates[i];
                player.sendMessage(new TextComponentTranslation("ac.ability.teleporter.crithit", rates[i]));
                aData.addSkillExp(CatTeleporter.dimFolding, (i + 1) * 0.005f);
                aData.addSkillExp(CatTeleporter.spaceFluct, 0.0001f);

                fireCritAttack(player, target, i);
                NetworkMessage.sendTo(player, NetworkMessage.staticCaller(TPSkillHelper.class),
                        "fire", player, target, i);
                break;
            }
        }

        ctx.attackIgnoreArmor(target, damage);
    }


    private static float prob(AbilityData data, int level) {
        float dimFoldingExp = data.isSkillLearned(DimFoldingTheorem.instance) ? data.getSkillExp(DimFoldingTheorem.instance) : -1;
        float spaceFluctExp = data.isSkillLearned(SpaceFluctuation.instance) ? data.getSkillExp(SpaceFluctuation.instance) : -1;

        switch (level) {
        case 0:
            return tryLerp(0.1f, 0.2f, dimFoldingExp) + tryLerp(0.18f, 0.25f, spaceFluctExp);
        case 1:
            return tryLerp(0.10f, 0.15f, spaceFluctExp);
        case 2:
            return tryLerp(0.01f, 0.03f, spaceFluctExp);
        }
        throw new IllegalArgumentException();
    }

    private static float rates[] = { 1.3f, 1.6f, 2.6f };

    private static float tryLerp(float a, float b, float l) {
        if (l == -1) return 0;
        return a + l * (b - a);
    }

    @Listener(channel="fire", side= Side.CLIENT)
    private static void fireCritAttack(EntityPlayer player, Entity target, int level) {
        MinecraftForge.EVENT_BUS.post(new TPCritHitEvent(player, target, level));
    }

    /**
     * Fired both client and server when player emits an critical hit.
     */
    public static class TPCritHitEvent extends AbilityEvent {
        public final int level;
        public final Entity target;

        public TPCritHitEvent(EntityPlayer _player, Entity _target, int _level) {
            super(_player);
            level = _level;
            target = _target;
        }
    }

}