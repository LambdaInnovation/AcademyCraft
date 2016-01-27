/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block.wind;

import cn.academy.core.block.ACBlockContainer;
import cn.lambdalib.template.client.render.block.RenderEmptyBlock;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class BlockWindGenPillar extends ACBlockContainer {

    public BlockWindGenPillar() {
        super("windgen_pillar", Material.rock, null);
        setHardness(4.0f);
        setHarvestLevel("pickaxe", 2);
    }
    
    @Override
    public int getRenderType() {
        return RenderEmptyBlock.id;
    }
    
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileWindGenPillar();
    }

}
