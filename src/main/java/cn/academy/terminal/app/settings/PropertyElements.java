package cn.academy.terminal.app.settings;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import cn.academy.core.client.Resources;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.gui.event.MouseDownEvent.MouseDownHandler;

public class PropertyElements {
	
	public static IPropertyElement CHECKBOX = new IPropertyElement() {
		
		final ResourceLocation 
			CHECK_TRUE = Resources.getTexture("guis/check_true"),
			CHECK_FALSE = Resources.getTexture("guis/check_false");

		@Override
		public Widget getWidget(UIProperty prop) {
			Configuration cfg = getConfig();
			Property p = cfg.get(prop.category, prop.id, (boolean) prop.defValue);
			
			Widget ret = SettingsUI.loaded.getWidget("t_checkbox").copy();
			TextBox.get(ret.getWidget("text")).setContent(prop.getDisplayID());
			
			Widget check = ret.getWidget("box");
			DrawTexture.get(check).setTex(p.getBoolean() ? CHECK_TRUE : CHECK_FALSE);
			
			check.regEventHandler(new MouseDownHandler() {

				@Override
				public void handleEvent(Widget w, MouseDownEvent event) {
					boolean b = !p.getBoolean();
					p.set(b);
					DrawTexture.get(check).setTex(b ? CHECK_TRUE : CHECK_FALSE);
				}
				
			});
			
			return ret;
		}
		
	};
	
}
