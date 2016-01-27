/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.core.block.ACBlockMulti;
import cn.academy.energy.client.gui.matrix.GuiMatrix;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib.annoreg.mc.gui.RegGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 *
 */
@Registrant
public class BlockMatrix extends ACBlockMulti {
    
    public BlockMatrix() {
        super("matrix", Material.rock);
        setHardness(3.0f);
        setLightLevel(1f);
        
        addSubBlock(0, 0, 1);
        addSubBlock(1, 0, 1);
        addSubBlock(1, 0, 0);
        
        addSubBlock(0, 1, 0);
        addSubBlock(0, 1, 1);
        addSubBlock(1, 1, 1);
        addSubBlock(1, 1, 0);
        
        this.finishInit();
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileMatrix();
    }

    @Override
    public double[] getRotCenter() {
        return new double[] { 1.0, 0, 1.0};
    }
    
//    @Override
//    public int getRenderType() {
//        return 0;
//    }
    
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float tx, float ty, float tz) {
        if(!player.isSneaking()) {
            int[] center = this.getOrigin(world, x, y, z);
            if(center != null) {
                guiHandler.openGuiContainer(player, world, center[0], center[1], center[2]);
            }
            return true;
        }
        return false;
    }
    
    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @SideOnly(Side.CLIENT)
        @Override
        protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
            TileMatrix te = check(world, x, y, z);
            return te == null ? null : new GuiMatrix(new ContainerMatrix(te, player));
        }
        
        @Override
        protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
            TileMatrix te = check(world, x, y, z);
            return te == null ? null : new ContainerMatrix(te, player);
        }
        
        private TileMatrix check(World world, int x, int y, int z) {
            TileEntity te = world.getTileEntity(x, y, z);
            return (TileMatrix) (te instanceof TileMatrix ? te : null);
        }
    };

}
