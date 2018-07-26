/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.core.block.ACBlockMulti;
import cn.academy.energy.client.ui.GuiSolarGen;
import cn.lambdalib2.annoreg.core.Registrant;
import cn.lambdalib2.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib2.annoreg.mc.gui.RegGuiHandler;
import cn.lambdalib2.template.client.render.block.RenderEmptyBlock;
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
@Registrant
public class BlockSolarGen extends ACBlockMulti {

    @RegGuiHandler
    public static final GuiHandlerBase handler = new GuiHandlerBase() {
        @SideOnly(Side.CLIENT)
        @Override
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
            ContainerSolarGen container = ((ContainerSolarGen) getServerContainer(player, world, x, y, z));
            return container == null ? null : GuiSolarGen.apply(container);
        }

        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileSolarGen) {
                return new ContainerSolarGen(player, ((TileSolarGen) te));
            } else {
                return null;
            }
        }
    };

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
        if(handler != null && !player.isSneaking()) {
            if(!world.isRemote)
                handler.openGuiContainer(player, world, x, y, z);
            return true;
        }
        return false;
    }

}
