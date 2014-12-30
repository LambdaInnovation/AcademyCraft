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
	
	public final Vec3 DEFAULT_COLOR = Vec3.createVectorHelper(46D / 255 , 192D / 255, 240D / 255);

	int pageID;
	
	LIGuiPage pageMain;
	List<DevSubpage> subs = new ArrayList<DevSubpage>();
	
	TileDeveloper dev;
	EntityPlayer user;
	
	public GuiDeveloper(TileDeveloper dev) {
		super(WIDTH, HEIGHT);
		user = dev.getUser();
		this.dev = dev;
		
		subs.add(new PageLearn(this));
		pageMain = new PageMainBase(this);
	}
	
    public void drawScreen(int par1, int par2, float par3)
    {
    	update();
    	drawElements(par1, par2);
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
    
    protected DevSubpage getCurPage() {
    	return subs.get(pageID);
    }

	@Override
	public void updateActivedPages(Set<LIGuiPage> pages) {
		pages.add(pageMain);
		pages.add(subs.get(pageID));
	}
	
}
