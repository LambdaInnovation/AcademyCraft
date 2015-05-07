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
package cn.academy.energy.client.gui.node;

import net.minecraft.util.ResourceLocation;
import cn.academy.energy.block.ContainerNode;
import cn.academy.energy.block.TileNode;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiContainer;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegSubmoduleInit(side = RegSubmoduleInit.Side.CLIENT_ONLY)
public class GuiNode extends LIGuiContainer {
	
	static LIGui loaded;
	
	final ContainerNode container;
	final TileNode tile;
	
	Widget pageMain, pageSelect, pageCheck;
    
    public GuiNode(ContainerNode c) {
        super(c);
        container = c;
        tile = c.node;
        create();
    }
    
    private void create() {
    	pageMain = loaded.getWidget("window_main").copy();
    	pageSelect = loaded.getWidget("window_ssidselect").copy();
    	pageCheck = loaded.getWidget("window_check").copy();
    	
    	gui.addWidget(pageMain);
    	gui.addWidget(pageSelect);
    	gui.addWidget(pageCheck);
    	
    	pageSelect.transform.doesDraw = false;
    	pageCheck.transform.doesDraw = false;
    }
    
    private void wrapThunder(Widget thunder, final ThunderCond cond) {
    	thunder.transform.doesDraw = false;
    	thunder.regEventHandler(new FrameEventHandler() {
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				w.transform.doesDraw = cond.shows();
			}
    	});
    }

    public static void init() {
    	loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/node.xml"));
    }
    
    interface ThunderCond {
    	boolean shows();
    }
    
}
