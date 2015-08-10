package cn.academy.energy.client.gui;

import javax.vecmath.Vector2d;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.Resources;
import cn.academy.energy.api.block.IWirelessUser;
import cn.annoreg.mc.network.Future;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.gui.event.MouseDownEvent.MouseDownHandler;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;
import cn.liutils.util.helper.Font.Align;



public class EnergyUIHelper {
	
	public static final Color 
		CRL_GLOW = new Color().setColor4i(0, 214, 232, 180),
		CRL_BACK = new Color().setColor4i(0, 128, 165, 90);
	
	public static final ResourceLocation
		BTN_WIFI = Resources.getTexture("guis/button/button_wifi"),
		BTN_WIFI_N = Resources.getTexture("guis/button/button_wifi2");
	
	public static void drawBox(double x, double y, double width, double height) {
		CRL_BACK.bind();
		HudUtils.colorRect(x, y, width, height);
		
		ACRenderingHelper.drawGlow(x, y, width, height, 3, CRL_GLOW);
	}
	
	public static void drawTextBox(String str, double x, double y, double size) {
		GL11.glEnable(GL11.GL_BLEND);
		drawTextBox(str, x, y, size, 233333, Align.LEFT);
	}
	
	public static void drawTextBox(String str, double x, double y, double size, double limit) {
		GL11.glEnable(GL11.GL_BLEND);
		drawTextBox(str, x, y, size, limit, Align.LEFT);
	}
	
	public static void drawTextBox(String str, double x, double y, double size, double limit, Align align) {
		Vector2d vec = Font.font.simDrawWrapped(str, size, limit);
		double X0 = x, Y0 = y, MARGIN = Math.min(5, size * 0.3);
		
		if(align == Align.CENTER) {
			X0 -= vec.x / 2;
		} else if(align == Align.RIGHT) {
			X0 -= vec.x;
		}
		
		drawBox(X0, Y0, MARGIN * 2 + vec.x, MARGIN * 2 + vec.y);
		Font.font.drawWrapped(str, X0 + MARGIN, Y0 + MARGIN, size, 0xffffff, limit);
	}
	
	/**
	 * Add the callback to open the link node UI to the button.
	 */
	public static void initNodeLinkButton(IWirelessUser target, Widget theButton) {
		DrawTexture.get(theButton).texture = BTN_WIFI_N;
		
		EnergyUISyncs.syncIsLinked((TileEntity) target, 
			Future.<Boolean>create((Boolean o) -> {
				System.out.println(o);
				if(o) {
					DrawTexture.get(theButton).texture = BTN_WIFI;
				}
			}));
		
		theButton.regEventHandler(new MouseDownHandler() {

			@Override
			public void handleEvent(Widget w, MouseDownEvent event) {
				Minecraft.getMinecraft().displayGuiScreen(new GuiLinkToNode(target));
			}
			
		});
	}
	
}
