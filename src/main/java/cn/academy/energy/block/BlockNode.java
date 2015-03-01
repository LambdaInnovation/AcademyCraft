/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.block.tile.impl.ContainerNode;
import cn.academy.energy.block.tile.impl.TileNode;
import cn.academy.energy.client.gui.GuiNode;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Energy Node
 * @author WeathFolD
 */
@RegistrationClass
public class BlockNode extends BlockContainer {
	
	private IIcon sideIcon, mainIcon;

	public BlockNode() {
		super(Material.rock);
		setBlockName("ac_node");
		setCreativeTab(AcademyCraft.cct);
		setHardness(2.0f);
	}
	
    @Override
	@SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
    	return side == 0 || side == 1 ? mainIcon : sideIcon;
    }
    
    @Override
	@SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister ir) {
        sideIcon = ir.registerIcon("academy:nodes_side");
        mainIcon = ir.registerIcon("academy:nodes_main");
    }
    
    @Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, 
    		int side, float accx, float accy, float accz){
    	TileNode node = safeGet(world, x, y, z);
    	if(node != null && !player.isSneaking() && node.isUseableByPlayer(player)) {
    		element.openGuiContainer(player, world, x, y, z);
    		return true;
    	}
        return false;
    }
    
    @Override
    public boolean isOpaqueCube() {
    	return false;
    }

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileNode();
	}
	
    @Override
	@SideOnly(Side.CLIENT)
    public int getRenderBlockPass() {
        return -1;
    }
    
    public static TileNode safeGet(World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		return (TileNode) (te instanceof TileNode ? te : null);
	}
    
    @RegGuiHandler
	public static GuiHandlerBase element = new GuiHandlerBase() {
		
		@Override
		@SideOnly(Side.CLIENT)
		protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileNode node = BlockNode.safeGet(world, x, y, z);
			if(node != null) {
				System.out.println("open client");
				return new GuiNode(new ContainerNode(node, player));
			}
			return null;
		}
		
		@Override
		protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileNode node = BlockNode.safeGet(world, x, y, z);
			if(node != null) {
				System.out.println("open server");
				return new ContainerNode(node, player);
			}
			return null;
		}
		
	};

}
