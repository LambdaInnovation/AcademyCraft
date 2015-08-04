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
package cn.academy.terminal.app.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.ability.api.ctrl.ClientController;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiScreen;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.ElementList;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.component.VerticalDragBar;
import cn.liutils.cgui.gui.component.VerticalDragBar.DraggedEvent;
import cn.liutils.cgui.gui.component.VerticalDragBar.DraggedHandler;
import cn.liutils.cgui.gui.event.global.GlobalMouseEvent;
import cn.liutils.cgui.gui.event.global.GlobalMouseEvent.GlobalMouseHandler;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;

/**
 * @author WeAthFolD
 */
public class SettingsUI extends LIGuiScreen {
	
	static LIGui loaded;
	static {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/settings.xml"));
	}
	
	private static Map<String, List<UIProperty>> properties = new HashMap();
	static {
		addProperty(PropertyElements.CHECKBOX, "generic", "attackPlayer", true);
		addProperty(PropertyElements.CHECKBOX, "generic", "destroyBlocks", true);
	}
	
	public static void addProperty(IPropertyElement elem, String cat, String id, Object defValue) {
		List<UIProperty> list = properties.get(cat);
		if(list == null)
			properties.put(cat, list = new ArrayList());
		list.add(new UIProperty(elem, cat, id, defValue));
	}
	
	public SettingsUI() {
		initPages();
	}
	
	private void initPages() {
		Widget main = loaded.getWidget("main").copy();
		
		Widget area = main.getWidget("area");
		
		ElementList list = new ElementList(); 
		{
			for(Entry<String, List<UIProperty>> entry : properties.entrySet()) {
				Widget head = loaded.getWidget("t_cathead").copy();
				TextBox.get(head.getWidget("text")).setContent(local("cat." + entry.getKey()));
				list.addWidget(head);
				
				for(UIProperty prop : entry.getValue()) {
					list.addWidget(prop.element.getWidget(prop));
				}
				
				Widget placeholder = new Widget();
				placeholder.transform.setSize(10, 20);
				list.addWidget(placeholder);
			}
		} 
		area.addComponent(list);
		
		Widget bar = main.getWidget("scrollbar");
		bar.regEventHandler(new DraggedHandler() {

			@Override
			public void handleEvent(Widget w, DraggedEvent event) {
				list.setProgress((int) (list.getMaxProgress() * VerticalDragBar.get(w).getProgress()));
			}
			
		});
		
		gui.addWidget(main);
	}
	
	private String local(String id) {
		return StatCollector.translateToLocal("ac.settings." + id);
	}
	
}
