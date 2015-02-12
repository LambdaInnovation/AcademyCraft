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
package cn.academy.energy.block;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.block.tile.impl.TileWindGenerator;
import cn.liutils.template.block.BlockDirectionalMulti;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;


/**
 * 风能发电机Block类
 * TODO: Not added in β
 * @author WeAthFolD
 */
public class BlockWindGenerator extends BlockDirectionalMulti {

	public BlockWindGenerator() {
		super(Material.rock);
		setBlockName("windgen");
		setBlockTextureName("academy:windgen");
		setCreativeTab(AcademyCraft.cct);
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileWindGenerator();
	}

	@Override
	public Vec3 getRenderOffset() {
		return null;
	}
	
    @SideOnly(Side.CLIENT)
    public Vec3 getOffsetRotated(int dir) {
    	return Vec3.createVectorHelper(0.5D, 0D, 0.5D);
    }


}
