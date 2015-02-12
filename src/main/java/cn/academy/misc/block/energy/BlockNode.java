/**
 * 
 */
package cn.academy.misc.block.energy;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.misc.block.energy.container.ContainerNode;
import cn.academy.misc.block.energy.tile.impl.TileNode;
import cn.academy.misc.client.gui.GuiNode;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 */
@RegistrationClass
public class BlockNode extends BlockContainer {
	
	private IIcon sideIcon, mainIcon;

	public BlockNode() {
		super(Material.rock);
		setBlockName("ac_node");
		setCreativeTab(AcademyCraft.cct);
	}
	
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
    	return side == 0 || side == 1 ? mainIcon : sideIcon;
    }
    
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister ir) {
        sideIcon = ir.registerIcon("academy:nodes_side");
        mainIcon = ir.registerIcon("academy:nodes_main");
    }
    
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, 
    		int side, float accx, float accy, float accz){
    	TileNode node = safeGet(world, x, y, z);
    	if(node != null && node.isUseableByPlayer(player)) {
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
		
		@SideOnly(Side.CLIENT)
		protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileNode node = BlockNode.safeGet(world, x, y, z);
			if(node != null) {
				return new GuiNode(new ContainerNode(node, player));
			}
			return null;
		}
		
		protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileNode node = BlockNode.safeGet(world, x, y, z);
			if(node != null) {
				return new ContainerNode(node, player);
			}
			return null;
		}
		
	};

}
