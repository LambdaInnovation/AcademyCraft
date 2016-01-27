/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.entity;

import cn.academy.ability.api.data.AbilityData;
import cn.academy.vanilla.teleporter.client.MarkRender;
import cn.academy.vanilla.teleporter.client.TPParticleFactory;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.util.entityx.EntityAdvanced;
import cn.lambdalib.util.generic.RandUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Spawn a position mark indicating where the player would be teleport to. You
 * should spawn it in CLIENT ONLY.
 * 
 * @author WeathFolD
 */
@Registrant
@RegEntity(clientOnly = true)
@SideOnly(Side.CLIENT)
@RegEntity.HasRender
public class EntityTPMarking extends EntityAdvanced {

    @RegEntity.Render
    @SideOnly(Side.CLIENT)
    public static MarkRender render;

    static TPParticleFactory particleFac = TPParticleFactory.instance;

    final AbilityData data;
    protected final EntityPlayer player;

    public boolean available = true;

    public EntityTPMarking(EntityPlayer player) {
        super(player.worldObj);
        data = AbilityData.get(player);
        this.player = player;
        setPosition(player.posX, player.posY, player.posZ);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        rotationPitch = player.rotationPitch;
        rotationYaw = player.rotationYaw;

        if (available && rand.nextDouble() < 0.4) {
            particleFac.setPosition(posX + RandUtils.ranged(-1, 1), posY + RandUtils.ranged(0.2, 1.6) - 1.6,
                    posZ + RandUtils.ranged(-1, 1));
            particleFac.setVelocity(RandUtils.ranged(-.03, .03), RandUtils.ranged(0, 0.05),
                    RandUtils.ranged(-.03, .03));

            worldObj.spawnEntityInWorld(particleFac.next(worldObj));
        }
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    public double getDist() {
        return this.getDistanceToEntity(player);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {
    }

}