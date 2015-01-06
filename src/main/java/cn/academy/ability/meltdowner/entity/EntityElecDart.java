package cn.academy.ability.meltdowner.entity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.ability.meltdowner.client.render.RenderElecDart;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

@RegistrationClass
@RegEntity(renderName = "renderer")
public class EntityElecDart extends Entity {
	
	@SideOnly(Side.CLIENT)
	public static RenderElecDart renderer;

	public EntityElecDart(EntityPlayer player) {
		super(player.worldObj);
	}
	
	public void emit() {
		
	}

	@Override
	protected void entityInit() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound var1) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound var1) {
		// TODO Auto-generated method stub

	}

}
