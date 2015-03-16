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
package cn.academy.core.client.gui.dev;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.AcademyCraft;
import cn.academy.core.block.dev.MsgDismount;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.gui.LIGui;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.util.HudUtils;
import cn.liutils.util.render.LambdaFont;

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

	//States
	int pageID;
	
	protected PageMain pageMain;
	protected List<DevSubpage> subs = new ArrayList<DevSubpage>();
	
	public static LambdaFont FONT = ACClientProps.FONT_YAHEI_32;
	
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
		if(data.hasAbility())
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
    
    @Override
	public void drawScreen(int mx, int my, float w)
    {
    	HudUtils.setTextureResolution(512, 512);
    	super.drawScreen(mx, my, w);
    }
    
    @Override
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
	
	public static double strLen(String text, double size) {
		return ACClientProps.FONT_YAHEI_32.getWidth(text, size);
	}
	
}
