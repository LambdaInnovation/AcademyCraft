/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.Preset;
import cn.academy.api.ctrl.PresetManager;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.client.ACLangs;
import cn.academy.core.ctrl.EventHandlerClient;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.api.key.IKeyHandler;
import cn.liutils.api.key.LIKeyProcess;
import cn.liutils.api.register.Configurable;
import cn.liutils.registry.AttachKeyHandlerRegistry.RegAttachKeyHandler;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;
import cn.liutils.registry.ConfigurableRegistry.RegConfigurable;
import cn.liutils.util.ClientUtils;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The preset selection GUI. Currently activated by C key.
 * Use Keyboard UP and DOWN to select, and ENTER to confirm.
 * Also supports mouse-wheel and mouse-click action.
 * @author WeathFolD
 */
@RegistrationClass
@RegConfigurable
@SideOnly(Side.CLIENT)
public class GuiPresetSelect extends AuxGui {

	@RegAuxGui
	public static final GuiPresetSelect instance = new GuiPresetSelect();
	
	@Configurable(category = "Control", key = "keyPresetSelect", defValueInt = Keyboard.KEY_C)
	@RegAttachKeyHandler(clazz = KeyHandler.class)
	public static int KEY_ID_PRESET_SELECT;
	
	public static class KeyHandler implements IKeyHandler {
		@Override
		public void onKeyDown(int keyCode, boolean tickEnd) {
			if(tickEnd || !ClientUtils.isPlayerInGame() ||
				AbilityDataMain.getData(Minecraft.getMinecraft().thePlayer).getCategoryID() == 0) return;
			if(GuiPresetSelect.instance.isOpen())
				GuiPresetSelect.instance.closeGui();
			else if(ClientUtils.isPlayerInGame()) {
				GuiPresetSelect.instance.openGui();
			}
		}
		@Override public void onKeyUp(int keyCode, boolean tickEnd) {}
		@Override public void onKeyTick(int keyCode, boolean tickEnd) {}
	}
	
	//Constants
	private static final float ALPHA = 0.6F;
	private static final int 
		SELECTION_MAX = EventHandlerClient.MAX_KEYS,
		ANIM_TIME = 300;
	
	//States
	private boolean isOpen = false;
	private long lastOpenTime;
	private int curSelection;
	
	private GuiPresetSelect() {
		//Key Registration
		IKeyHandler up = new IKeyHandler() {
			@Override
			public void onKeyDown(int keyCode, boolean tickEnd) {
				if(curSelection > 0) curSelection--;
			}
			@Override public void onKeyUp(int keyCode, boolean tickEnd) {}
			@Override public void onKeyTick(int keyCode, boolean tickEnd) {}
		};
		this.addKeyHandler("up", Keyboard.KEY_UP, false, up);
		this.addKeyHandler("mwup", LIKeyProcess.MWHEELUP, false, up);
		
		IKeyHandler down = new IKeyHandler() {
			@Override
			public void onKeyDown(int keyCode, boolean tickEnd) {
				if(curSelection < SELECTION_MAX - 1) curSelection++;
			}
			@Override public void onKeyUp(int keyCode, boolean tickEnd) {}
			@Override public void onKeyTick(int keyCode, boolean tickEnd) {}
		};
		addKeyHandler("down", Keyboard.KEY_DOWN, false, down);
		addKeyHandler("mwdown", LIKeyProcess.MWHEELDOWN, false, down);
		
		IKeyHandler confirm = new IKeyHandler() {
			@Override
			public void onKeyDown(int keyCode, boolean tickEnd) {
				PresetManager.setCurrentPreset(curSelection);
				closeGui();
			}
			@Override public void onKeyUp(int keyCode, boolean tickEnd) {}
			@Override public void onKeyTick(int keyCode, boolean tickEnd) {}
		};
		addKeyHandler("confirm", Keyboard.KEY_RETURN, false, confirm);
		addKeyHandler("mconfirm", LIKeyProcess.MOUSE_LEFT, false, confirm);
	}
	
	public void openGui() {
		isOpen = true;
		curSelection = PresetManager.getCurrentPresetId();
		lastOpenTime = Minecraft.getSystemTime();
	}
	
	public void closeGui() {
		isOpen = false;
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public void draw(ScaledResolution sr) {
		int h = sr.getScaledHeight(), w = sr.getScaledWidth();
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glPushMatrix(); {
			float x0 = w / 2F, 
				y0 = h / 2F,
				scale = 0.32F;
			GL11.glTranslated(0, -5, 0);
			GL11.glTranslatef(x0, y0, 0);
			GL11.glScalef(scale, scale, 1);
			
			HudUtils.setTextureResolution(768, 512);
			RenderUtils.loadTexture(ACClientProps.TEX_GUI_PRESET);
			long dt = Minecraft.getSystemTime() - lastOpenTime;
			drawSelectionMenu(w, h, dt);
			drawPresetInfo(w, h, dt);
			drawTag(w, h, dt);
		} GL11.glPopMatrix();
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private void drawTag(int w, int h, long dt) {
		double dx = 0;
		double mAlpha;
		mAlpha = Math.min(1.0, dt / 300d);
		final double delta = 30;
		if(dt < 200) {
			dx = delta;
		} else if(dt < 400) {
			dx = -(delta / 200) * (dt - 200) + delta;
		} else {
			dt = 0;
		}
		GL11.glPushMatrix();
		GL11.glTranslated(dx, 0, 0);
		GL11.glColor4d(1, 1, 1, mAlpha);
		rect(1, -28, 399, 87, 162, 28);
		GL11.glColor4d(.2, .2, .2, mAlpha * .8);
		drawText(ACLangs.presetSelect(), 15, -25, 17);
		GL11.glPopMatrix();
	}
	
	private void drawSelectionMenu(int w, int h, long dt) {
		double ratio = dt < 500 ? 0 : (dt < 600 ? (dt - 500) / 100d : 1.0);
		double mAlpha = dt < 700 ? 0 : Math.min(1.0, (dt - 700) / 300d);
		double textAlpha = dt < 850 ? 0 : Math.min(1.0, (dt - 850) / 300d);
		
		GL11.glColor4d(1, 1, 1, 1);
		GL11.glPushMatrix();
		GL11.glTranslated(18, 0, 0);
		rect(0, 0, 419, 115, 6, 22 * ratio);
		
		GL11.glColor4d(1, 1, 1, mAlpha);
		rect(0, 22, 419, 137, 118, 127);
		
		//draw each text
		final double y_step = 32;
		double y0 = 22;
		GL11.glColor4d(0.3, 0.3, 0.3, mAlpha);
		for(int i = 0; i < 4; ++i) {
			if(curSelection == i) {
				rect(-6, y0, 413, 233, 6, 31);
				GL11.glColor4d(0, 0, 0, mAlpha * 0.2);
				HudUtils.drawModalRect(0, y0, 118, 31);
			}
			
			GL11.glColor4d(0.3, 0.3, 0.3, mAlpha);
			drawText(ACLangs.presetPrefix() + (i + 1), 10, y0 + 8, 15);
			RenderUtils.loadTexture(ACClientProps.TEX_GUI_PRESET);
			y0 += y_step;
		}
		
		GL11.glPopMatrix();
	}
	
	
	final int[][] posArray = {{7, 7}, {83, 7}, {249, 40}, {325, 40}};
	private void drawPresetInfo(int w, int h, long dt) {
		double mAlpha = Math.min(1.0, dt / 300d);
		GL11.glColor4d(1, 1, 1, mAlpha);
		rect(-399, -115, 0, 0, 399, 115);
		Preset p = PresetManager.getPreset(curSelection);
		AbilityData data = AbilityDataMain.getData(Minecraft.getMinecraft().thePlayer);
		GL11.glColor4d(1, 1, 1, .35);
		for(int i = 0; i < 4; i++) {
			int n = p.getSkillMapping(i);
			if(n != 0) {
				SkillBase sb = data.getSkill(p.getSkillMapping(i));
				ResourceLocation logo = sb.getLogo();
				if(logo != null) {
					RenderUtils.loadTexture(logo);
					HudUtils.drawRect(posArray[i][0] - 399, posArray[i][1] - 115, 69, 69);
				}
			}
		}
		RenderUtils.loadTexture(ACClientProps.TEX_GUI_PRESET);
	}
	
	@Override
	protected boolean overrideMouse() {
		return true;
	}
	
	/**
	 * TexW == RealW, TexH == RealH
	 */
	private void rect(double x, double y, double u, double v, double w, double h) {
		HudUtils.drawRect(x, y, u, v, w, h, w, h);
	}
	
	private void drawText(String text, double x, double y, float size) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size);
	}
	
	private void drawText(String text, double x, double y, float size, Align align) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size, align);
	}
}
