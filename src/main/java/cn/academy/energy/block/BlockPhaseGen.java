/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.core.block.ACBlockContainer;
import cn.academy.energy.client.ui.GuiPhaseGen;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib.annoreg.mc.gui.RegGuiHandler;
import cn.lambdalib.template.client.render.block.RenderEmptyBlock;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@Registrant
public class BlockPhaseGen extends ACBlockContainer {
    
    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @SideOnly(Side.CLIENT)
        @Override
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
            ContainerPhaseGen container = (ContainerPhaseGen) getServerContainer(player, world, x, y, z);
            return container == null ? null : GuiPhaseGen.apply(container);
        }
        
        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
            TileEntity te = world.getTileEntity(x, y, z);
            return te instanceof TilePhaseGen ? new ContainerPhaseGen(player, (TilePhaseGen) te) : null;
        }
    };

    public BlockPhaseGen() {
        super("phase_generator", Material.rock, guiHandler);
        setHardness(2.5f);
        setHarvestLevel("pickaxe", 1);
    }
    
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderType() {
        return RenderEmptyBlock.id;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TilePhaseGen();
    }

}
