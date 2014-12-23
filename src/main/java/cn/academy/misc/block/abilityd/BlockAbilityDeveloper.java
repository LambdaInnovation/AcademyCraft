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
package cn.academy.misc.block.abilityd;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.block.ACBlockContainer;
import cn.academy.misc.item.ItemModuleAttached;
import cn.liutils.core.proxy.LIClientProps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 能力开发机的方块类。<br/>
 * META: <br/><code>
 * 	第1位指示头尾：0Head 1Tail <br/>
 * 	第2、3位：byte(2) facingDirection 值和ForgeDirection有一个映射<br/></code>
 * Head方块执行渲染行为<br/>
 * @author WeAthFolD
 */
public class BlockAbilityDeveloper extends ACBlockContainer {
	
	private static final int[] dirMap = { 3, 4, 2, 5 };
	
	public BlockAbilityDeveloper() {
		super(Material.iron);
		setHardness(2.0F);
		setCreativeTab(AcademyCraftMod.cct);
		setBlockName("ability_developer");
		setBlockTextureName("academy:bed");
	}
	
	public static ForgeDirection getFacingDirection(int metadata) {
		return ForgeDirection.values()[dirMap[metadata >> 1]];
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
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World wrld, int x, int y, int z)
    {
    	return null;
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public int getRenderType() {
		return LIClientProps.RENDER_TYPE_EMPTY;
	}
	
	@Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack)
    {
        int l = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        int metadata = l << 1;

        world.setBlockMetadataWithNotify(x, y, z, metadata, 0x02);
        
        ForgeDirection dir = getFacingDirection(metadata); 
        Block b0 = world.getBlock(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
        if(b0 == Blocks.air || b0 instanceof BlockLiquid)
        	world.setBlock(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, this, metadata + 1, 0x03); //set the 'tail' block
        else {
        	world.setBlockToAir(x, y, z);
        	if(!world.isRemote) {
        		this.dropBlockAsItem(world, x, y, z, new ItemStack(this));
        	}
        }
    }
    
    @Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
    {
    	super.onNeighborBlockChange(world, x, y, z, block);
    	int metadata = world.getBlockMetadata(x, y, z);
		ForgeDirection dir = getFacingDirection(metadata);
		if((metadata & 0x01) == 1) {
			dir = dir.getOpposite();
		}
		if(world.getBlock(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ) != this)
			world.setBlockToAir(x, y, z);
    }
    
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitx, float hity, float hitz)
    {
    	//Always switch to the HEAD block
    	
    	if(isTale(world, x, y, z)) {
    		int[] coords = getOrigin(world, x, y, z);
    		x = coords[0];
    		y = coords[1];
    		z = coords[2];
    	}
    	ItemStack stack = player.getCurrentEquippedItem();
    	if(world.getBlock(x, y, z) != this || player.isSneaking() || 
    			(stack != null && stack.getItem() instanceof ItemModuleAttached)) return false;
    	TileAbilityDeveloper dev = (TileAbilityDeveloper) world.getTileEntity(x, y, z);
    	if(dev.tryMount(player)) {
    		player.openGui(AcademyCraftMod.INSTANCE, ACClientProps.GUI_ID_ABILITY_DEV, world, x, y, z);
    	}
    	return true;
    }
    
    public static int[] getOrigin(World world, int x, int y, int z) {
    	int meta = world.getBlockMetadata(x, y, z);
    	if((meta & 0x01) == 1) {
    		ForgeDirection dir = getFacingDirection(meta).getOpposite();
    		x += dir.offsetX;
    		z += dir.offsetZ;
    	}
    	return new int[] {x, y, z};
    }
    
	public static boolean isTale(World world, int x, int y, int z) {
		return (world.getBlockMetadata(x, y, z) & 0x01) == 1;
 	}
    
	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileAbilityDeveloper();
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block par5, int par6) {
		TileAbilityDeveloper dev = (TileAbilityDeveloper) world.getTileEntity(x, y, z);
		for(int i = 0; i < 4; i++) {
			IADModuleAttached module = dev.getModule(i);
			if(module != null) {
				ItemStack st = module.getDrop();
				if(st != null)
					this.dropItems(world, x, y, z, st);
			}
		}
		super.breakBlock(world, x, y, z, par5, par6);
	}
	
}
