/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.entity;

import cn.academy.vanilla.teleporter.client.RenderMarker;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.util.entityx.EntityAdvanced;
import cn.lambdalib.util.helper.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@Registrant
@SideOnly(Side.CLIENT)
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityMarker extends EntityAdvanced {

    @RegEntity.Render
    public static RenderMarker renderer;

    public Entity target = null;
    public Color color = Color.white();
    public boolean ignoreDepth = false;

    public EntityMarker(Entity entity) {
        this(entity.worldObj);
        setPosition(entity.posX, entity.posY, entity.posZ);
        setSize(0.5f, 0.5f);
        target = entity;
    }

    public EntityMarker(World world) {
        super(world);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (target != null)
            setPosition(target.posX, target.posY, target.posZ);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
    }

}
