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
import cn.academy.vanilla.teleporter.client.TPParticleFactory;
import cn.academy.vanilla.teleporter.entity.EntityMarker;
import cn.academy.vanilla.teleporter.util.TPSkillHelper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;

import static cn.lambdalib.util.generic.MathUtils.*;

/**
 * @author WeAthFolD
 */
@Registrant
public class ShiftTeleport extends Skill {

    static final Color CRL_BLOCK_MARKER = new Color().setColor4i(139, 139, 139, 180),
            CRL_ENTITY_MARKER = new Color().setColor4i(235, 81, 81, 180);

    public static final ShiftTeleport instance = new ShiftTeleport();

    private ShiftTeleport() {
        super("shift_tp", 4);
    }

    static float getExpIncr(int attackEntities) {
        return (1 + attackEntities) * 0.002f;
    }

    private static float getDamage(float exp) {
        return lerpf(16, 27, exp);
    }

    private static float getRange(float exp) {
        return lerpf(25, 35, exp);
    }

    private static float getConsumption(float exp) {
        return lerpf(374, 500, exp);
    }

    private static float getOverload(float exp) {
        return lerpf(38, 27, exp);
    }

    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstance().addChild(new ShiftTPAction())
                .setEstmCP(getConsumption(AbilityData.get(player).getSkillExp(instance)));
    }

    public static class ShiftTPAction extends SkillSyncAction {

        float exp;

        public ShiftTPAction() {
            super(-1);
        }

        @Override
        public void onStart() {
            super.onStart();

            exp = aData.getSkillExp(instance);

            ItemStack stack = player.getCurrentEquippedItem();
            Block block;
            if (!(stack != null && stack.getItem() instanceof ItemBlock
                    && (Block.getBlockFromItem(stack.getItem())) != null))
                ActionManager.abortAction(this);

            if (isRemote)
                startEffects();
        }

        @Override
        public void onTick() {
            if (isRemote)
                updateEffects();
        }

        boolean attacked;

        @Override
        public void onEnd() {
            if (isRemote)
                return;

            Block block;
            ItemStack stack = player.getCurrentEquippedItem();
            attacked = stack != null && stack.getItem() instanceof ItemBlock
                    && (block = Block.getBlockFromItem(stack.getItem())) != null;
            if (!attacked)
                return;

            ItemBlock item = (ItemBlock) stack.getItem();
            MovingObjectPosition position = getTracePosition();

            if (item.field_150939_a.canPlaceBlockAt(player.worldObj, position.blockX, position.blockY, position.blockZ)
                    && cpData.perform(getOverload(exp), getConsumption(exp))) {

                item.placeBlockAt(stack, player, player.worldObj, position.blockX, position.blockY, position.blockZ,
                        position.sideHit, (float) position.hitVec.xCoord, (float) position.hitVec.yCoord,
                        (float) position.hitVec.zCoord, stack.getItemDamage());

                if (!player.capabilities.isCreativeMode) {
                    if (--stack.stackSize <= 0)
                        player.setCurrentItemOrArmor(0, null);
                }

                List<Entity> list = getTargetsInLine();
                for (Entity target : list) {
                    TPSkillHelper.attack(player, instance, target, getDamage(exp));
                }

                player.worldObj.playSoundAtEntity(player, "academy:tp.tp_shift", 0.5f, 1f);
                aData.addSkillExp(instance, getExpIncr(list.size()));

                if (!player.capabilities.isCreativeMode) {
                    if (stack.stackSize-- == 0) {
                        player.setCurrentItemOrArmor(0, null);
                    }
                }

                setCooldown(instance, (int) lerpf(20, 5, exp));
            }
        }

        @Override
        public void onFinalize() {
            if (isRemote)
                endEffects();
        }

        // TODO: Some boilerplate... Clean this up in case you aren't busy
        private int[] getTraceDest() {
            double range = getRange(exp);
            MovingObjectPosition result = Raytrace.traceLiving(player, range, EntitySelectors.nothing);
            if (result != null) {
                ForgeDirection dir = ForgeDirection.values()[result.sideHit];
                return new int[] { result.blockX + dir.offsetX, result.blockY + dir.offsetY,
                        result.blockZ + dir.offsetZ };
            }
            Motion3D mo = new Motion3D(player, true).move(range);
            return new int[] { (int) mo.px, (int) mo.py, (int) mo.pz };
        }

        private MovingObjectPosition getTracePosition() {
            double range = getRange(exp);
            MovingObjectPosition result = Raytrace.traceLiving(player, range, EntitySelectors.nothing);
            if (result != null) {
                ForgeDirection dir = ForgeDirection.values()[result.sideHit];
                result.blockX += dir.offsetX;
                result.blockY += dir.offsetY;
                result.blockZ += dir.offsetZ;
                return result;
            }
            Motion3D mo = new Motion3D(player, true).move(range);
            return new MovingObjectPosition((int) mo.px, (int) mo.py, (int) mo.pz, 0,
                    VecUtils.vec(mo.px, mo.py, mo.pz));
        }

        private List<Entity> getTargetsInLine() {
            int[] dest = getTraceDest();
            Vec3 v0 = VecUtils.vec(player.posX, player.posY, player.posZ),
                    v1 = VecUtils.vec(dest[0] + .5, dest[1] + .5, dest[2] + .5);

            AxisAlignedBB area = WorldUtils.minimumBounds(v0, v1);
            IEntitySelector selector = new IEntitySelector() {

                @Override
                public boolean isEntityApplicable(Entity entity) {
                    double hw = entity.width / 2;
                    return VecUtils.checkLineBox(VecUtils.vec(entity.posX - hw, entity.posY, entity.posZ - hw),
                            VecUtils.vec(entity.posX + hw, entity.posY + entity.height, entity.posZ + hw), v0,
                            v1) != null;
                }

            };
            return WorldUtils.getEntities(player.worldObj, area,
                    EntitySelectors.and(EntitySelectors.living, EntitySelectors.excludeOf(player), selector));
        }

        // CLIENT
        @SideOnly(Side.CLIENT)
        EntityMarker blockMarker;

        @SideOnly(Side.CLIENT)
        List<EntityMarker> targetMarkers;

        @SideOnly(Side.CLIENT)
        int effTicker;

        @SideOnly(Side.CLIENT)
        void startEffects() {
            targetMarkers = new ArrayList();
            if (isLocal()) {
                blockMarker = new EntityMarker(player.worldObj);
                blockMarker.ignoreDepth = true;
                blockMarker.width = blockMarker.height = 1.2f;
                blockMarker.color = CRL_BLOCK_MARKER;
                blockMarker.setPosition(player.posX, player.posY, player.posZ);

                player.worldObj.spawnEntityInWorld(blockMarker);
            }
        }

        @SideOnly(Side.CLIENT)
        void updateEffects() {
            if (isLocal()) {
                if (++effTicker == 3) {
                    effTicker = 0;
                    for (EntityMarker em : targetMarkers) {
                        em.setDead();
                    }
                    targetMarkers.clear();
                    List<Entity> targetsInLine = getTargetsInLine();
                    for (Entity e : targetsInLine) {
                        EntityMarker em = new EntityMarker(e);
                        em.color = CRL_ENTITY_MARKER;
                        em.ignoreDepth = true;
                        player.worldObj.spawnEntityInWorld(em);
                        targetMarkers.add(em);
                    }
                }

                int[] dest = getTraceDest();
                blockMarker.setPosition(dest[0] + 0.5, dest[1], dest[2] + 0.5);
            }
        }

        @SideOnly(Side.CLIENT)
        void endEffects() {
            if (isLocal()) {
                for (EntityMarker em : targetMarkers)
                    em.setDead();
                blockMarker.setDead();
            }

            if (attacked) {
                int[] dest = getTraceDest();
                double dx = dest[0] + .5 - player.posX, dy = dest[1] + .5 - (player.posY - 0.5),
                        dz = dest[2] + .5 - player.posZ;
                double dist = MathUtils.length(dx, dy, dz);
                Motion3D mo = new Motion3D(player.posX, player.posY - 0.5, player.posZ, dx, dy, dz);
                mo.normalize();

                double move = 1;
                for (double x = move; x <= dist; x += (move = RandUtils.ranged(0.6, 1))) {
                    mo.move(move);
                    player.worldObj.spawnEntityInWorld(TPParticleFactory.instance.next(player.worldObj, mo.getPosVec(),
                            VecUtils.vec(RandUtils.ranged(-.05, .05), RandUtils.ranged(-.02, .05),
                                    RandUtils.ranged(-.05, .05))));
                }
            }
        }

    }
}
