/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.vanilla.teleporter.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import cn.academy.vanilla.teleporter.client.RenderMarker;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.util.entityx.EntityAdvanced;
import cn.lambdalib.util.helper.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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
