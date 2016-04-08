/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.skills;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.client.ui.CPBar;
import cn.academy.ability.client.ui.CPBar.IConsumptionHintProvider;
import cn.academy.misc.achievements.ModuleAchievements;
import cn.academy.vanilla.teleporter.client.LocTeleportUI;
import cn.academy.vanilla.teleporter.data.LocTeleData.Location;
import cn.academy.vanilla.teleporter.util.TPSkillHelper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.s11n.network.NetworkS11n;
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.util.List;
import java.util.function.Predicate;

/**
 * Location teleport. This is the skill plus the synchronization&logic needed
 * for the UI.
 * 
 * @author WeAthFolD
 */
@Registrant
@NetworkS11nType
public class LocationTeleport extends Skill {

    private static final String MSG_PERFORM = "perform";

    public static final LocationTeleport instance = new LocationTeleport();
    static Predicate<Entity> basicSelector = EntitySelectors.living().and(entity -> entity.width * entity.width * entity.height < 80f);

    private LocationTeleport() {
        super("location_teleport", 3);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstanceInstant() {
            @Override
            @SideOnly(Side.CLIENT)
            public void execute() {
                Minecraft.getMinecraft().displayGuiScreen(new LocTeleportUI());

                CPBar.setHintProvider(new IConsumptionHintProvider() {

                    @Override
                    public boolean alive() {
                        return locate() != null;
                    }

                    @Override
                    public float getConsumption() {
                        LocTeleportUI ui = locate();
                        if (ui == null || ui.selection == null)
                            return 0;
                        return LocationTeleport.getConsumption(player, ui.selection);
                    }

                    private LocTeleportUI locate() {
                        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
                        return screen instanceof LocTeleportUI ? ((LocTeleportUI) screen) : null;
                    }

                });
            }
        };
    }

    /**
     * Determine whether player can record the current location.
     */
    public static boolean canRecord(EntityPlayer player) {
        AbilityData aData = AbilityData.get(player);
        return player.worldObj.provider.dimensionId == 0 || aData.getSkillExp(instance) >= 0.3f;
    }

    public static Location toLocation(EntityPlayer player, String name) {
        return new Location(name, player.worldObj.provider.dimensionId, (float) player.posX, (float) player.posY,
                (float) player.posZ);
    }

    public static float getConsumption(EntityPlayer player, Location dest) {
        AbilityData data = AbilityData.get(player);
        float distance = (float) player.getDistance(dest.x, dest.y, dest.z);
        float dimPenalty = player.worldObj.provider.dimensionId != dest.dimension ? 2 : 1;

        return MathUtils.lerpf(200, 150, data.getSkillExp(instance)) *
                dimPenalty *
                Math.max(8.0f, MathHelper.sqrt_float(Math.min(800.0f, distance)));
    }

    private static float getOverload(EntityPlayer player) {
        return 240;
    }

    public static boolean canPerform(EntityPlayer player, Location dest) {
        CPData cpData = CPData.get(player);
        return cpData.getCP() >= getConsumption(player, dest);
    }

    private static float getRange() {
        return 4;
    }

    public static void performAction(EntityPlayer player, Location dest) {
        NetworkMessage.sendToServer(
                NetworkMessage.staticCaller(LocationTeleport.class),
                MSG_PERFORM, player, dest);
    }

    @Listener(channel=MSG_PERFORM, side=Side.SERVER)
    private static void hPerform(EntityPlayer player, Location dest) {
        AbilityData aData = AbilityData.get(player);
        CPData cpData = CPData.get(player);

        if (cpData.perform(getOverload(player), getConsumption(player, dest))) {
            List<Entity> entitiesToTeleport = WorldUtils.getEntities(player, 5,
                    basicSelector.and(EntitySelectors.exclude(player)));
            entitiesToTeleport = entitiesToTeleport.subList(0, Math.min(4, entitiesToTeleport.size()));

            if (player.worldObj.provider.dimensionId != dest.dimension) {
                player.travelToDimension(dest.dimension);
                for (Entity e : entitiesToTeleport) {
                    e.travelToDimension(dest.dimension);
                }
            }

            for (Entity e : entitiesToTeleport) {
                double dx = e.posX - player.posX, dy = e.posY - player.posY, dz = e.posZ - player.posZ;
                e.setPosition(dest.x + dx, dest.y + dy, dest.z + dz);
            }
            player.setPositionAndUpdate(dest.x, dest.y, dest.z);

            double dist = player.getDistance(dest.x, dest.y, dest.z);

            float expincr = dist >= 200 ? 0.08f : 0.05f;

            aData.addSkillExp(instance, expincr);
            ModuleAchievements.trigger(player, "teleporter.ignore_barrier");
            TPSkillHelper.incrTPCount(player);
        }
    }

}
