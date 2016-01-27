/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.entity;

import cn.academy.vanilla.meltdowner.client.render.RenderDiamondShield;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.util.entityx.EntityAdvanced;
import cn.lambdalib.util.helper.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityDiamondShield extends EntityAdvanced {
    
    @RegEntity.Render
    public static RenderDiamondShield renderer;
    
    public static final float SIZE = 1.8f;
    
    final EntityPlayer player;

    public EntityDiamondShield(EntityPlayer _player) {
        super(_player.worldObj);
        player = _player;
        this.setSize(SIZE, SIZE);
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        
        Motion3D mo = new Motion3D(player, true).move(1);
        mo.py -= 0.5;
        setPosition(mo.px, mo.py, mo.pz);
        
        this.rotationYaw = player.rotationYawHead;
        this.rotationPitch = player.rotationPitch;
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

}
