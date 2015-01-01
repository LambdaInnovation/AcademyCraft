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
import cn.liutils.api.client.gui.GuiScreenLIAdaptor;
import cn.liutils.api.client.gui.LIGuiPage;
import cn.liutils.api.register.IGuiElement;

/**
 * @author WeathFolD
 *
 */
public class GuiDeveloper extends GuiScreenLIAdaptor {
	
	//Constants 
	protected static final int
		WIDTH = 228,
		HEIGHT = 185;
	
	public final int[] DEFAULT_COLOR = {48, 155, 190};

	int pageID;
	
	LIGuiPage pageMain;
	protected List<DevSubpage> subs = new ArrayList<DevSubpage>();
	
	AbilityData data;
	TileDeveloper dev;
	EntityPlayer user;
	
	public GuiDeveloper(TileDeveloper dev) {
		super(WIDTH, HEIGHT);
		user = dev.getUser();
		this.dev = dev;
		data = AbilityDataMain.getData(user);
		
		subs.add(new PageLearn(this));
		subs.add(new PageSkills(this));
		pageMain = new PageMainOrdinary(this);
	}
	
    public void drawScreen(int par1, int par2, float par3)
    {
    	update();
    	this.drawDefaultBackground();
    	GL11.glPushMatrix(); {
	    	GL11.glTranslated(0, 0, 100);
	    	drawElements(par1, par2);
    	} GL11.glPopMatrix();
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
    
    public static void bindColor(Vec3 cv) {
    	GL11.glColor3d(cv.xCoord, cv.yCoord, cv.zCoord);
    }
    
    public static void bindColor(int[] arr) {
    	bindColor(arr[0], arr[1], arr[2]);
    }
    
    public static void bindColor(int r, int g, int b) {
    	GL11.glColor3ub((byte)r, (byte)g, (byte)b);
    }
    
    protected DevSubpage getCurPage() {
    	return subs.get(pageID);
    }

	@Override
	public void updateActivedPages(Set<LIGuiPage> pages) {
		pages.add(pageMain);
		pages.add(subs.get(pageID));
	}
	
	@Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }
	
}
