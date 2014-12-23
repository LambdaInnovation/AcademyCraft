/**
 * Copyright (C) Lambda-Innovation, 2013-2014
 * This code is open-source. Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 */
package cn.academy.misc.block.dev;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.academy.core.AcademyCraftMod;
import cn.liutils.api.util.EntityUtils;
import cn.liutils.api.util.GenericUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * 能力开发机TileEntity。仍然在制作中。
 * 
 * @author WeAthFolD, Lyt99
 */
public class TileAbilityDeveloper extends TileEntity implements IEnergySink {

	public EntityPlayer mountPlayer;

	public static final int MAX_SIDES = 4;
	public Set<IADModule> plainModules = new HashSet();
	public int[] sidedModules = { -1, -1, -1, -1 };

	public static List<IADModuleAttached> attachedModuleList = new ArrayList();
	static {
//		attachedModuleList.add(new ModuleCard());
	}

	public TileAbilityDeveloper() {
	}

	public boolean insertAttachedModule(int a) {
		if (a < 0 || a >= attachedModuleList.size())
			return false;
		if (BlockAbilityDeveloper.isTale(worldObj, xCoord, yCoord, zCoord)) {
			int[] crds = BlockAbilityDeveloper.getOrigin(worldObj, xCoord,
					yCoord, zCoord);
			// Maybe dangerous?
			TileAbilityDeveloper dev = (TileAbilityDeveloper) worldObj
					.getTileEntity(crds[0], crds[1], crds[2]);
			return dev.insertAttachedModule(a);
		}
		for (int i = 0; i < 4; i++) {
			if (sidedModules[i] == -1) {
				sidedModules[i] = a;
//				AcademyCraftMod.netHandler.sendToDimension(
//						new MsgDeveloperAttachment(this, 0x01),
//						worldObj.provider.dimensionId);
				return true;
			}
		}
		return false;
	}

	public IADModuleAttached getModule(int ind) {
		return sidedModules[ind] == -1 ? null : GenericUtils.safeFetchFrom(
				attachedModuleList, sidedModules[ind]);
	}

	int ticksAfterUpdate = 0;

	@Override
	public void updateEntity() {
		if (!this.tileEntityInvalid && mountPlayer != null) {
			setPosition();
		}

		if (++ticksAfterUpdate > 40) {
			ticksAfterUpdate = 0;
//			AcademyCraftMod.netHandler.sendToDimension(new MsgDeveloperAttachment(
//					this, 0x03), worldObj.provider.dimensionId);
		}

		if (!worldObj.isRemote && !this.addedToEnergyNet) {
			this.onLoaded();
		}
	}

	@Override
	public void invalidate() {
		disMount();
		super.invalidate();
	}

	public EntityPlayer getMountedPlayer() {
		return mountPlayer;
	}

	public boolean tryMount(EntityPlayer player) {
		if (mountPlayer == null || mountPlayer == player) {
			mountPlayer = player;
			setPosition();
			player.getEntityData().setBoolean("ac_ondev", true);
			player.getEntityData().setByte("ac_devdir",
					(byte) (getBlockMetadata() >> 1));
			if (!player.worldObj.isRemote)
//				AcademyCraftMod.netHandler.sendToDimension(new MsgDeveloperPlayer(
//						player, true, getBlockMetadata() >> 1),
//						worldObj.provider.dimensionId);
			return true;
		}
		return false;
	}

	public void disMount() {
		if (mountPlayer != null) {
			mountPlayer.getEntityData().setBoolean("ac_ondev", false);
			mountPlayer.yOffset = worldObj.isRemote ? 1.62F : 0.0F;
			Vec3 vec3 = calculateExitPosition();
			EntityUtils.applyEntityToPos(mountPlayer, vec3);
			// mountPlayer.onGround = true;
			if (!mountPlayer.worldObj.isRemote)
//				AcademyCraftMod.netHandler.sendToDimension(new MsgDeveloperPlayer(
//						mountPlayer, false, getBlockMetadata() >> 1),
//						worldObj.provider.dimensionId);
			mountPlayer = null;
		}
	}

	private void setPosition() {
		ForgeDirection dir = BlockAbilityDeveloper
				.getFacingDirection(getBlockMetadata());
		mountPlayer.yOffset = 1.00F;
		mountPlayer.motionX = mountPlayer.motionY = mountPlayer.motionZ = 0.0;
		double x = xCoord + 0.5, y = yCoord + .63, z = zCoord + 0.5;
		mountPlayer.posX = x;
		mountPlayer.posY = y;
		mountPlayer.posZ = z;
	}

	private Vec3 calculateExitPosition() {
		ForgeDirection ndir = BlockAbilityDeveloper.getFacingDirection(
				getBlockMetadata()).getRotation(ForgeDirection.UP);
		return worldObj.getWorldVec3Pool().getVecFromPool(
				xCoord + 0.5 + ndir.offsetX, yCoord + 3.0F,
				zCoord + 0.5 + ndir.offsetZ);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		for (int i = 0; i < 4; i++)
			sidedModules[i] = nbt.getInteger("sm" + i);
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		for (int i = 0; i < 4; i++)
			nbt.setInteger("sm" + i, sidedModules[i]);
	}

	public double getActionSuccessProb() {
		return 0.7;
	}

	public double getDUModifier() {
		return 0.6;
	}

	// 莫名其妙的部分

	public int tier = 2, output = 512, maxEnergy = 2000000;
	public double energy = 0.0D;

	public void onLoaded() {
		// isSimulating到底是啥....
		MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
		this.addedToEnergyNet = true;
		System.out.println(this + "added to Energy Net");
	}

	public void onUnloaded() {
		if (this.addedToEnergyNet) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
			this.addedToEnergyNet = false;
		}
	}

	public void onChunkUnload() {
		if (this.loaded) {
			onUnloaded();
		}
		super.onChunkUnload();
	}

	// IEnergySink
	@Override
	public boolean acceptsEnergyFrom(TileEntity emitter,
			ForgeDirection direction) {
		return true;
	}

	@Override
	public double demandedEnergyUnits() {
		return this.maxEnergy - this.energy;
	}

	@Override
	public double injectEnergyUnits(ForgeDirection directionFrom, double amount) {
		if (this.energy >= this.maxEnergy) {
			return amount;
		}
		if (this.maxEnergy - this.energy < amount) {
			this.energy += this.maxEnergy - this.energy;
			return amount - this.maxEnergy + this.energy;
		}
		this.energy += amount;
		return 0.0D;
	}

	@Override
	public int getMaxSafeInput() {
		return this.output;
	}

	boolean addedToEnergyNet = false;
	boolean loaded = false;
}
