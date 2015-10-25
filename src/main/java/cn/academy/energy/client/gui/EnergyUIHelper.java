package cn.academy.energy.client.gui;

import javax.vecmath.Vector2d;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.Resources;
import cn.academy.energy.api.block.IWirelessUser;
import cn.annoreg.mc.network.Future;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.Tint;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.gui.event.MouseDownEvent.MouseDownHandler;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;
import cn.liutils.util.helper.Font.Align;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;



public class EnergyUIHelper {
	
	public static final Color 
		CRL_GLOW = new Color().setColor4i(0, 214, 232, 180),
		CRL_BACK = new Color().setColor4i(0, 128, 165, 90),
		CRL_GLOW_MONO = CRL_GLOW.monoize(),
		CRL_BACK_MONO = CRL_BACK.monoize();
	
	public static final ResourceLocation
		BTN_WIFI = Resources.getTexture("guis/button/button_wifi"),
		BTN_WIFI_N = Resources.getTexture("guis/button/button_wifi2");
	
	public static void drawBox(double x, double y, double width, double height) {
		drawBox(x, y, width, height, false);
	}
	
	public static void drawBox(double x, double y, double width, double height, boolean mono) {
		Color back, glow;
		if(mono) {
			back = CRL_BACK_MONO;
			glow = CRL_GLOW_MONO;
		} else {
			back = CRL_BACK;
			glow = CRL_GLOW;
		}
		
		back.bind();
		HudUtils.colorRect(x, y, width, height);
		
		ACRenderingHelper.drawGlow(x, y, width, height, 3, glow);
	}
	
	public static void drawTextBox(String str, double x, double y, double size) {
		GL11.glEnable(GL11.GL_BLEND);
		drawTextBox(str, x, y, size, 233333, Align.LEFT);
	}
	
	public static void drawTextBox(String str, double x, double y, double size, double limit) {
		drawTextBox(str, x, y, size, limit, Align.LEFT);
	}
	
	public static void drawTextBox(String str, double x, double y, double size, double limit, Align align) {
		drawTextBox(str, x, y, size, limit, align, false);
	}
	
	public static void drawTextBox(String str, double x, double y, double size, double limit, Align align, boolean mono) {
		GL11.glEnable(GL11.GL_BLEND);
		
		Vector2d vec = Font.font.simDrawWrapped(str, size, limit);
		double X0 = x, Y0 = y, MARGIN = Math.min(5, size * 0.3);
		
		if(align == Align.CENTER) {
			X0 -= vec.x / 2;
		} else if(align == Align.RIGHT) {
			X0 -= vec.x;
		}
		
		drawBox(X0, Y0, MARGIN * 2 + vec.x, MARGIN * 2 + vec.y, mono);
		GL11.glPushMatrix();
		GL11.glTranslated(0, 0, 1);
		Font.font.drawWrapped(str, X0 + MARGIN, Y0 + MARGIN, size, 0xffffff, limit);
		GL11.glPopMatrix();
	}
	
	public static void initNodeLinkButton(IWirelessUser target, Widget theButton) {
		initNodeLinkButton(target, theButton, false);
	}
	
	/**
	 * Add the callback to open the link node UI to the button.
	 */
	public static void initNodeLinkButton(IWirelessUser target, Widget theButton, boolean mono) {
		DrawTexture.get(theButton).texture = BTN_WIFI_N;
		
		if(theButton.getComponent("Tint") == null) {
			Tint tint = new Tint();
			tint.affectTexture = true;
			tint.idleColor.setColor4i(255, 255, 255, 180);
			tint.hoverColor.setColor4i(255, 255, 255, 255);
			theButton.addComponent(tint);
		}
		
		EnergyUISyncs.syncIsLinked((TileEntity) target, 
			Future.<Boolean>create((Boolean o) -> {
				if(o) {
					DrawTexture.get(theButton).texture = BTN_WIFI;
				}
			}));
		
		theButton.regEventHandler(new MouseDownHandler() {

			@Override
			public void handleEvent(Widget w, MouseDownEvent event) {
				Minecraft.getMinecraft().displayGuiScreen(new GuiLinkToNode(target, mono));
			}
			
		});
		
		theButton.regEventHandler(new FrameEventHandler() {

			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				if(event.hovering)
					drawTextBox(StatCollector.translateToLocal("ac.network.search"), event.mx + 10, event.my - 5, 10 / w.scale, 
							233333, Align.LEFT, mono);
			}
			
		});
	}
	
}
