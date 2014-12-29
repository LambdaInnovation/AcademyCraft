/**
 * 
 */
package cn.academy.core.client.gui.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.client.TextUtils;
import cn.liutils.api.client.gui.GuiScreenLIAdaptor;
import cn.liutils.api.client.gui.LIGuiPage;
import cn.liutils.api.client.gui.part.LIGuiPart;
import cn.liutils.api.client.util.HudUtils;
import cn.liutils.api.client.util.RenderUtils;
import cn.liutils.api.register.IGuiElement;

/**
 * @author WeathFolD
 *
 */
public class GuiDeveloper extends GuiScreenLIAdaptor {
	
	//Constants
	public static final float 
		TITLE_CENTER_X = 165.75F, TITLE_CENTER_Y = 10.5F;
	
	protected static final int
		WIDTH = 228,
		HEIGHT = 185;
	
	public static final Vec3 DEFAULT_COLOR = Vec3.createVectorHelper(46D / 255 , 150D / 255, 165D / 255);

	int pageID;
	
	LIGuiPage pageMain = new PageMain();
	List<DevSubpage> subs = new ArrayList<DevSubpage>();
	
	public GuiDeveloper() {
		super(WIDTH, HEIGHT);
		subs.add(new PageLearn(this));
	}
	
    public void drawScreen(int par1, int par2, float par3)
    {
    	update();
    	drawElements(par1, par2);
    }
    
    public class PageMain extends LIGuiPage {

		public PageMain() {
			super(GuiDeveloper.this, "main", 0, 0);
		}

		@Override
		public void drawPage() {
			//Page name
			String pname = getCurPage().getDisplayName();
			//System.out.println(pname);
			bindColor(DEFAULT_COLOR);
			TextUtils.drawAtCenter(TextUtils.FONT_CONSOLAS_64, pname, TITLE_CENTER_X, TITLE_CENTER_Y, 12);
			
			//System.out.println("dp");
			GL11.glEnable(GL11.GL_BLEND);
			RenderUtils.loadTexture(ACClientProps.TEX_GUI_AD_MAIN);
			GL11.glColor4f(1F, 1F, 1, 1);
			HudUtils.setTextureResolution(512, 512);
			HudUtils.drawTexturedModalRect(0, 0, 0, 0, WIDTH, HEIGHT, 456, 369);
		}

		@Override
		public void addElements(Set<LIGuiPart> set) {
		}

		@Override
		public void onPartClicked(LIGuiPart part, float subX, float subY) {
		}
    	
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
			return new GuiDeveloper();
		}
    	
    }
    
    public static void bindColor(Vec3 cv) {
    	GL11.glColor3d(cv.xCoord, cv.yCoord, cv.zCoord);
    }
    
    private DevSubpage getCurPage() {
    	return subs.get(pageID);
    }

	@Override
	public void updateActivedPages(Set<LIGuiPage> pages) {
		pages.add(pageMain);
		pages.add(subs.get(pageID));
	}
	
}
