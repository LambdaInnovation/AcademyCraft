/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.block;

import cn.academy.core.block.ACBlockMulti;
import cn.academy.energy.client.gui.GuiLinkToNode;
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
    
    @SideOnly(Side.CLIENT)
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, 
            float hx, float hy, float hz) {
        if(world.isRemote) {
            TileEntity te = world.getTileEntity(x, y, z);
            if(te instanceof TileSolarGen) {
                Minecraft.getMinecraft().displayGuiScreen(
                        new GuiLinkToNode((TileSolarGen) te));
                return true;
            }
        }
        return false;
    }

}
