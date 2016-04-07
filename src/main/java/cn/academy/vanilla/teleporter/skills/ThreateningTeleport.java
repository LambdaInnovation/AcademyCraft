/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.skills;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.teleporter.client.TPParticleFactory;
import cn.academy.vanilla.teleporter.entity.EntityMarker;
import cn.academy.vanilla.teleporter.util.TPSkillHelper;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.BlockSelectors;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import static cn.lambdalib.util.generic.MathUtils.*;

/**
 * @author WeAthFolD
 */
public class ThreateningTeleport extends Skill {

    static final Color COLOR_NORMAL = new Color().fromHexColor(0xbabababa),
            COLOR_THREATENING = new Color().fromHexColor(0xbab2232a);

    public static final ThreateningTeleport instance = new ThreateningTeleport();

    public ThreateningTeleport() {
        super("threatening_teleport", 1);
    }

    private static float getRange(float exp) {
        return lerpf(8, 15, exp);
    }

    private static float getExpIncr(boolean attacked) {
        return (attacked ? 1 : 0.2f) * .003f;
    }

    private static float getDamage(AbilityData data, ItemStack stack) {
        float dmg = lerpf(6, 9, data.getSkillExp(instance));
        if (stack.getItem() == ModuleVanilla.needle) {
            dmg *= 1.5f;
        }
        return dmg;
    }

    private static float getConsumption(float exp) {
        return lerpf(129, 149, exp);
    }

    private static float getOverload(float exp) {
        return lerpf(26, 11, exp);
    }

    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstance() {

            @Override
            public void onStart() {
                this.estimatedCP = ThreateningTeleport.
                        getConsumption(AbilityData.get(getPlayer()).getSkillExp(instance));
            }

        }.addChild(new ThreateningAction());
    }

    public static class ThreateningAction extends SkillSyncAction {

        float exp;

        boolean attacked;

        public ThreateningAction() {
            super(-1);
        }

        @Override
        public void onStart() {
            super.onStart();

            exp = aData.getSkillExp(instance);

            if (!isRemote && player.getCurrentEquippedItem() == null) {
                ActionManager.abortAction(this);
            }

            if (isRemote) {
                startEffects();
            }
        }

        @Override
        public void onTick() {
            if (isRemote) {
                updateEffects();
            }

            if (!isRemote && player.getCurrentEquippedItem() == null) {
                ActionManager.abortAction(this);
            }
        }

        @Override
        public void onEnd() {
            ItemStack curStack = player.getCurrentEquippedItem();
            if (curStack != null && cpData.perform(getOverload(exp), getConsumption(exp))) {
                attacked = true;
                TraceResult result = calcDropPos();

                if (!isRemote) {
                    double dropProb = 1.0;
                    boolean attacked = false;

                    if (result.target != null) {
                        attacked = true;
                        TPSkillHelper.attack(player, instance, result.target, getDamage(aData, curStack));
                        instance.triggerAchievement(player);
                        dropProb = 0.3;
                    }

                    if (!player.capabilities.isCreativeMode) {
                        if (--curStack.stackSize <= 0) {
                            player.setCurrentItemOrArmor(0, null);
                        }
                    }

                    if (RandUtils.ranged(0, 1) < dropProb) {
                        ItemStack drop = curStack.copy();
                        drop.stackSize = 1;
                        player.worldObj.spawnEntityInWorld(
                                new EntityItem(player.worldObj, result.x, result.y, result.z, drop));
                    }
                    aData.addSkillExp(instance, getExpIncr(attacked));
                }

                setCooldown(instance, (int) lerpf(18, 10, exp));
            }

            if (isRemote) {
                endEffects();
            }
        }

        @Override
        public void onAbort() {
            if (isRemote) {
                endEffects();
            }
        }

        TraceResult calcDropPos() {
            double range = getRange(exp);

            MovingObjectPosition pos = Raytrace.traceLiving(player, range, EntitySelectors.living(),
                    BlockSelectors.filEverything);
            if (pos == null)
                pos = Raytrace.traceLiving(player, range, EntitySelectors.nothing());

            TraceResult ret = new TraceResult();
            if (pos == null) {
                Motion3D mo = new Motion3D(player, true).move(range);
                ret.setPos(mo.px, mo.py, mo.pz);
            } else if (pos.typeOfHit == MovingObjectType.BLOCK) {
                ret.setPos(pos.hitVec.xCoord, pos.hitVec.yCoord, pos.hitVec.zCoord);
            } else {
                Entity ent = pos.entityHit;
                ret.setPos(ent.posX, ent.posY + ent.height, ent.posZ);
                ret.target = ent;
            }
            return ret;
        }

        // CLIENT
        @SideOnly(Side.CLIENT)
        EntityMarker marker;

        @SideOnly(Side.CLIENT)
        void startEffects() {
            player.worldObj.spawnEntityInWorld(marker = new EntityMarker(player.worldObj));
            marker.setPosition(player.posX, player.posY, player.posZ);
            marker.width = marker.height = 0.5f;
        }

        @SideOnly(Side.CLIENT)
        void updateEffects() {
            TraceResult res = calcDropPos();
            if (res.target != null)
                res.y -= res.target.height;
            marker.setPosition(res.x, res.y, res.z);
            marker.target = res.target;
            marker.color = marker.target != null ? COLOR_THREATENING : COLOR_NORMAL;
        }

        @SideOnly(Side.CLIENT)
        void endEffects() {
            marker.setDead();
            if (attacked) {
                ACSounds.playClient(player, "tp.tp", 0.5f);

                TraceResult dropPos = calcDropPos();
                double dx = dropPos.x + .5 - player.posX, dy = dropPos.y + .5 - (player.posY - 0.5),
                        dz = dropPos.z + .5 - player.posZ;
                double dist = MathUtils.length(dx, dy, dz);
                Motion3D mo = new Motion3D(player.posX, player.posY - 0.5, player.posZ, dx, dy, dz);
                mo.normalize();

                double move = 1;
                for (double x = move; x <= dist; x += (move = RandUtils.ranged(1, 2))) {
                    mo.move(move);
                    player.worldObj.spawnEntityInWorld(TPParticleFactory.instance.next(player.worldObj, mo.getPosVec(),
                            VecUtils.vec(RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .05),
                                    RandUtils.ranged(-.02, .02))));
                }
            }
        }

    }

    private static class TraceResult {
        double x, y, z;
        Entity target;

        public void setPos(double _x, double _y, double _z) {
            x = _x;
            y = _y;
            z = _z;
        }
    }

}
