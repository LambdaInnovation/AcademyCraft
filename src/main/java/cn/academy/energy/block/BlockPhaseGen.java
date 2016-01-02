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

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.core.block.ACBlockContainer;
import cn.academy.energy.client.gui.GuiPhaseGen;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.gui.GuiHandlerBase;
import cn.lambdalib.annoreg.mc.gui.RegGuiHandler;
import cn.lambdalib.template.client.render.block.RenderEmptyBlock;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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
            return container == null ? null : new GuiPhaseGen(container);
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
    
    @Override
    public int getRenderType() {
        return RenderEmptyBlock.id;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TilePhaseGen();
    }

}
