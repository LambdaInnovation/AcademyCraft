/**
 * 
 */
package cn.academy.core.client.gui.dev;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.AcademyCraft;
import cn.academy.core.block.dev.MsgDismount;
import cn.academy.core.block.dev.TileDeveloper;
import cn.liutils.api.draw.tess.GUIRect;
import cn.liutils.api.draw.tess.RectMapping;
import cn.liutils.api.gui.LIGui;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.api.gui.Widget;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.TextUtils;
import cn.liutils.util.render.TrueTypeFont;

/**
 * Main class of Developer GUI.
 * @author WeathFolD
 */
public class GuiDeveloper extends LIGuiScreen {
    
	//Constants 
	protected static final int
		WIDTH = 228,
		HEIGHT = 185;
	
	public final int[] 
		DEFAULT_COLOR = {48, 155, 190},
		EXP_INDI_COLOR = { 161, 199, 152 },
		EU_INDI_COLOR = { 234, 84, 44 };
	
	public static final TrueTypeFont FONT = TextUtils.FONT_YAHEI_64;

	//States
	int pageID;
	
	protected PageMain pageMain;
	protected List<DevSubpage> subs = new ArrayList<DevSubpage>();
	
	AbilityData data;
	TileDeveloper dev;
	EntityPlayer user;
	
	public GuiDeveloper(TileDeveloper dev) {
		this.user = dev.getUser();
		this.dev = dev;
		this.data = AbilityDataMain.getData(user);
		
		reload();
	}
	
	void reload() {
		gui = new LIGui();
		gui.addWidget(pageMain = new PageMain(this));
		subs.clear();
		subs.add(new PageLearn(this));
		subs.add(new PageSkills(this));
		
		for(DevSubpage sp : subs) {
			pageMain.addWidget(sp);
		}
		 
		updateVisiblility();
	}
    
    protected void updateVisiblility() {
    	for(int i = 0; i < subs.size(); ++i) {
    		subs.get(i).doesDraw = (i == pageID);
    	}
    }
    
    public void drawScreen(int mx, int my, float w)
    {
    	HudUtils.setTextureResolution(512, 512);
    	super.drawScreen(mx, my, w);
    }
    
    public void onGuiClosed() {
    	super.onGuiClosed();
    	//TODO: State checking and doesn't userQuit when re-constructing
    	dev.userQuit();
    	AcademyCraft.netHandler.sendToServer(new MsgDismount(this.dev));
    }
    
    protected DevSubpage getCurPage() {
    	return subs.get(pageID);
    }
	
	@Override
    public boolean doesGuiPauseGame() {
        return false;
    }
	
	public static void drawText(String text, double x, double y, float size) {
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, text, x, y, size);
	}
	
	
}
