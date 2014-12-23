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

import cn.academy.core.AcademyCraftMod;
import cn.liutils.core.proxy.LIClientProps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * 开发机的磁场增强模块Block类。
 * @author WeAthFolD
 */
public class BlockFieldIncrease extends Block implements ITileEntityProvider {

	/**
	 * 
	 */
	public BlockFieldIncrease() {
		super(Material.iron);
		this.setHardness(4.0F);
		this.setHarvestLevel("pickaxe", 2);
		this.setCreativeTab(AcademyCraftMod.cct);
		this.setLightLevel(0.5F);
		setBlockName("fieldincr");
		setBlockTextureName("academy:machine");
	}
	
	@Override
    public boolean isOpaqueCube()
    {
		return false;
    }
	
	@Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public int getRenderType() {
		return LIClientProps.RENDER_TYPE_EMPTY;
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileFieldIncrease();
	}
	
	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world,
			int par2, int par3, int par4) {
		int meta = world.getBlockMetadata(par2, par3, par4);
		if(meta == 1 || meta == 3) {
			this.setBlockBounds(0.15F, 0.2F, 0.0F, 0.8F, 0.72F, 1.0F);
		} else {
			this.setBlockBounds(0.0F, 0.2F, 0.15F, 1.0F, 0.72F, 0.8F);
		}
	}
	
	
	private static final int[] dirMap = { 3, 4, 2, 5 };
	
	public static ForgeDirection getFacingDirection(int metadata) {
		return ForgeDirection.values()[dirMap[metadata]];
	}
	
	@Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack)
    {
        int l = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        int metadata = l;
        world.setBlockMetadataWithNotify(x, y, z, metadata, 0x03);
    }
}
