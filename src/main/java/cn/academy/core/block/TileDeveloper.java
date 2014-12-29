/**
 * 
 */
package cn.academy.core.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.client.gui.dev.GuiDeveloper;
import cn.academy.core.proxy.ACCommonProps;
import cn.liutils.api.util.EntityManipHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 能力开发机的TE
 * @author WeathFolD
 */
public class TileDeveloper extends TileEntity {
	
	private EntityPlayer user;

	public TileDeveloper() { }
	
	@Override
	@SideOnly(Side.CLIENT)
	public void updateEntity() {
		GuiScreen gs = Minecraft.getMinecraft().currentScreen;
		if(gs == null || !(gs instanceof GuiDeveloper)) {
			userQuit();
		}
	}
	
	/**
	 * 尝试让某个玩家使用开发机
	 * @param player
	 * @return if attempt successful
	 */
	public boolean use(EntityPlayer player) {
		if(user != null) return false;
		user = player;
		EntityManipHandler.addEntityManip(new DevPlayerManip(user, this), true);
		player.openGui(AcademyCraftMod.INSTANCE, ACCommonProps.GUI_ID_ABILITY_DEV, player.worldObj, 0, 0, 0);
		return true;
	}
	
	public void userQuit() {
		user = null;
	}
	
	public EntityPlayer getUser() {
		return user;
	}

}
