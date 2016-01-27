/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.block;

import cn.academy.core.AcademyCraft;
import cn.lambdalib.multiblock.BlockMulti;
import cn.lambdalib.util.mc.StackUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public abstract class ACBlockMulti extends BlockMulti {

    public ACBlockMulti(String name, Material mat) {
        super(mat);
        setCreativeTab(AcademyCraft.cct);
        setBlockName("ac_" + name);
        setBlockTextureName("academy:" + name);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int wtf) {
        if(!world.isRemote) {
            TileEntity te = world.getTileEntity(x, y, z);
            if(te instanceof IInventory) {
                StackUtils.dropItems(world, x, y, z, (IInventory) te);
            }
        }
        super.breakBlock(world, x, y, z, block, wtf);
    }
    
}
