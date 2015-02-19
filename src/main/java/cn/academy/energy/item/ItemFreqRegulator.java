/**
 * 
 */
package cn.academy.energy.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cn.academy.api.energy.IWirelessTile;
import cn.academy.core.AcademyCraft;
import cn.academy.energy.block.tile.base.TileUserBase;
import cn.academy.energy.client.gui.GuiFreqRegulator;
import cn.academy.energy.msg.fr.MsgFRInit;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


/**
 * Frequency regulator
 * @author WeathFolD
 */
@RegistrationClass
public class ItemFreqRegulator extends Item {
	
	public static final int LIST_MAX = 8;
	
	public ItemFreqRegulator() {
		setMaxDamage(30);
		setUnlocalizedName("ac_freqreg");
		setTextureName("academy:freqreg");
		setCreativeTab(AcademyCraft.cct);
	}
	
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, 
    		int x, int y, int z, int side, float tx, float ty, float tz) {
    	TileEntity te = world.getTileEntity(x, y, z);
    	if(!(te instanceof TileUserBase)) {
    		return false;
    	}
    	guiHandler.openGuiContainer(player, world, x, y, z);
    	stack.damageItem(1, player);
        return true;
    }
    
    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
    	@SideOnly(Side.CLIENT)
    	protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
    		TileUserBase te = tsGet(world, x, y, z);
    		if(te == null) return null;
    		return new GuiFreqRegulator(te);
    	}
    	
    	protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
    		return null;
    	}
    	
    	private TileUserBase tsGet(World world, int x, int y, int z) {
    		TileEntity te = world.getTileEntity(x, y, z);
    		if(!(te instanceof TileUserBase)) {
    			AcademyCraft.log.error("WTF? Not energy tile?");
    			return null;
    		}
    		return (TileUserBase) te;
    	}
    };
}
