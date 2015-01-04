/**
 * 
 */
package cn.academy.core.client.gui.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.block.TileDeveloper;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.register.IGuiElement;

/**
 * Main class of Developer GUI.
 * @author WeathFolD
 */
public class GuiDeveloper extends LIGuiScreen {
	
	//Constants 
	protected static final int
		WIDTH = 228,
		HEIGHT = 185;
	public final int[] DEFAULT_COLOR = {48, 155, 190};

	//States
	int pageID;
	
	protected PageMainBase pageMain;
	private List<DevSubpage> subs = new ArrayList<DevSubpage>();
	
	AbilityData data;
	TileDeveloper dev;
	EntityPlayer user;
	
	public GuiDeveloper(TileDeveloper dev) {
		this.user = dev.getUser();
		this.dev = dev;
		this.data = AbilityDataMain.getData(user);
		
		pageMain = new PageMainOrdinary(this);
		subs.add(new PageLearn(pageMain));
		subs.add(new PageSkills(pageMain));
		
		updateVisiblility();
	}
    
    private void updateVisiblility() {
    	for(int i = 0; i < subs.size(); ++i) {
    		subs.get(i).visible = i == pageID;
    	}
    }
    
    protected DevSubpage getCurPage() {
    	return subs.get(pageID);
    }
	
	@Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }
	
    public static class Element implements IGuiElement {

		@Override
		public Object getServerContainer(EntityPlayer player, World world,
				int x, int y, int z) {
			return null;
		}

		@Override
		public Object getClientGui(EntityPlayer player, World world, int x,
				int y, int z) {
			TileEntity te = world.getTileEntity(x, y, z);
			if(te == null || !(te instanceof TileDeveloper)) {
				AcademyCraftMod.log.error("Failed opening developer gui: no TileDeveloper found");
				return null;
			}
			return new GuiDeveloper((TileDeveloper) te);
		}
    	
    }
	
}
