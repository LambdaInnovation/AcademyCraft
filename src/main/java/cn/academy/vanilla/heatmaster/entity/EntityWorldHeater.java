package cn.academy.vanilla.heatmaster.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import cn.liutils.entityx.EntityAdvanced;
import cn.liutils.util.client.ViewOptimize.IAssociatePlayer;

public class EntityWorldHeater extends EntityAdvanced implements
		IAssociatePlayer
{

	public EntityWorldHeater(World world)
	{
		super(world);
		// TODO 自动生成的构造函数存根
	}

	@Override
	public EntityPlayer getPlayer()
	{
		// TODO 自动生成的方法存根
		return null;
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_)
	{
		// TODO 自动生成的方法存根

	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_)
	{
		// TODO 自动生成的方法存根

	}

}
