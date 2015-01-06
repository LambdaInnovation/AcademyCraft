/**
 * 
 */
package cn.academy.core.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.api.ctrl.Preset;
import cn.academy.api.ctrl.PresetManager;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.client.ACLangs;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.api.key.IKeyHandler;
import cn.liutils.api.key.LIKeyProcess;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.TextUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The preset selection GUI. Currently activated by C key.
 * Use Keyboard UP and DOWN to select, and ENTER to confirm.
 * May consider adding mouse-wheel and mouse-click action.
 * @author WeathFolD
 */
@RegistrationClass
@SideOnly(Side.CLIENT)
public class GuiPresetSelect extends AuxGui {

	@RegAuxGui
	public static final GuiPresetSelect instance = new GuiPresetSelect();
	
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
		this.addKeyHandler("up", Keyboard.KEY_UP, false, new IKeyHandler() {
			@Override
			public void onKeyDown(int keyCode, boolean tickEnd) {
				if(!tickEnd && curSelection > 0) curSelection--;
			}
			@Override public void onKeyUp(int keyCode, boolean tickEnd) {}
			@Override public void onKeyTick(int keyCode, boolean tickEnd) {}
		});
		
		addKeyHandler("down", Keyboard.KEY_DOWN, false, new IKeyHandler() {
			@Override
			public void onKeyDown(int keyCode, boolean tickEnd) {
				if(!tickEnd && curSelection < SELECTION_MAX - 1) curSelection++;
			}
			@Override public void onKeyUp(int keyCode, boolean tickEnd) {}
			@Override public void onKeyTick(int keyCode, boolean tickEnd) {}
		});
		
		addKeyHandler("confirm", Keyboard.KEY_RETURN, false, new IKeyHandler() {
			@Override
			public void onKeyDown(int keyCode, boolean tickEnd) {
				if(!tickEnd) {
					PresetManager.setCurrentPreset(curSelection);
					closeGui();
				}
			}
			@Override public void onKeyUp(int keyCode, boolean tickEnd) {}
			@Override public void onKeyTick(int keyCode, boolean tickEnd) {}
		});
	}
	
	public void openGui() {
		isOpen = true;
		curSelection = PresetManager.getCurrentPresetId();
		lastOpenTime = Minecraft.getSystemTime();
		System.out.println("Open");
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
				scale = 0.9F;
			GL11.glTranslatef(x0, y0, 0);
			GL11.glScalef(scale, scale, 1);
			GL11.glTranslatef(-x0, -y0, 0);
			
			double prog = Math.min(1.0, 
					(Minecraft.getSystemTime() - lastOpenTime) / (double)ANIM_TIME);
			drawSelectionMenu(w, h, prog);
			drawPresetInfo(w, h, prog);
			drawTag(w, h, prog);
		} GL11.glPopMatrix();
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private void drawTag(int w, int h, double prog) {
		float vw = 92.6F, vh = 13;
		float x0 = w / 2F, y0 = h / 2F - vh;
		float tx = 6, ty = -5;
		float color = 0.16F;
		GL11.glPushMatrix(); {
			//the mask
			rect(x0 + tx, y0 + ty, vw * prog, vh, 0.12F, 0.6F);
			//text
			GL11.glDepthFunc(GL11.GL_GEQUAL);
			GL11.glColor4d(1, 1, 1, prog);
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, "Preset Selection", x0 + tx + 5, y0 + ty + 1.7, 10);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
		} GL11.glPopMatrix();
	}
	
	private void drawPresetInfo(int w, int h, double prog) {
		float vw = 97.2F, vh = 34.9F;
		float x0 = w * 0.5F - vw, y0 = h * 0.5F - vh;
		float tx = -6, ty = -5;
		GL11.glPushMatrix(); {
			//Background&Mask
			GL11.glPushMatrix(); {
				GL11.glTranslated(vw * (1.0 - prog), 0, 0);
				rect(x0 + tx, y0 + ty, vw * prog, vh, 0.12F, 0.6F);
			} GL11.glPopMatrix();
			
			//Front Drawing
			float step = 23F;
			Preset preset = EventHandlerClient.getPresetManager().getPreset(curSelection);
			float cx = 0F;
			GL11.glDepthFunc(GL11.GL_GEQUAL);
			GL11.glColor4d(1, 1, 1, prog * 0.5);
			
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			AbilityData data = AbilityDataMain.getData(player);
			for(int i = 0; i < SELECTION_MAX; ++i) {
				//Text
				String name = LIKeyProcess.getKeyName(EventHandlerClient.getKeyId(i));
				float font_size = 10F;
				double len = TextUtils.getWidth(TextUtils.FONT_CONSOLAS_64, name, font_size);
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, name, 
						x0 + tx + cx + step / 2 - len / 2 + 3, y0 + ty + 4, font_size);
				
				//Ability Logo
				{
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					float rectSize = 14F;
					double rx = x0 + tx + cx + step / 2 - rectSize / 2 + 3,
						  ry = y0 + ty + 16.5;
					HudUtils.drawRectOutline(rx, ry, rectSize, rectSize, 2F);
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					//Find the logo, if any
					
					SkillBase skl = data.getSkill(preset.getSkillMapping(i));
					if(skl != null) {
						//Draw the logo
						ResourceLocation logo = skl.getLogo();
						if(logo != null) {
							RenderUtils.loadTexture(logo);
							HudUtils.drawRect(rx + 1, ry + 1, rectSize - 2, rectSize - 2);
						}
					}
					
				}
				
				cx += step;
			}
			GL11.glDepthFunc(GL11.GL_LEQUAL);
		} GL11.glPopMatrix();
	}
	
	private void drawSelectionMenu(int w, int h, double prog) {
		prog = Math.min(prog * 1.2, 1); //speedup
		float x0 = w * 0.5F, y0 = h * 0.5F;
		
		//BackMenu
		float tx = 6, ty = 5;
		float mw = 40, mh = 45.4F;
		float color = 0.16F;
		rect(x0 + tx, y0 + ty, mw, mh * prog, 0.12F, 0.6F);
		
		//select element
		GL11.glDepthFunc(GL11.GL_GEQUAL);
		float y_step = 11F;
		for(int i = 0; i < SELECTION_MAX; ++i) {
			float ny = y_step * i + 0.5F;
			if(curSelection == i) {
				rect(x0 + tx, y0 + ty + ny, mw, y_step, 0.6F, 0.6F);
			}
			Preset pr = EventHandlerClient.getPresetManager().getPreset(i);
			GL11.glColor4d(1, 1, 1, 0.1 + prog * 0.9); //Linear brightening effect
			GL11.glDisable(GL11.GL_CULL_FACE);
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, ACLangs.presetPrefix() + (i + 1), x0 + tx + 3, y0 + ty + ny + 2, 7);
			GL11.glEnable(GL11.GL_CULL_FACE);
		}
		GL11.glDepthFunc(GL11.GL_LEQUAL);
	}
	
	//Fast rect drawing routine (grayscale)
	private void rect(float x, float y, double width, double height, float lum, float alpha) {
		GL11.glColor4f(lum, lum, lum, alpha);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		HudUtils.drawModalRect(x, y, width, height);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glColor4f(1F, 1F, 1F, 1F);
	}
	
}
