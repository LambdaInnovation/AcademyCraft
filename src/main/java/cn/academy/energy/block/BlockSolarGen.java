/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.core.block.ACBlockMulti;
import cn.lambdalib.template.client.render.block.RenderEmptyBlock;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class BlockSolarGen extends ACBlockMulti {

    public BlockSolarGen() {
        super("solar_gen", Material.rock);
        setBlockBounds(0, 0, 0, 1, 0.5f, 1);
        this.finishInit();
        
        setHardness(1.5f);
        setHarvestLevel("pickaxe", 1);
    }

    @SideOnly(Side.CLIENT)
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
        return new TileSolarGen();
    }

    @Override
    public double[] getRotCenter() {
        return new double[] { 0.5, 0, 0.5 };
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float hx, float hy, float hz) {
        TileEntity te = world.getTileEntity(x, y, z);
        if(te instanceof TileSolarGen) {
            if (world.isRemote) {
                openGui((TileSolarGen) te);
            }
            return true;
        } else {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    private void openGui(TileSolarGen te) {
        // Minecraft.getMinecraft().displayGuiScreen(new GuiLinkToNode(te));
    }

}
