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
package cn.academy.ability.client.ui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTTextureEnvCombine;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import cn.academy.ability.api.ctrl.ClientHandler;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.event.PresetSwitchEvent;
import cn.academy.core.client.ui.ACHud;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.ForcePreloadTexture;
import cn.annoreg.mc.RegInit;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.Transform.WidthAlign;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;
import cn.liutils.util.helper.Font.Align;
import cn.liutils.util.helper.GameTimer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * @author WeAthFolD
 */
@Registrant
@RegInit
@ForcePreloadTexture
public class CPBar extends Widget {
	
	static final float WIDTH = 964, HEIGHT = 147;
	
	static double sin41 = Math.sin(44.0 / 180 * Math.PI);
	
	static final float CP_BALANCE_SPEED = 2.0f, O_BALANCE_SPEED = 2.0f;
	
	static IConsumptionHintProvider chProvider;
	
	public static void init() {
		ACHud.instance.addElement(new CPBar(), () -> true);
	}
	
	public static void setHintProvider(IConsumptionHintProvider provider) {
		chProvider = provider;
	}
	
	public static ResourceLocation
		TEX_BACK_NORMAL = tex("back_normal"),
		TEX_BACK_OVERLOAD = tex("back_overload"),
		TEX_CP = tex("cp"),
		TEX_FRONT_OVERLOAD = tex("front_overload"),
		TEX_OVERLOADED = tex("overloaded"),
		TEX_OVERLOAD_HIGHLIGHT = tex("highlight_overload"),
		TEX_MASK = tex("mask");
	
	List<ProgColor> cpColors = new ArrayList(), overrideColors = new ArrayList();
	
	static boolean supportARB;
	static {
		ContextCapabilities contextcapabilities = GLContext.getCapabilities();
		supportARB = contextcapabilities.GL_ARB_multitexture;
	}
	
	public interface IConsumptionHintProvider {
		boolean alive();
		float getConsumption();
	}
	
	long presetChangeTime, lastPresetTime;
	
	long lastDrawTime;
	long showTime;
	
	float mAlpha; //Master alpha, used for blending in.
	
	float bufferedCP;
	float bufferedOverload;

	private CPBar() {
		transform.setSize(WIDTH, HEIGHT);
		transform.scale = 0.2f;
		transform.alignWidth = WidthAlign.RIGHT;
		transform.setPos(-12, 12);
		
		initEvents();
		
		cpColors.add(new ProgColor(0.0, new Color(0xfff06767)));
		cpColors.add(new ProgColor(0.35, new Color(0xffffae44)));
		cpColors.add(new ProgColor(1.0, new Color(0xffffffff)));
		
		overrideColors.add(new ProgColor(0.0, new Color(0x0Adfdfdf)));
		overrideColors.add(new ProgColor(0.55, new Color(0x23f0d49d)));
		overrideColors.add(new ProgColor(1.0, new Color(0x50f56464)));
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
	public void onSwitchPreset(PresetSwitchEvent event) {
		lastPresetTime = presetChangeTime;
		presetChangeTime = GameTimer.getTime();
	}
	
	private void initEvents() {
		regEventHandler(new FrameEventHandler() {
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				EntityPlayer player = Minecraft.getMinecraft().thePlayer;
				CPData cpData = CPData.get(player);
				
				// Calculate alpha
				long time = GameTimer.getTime();
				if(time - lastDrawTime > 300L) {
					showTime = time;
				}
				
				long deltaTime = Math.min(100L, time - lastDrawTime);
				lastDrawTime = time;
				
				final long BLENDIN_TIME = 200L;
				mAlpha = (time - showTime < BLENDIN_TIME) ? (float) (time - showTime) / BLENDIN_TIME : 1.0f;
				
				
				if(cpData.isActivated()) {
					/* Draw CPBar */ {
						float poverload = cpData.getOverload() / cpData.getMaxOverload();
						bufferedOverload = balance(bufferedOverload, poverload, deltaTime * 1E-3f * O_BALANCE_SPEED);
						
						float pcp = cpData.getCP() / cpData.getMaxCP();
						bufferedCP = balance(bufferedCP, pcp, deltaTime * 1E-3f * CP_BALANCE_SPEED);
						
						if(bufferedOverload < 1.0) {
							drawNormal(bufferedOverload);
						} else {
							if(supportARB) {
								drawOverload(bufferedOverload);
							} else {
								drawOverloadLegacy(bufferedOverload);
							}
						}
						
						if(chProvider != null && !chProvider.alive())
							chProvider = null;
						
						float estmCons = chProvider == null ? 0 : chProvider.getConsumption();
						
						if(estmCons != 0) {
							float ncp = Math.max(0, cpData.getCP() - estmCons);
							
							float oldAlpha = mAlpha;
							mAlpha *= 0.2f + 0.1f * (1 + Math.sin(time / 80.0f));
							
							drawCPBar(pcp);
							
							mAlpha = oldAlpha;
							
							drawCPBar(ncp / cpData.getMaxCP());
						} else {
							drawCPBar(bufferedCP);
						}
					}
					
					/* Draw Preset Hint */ {
						final long preset_wait = 2000L;
						if(time - presetChangeTime < preset_wait)
							drawPresetHint((double)(time - presetChangeTime) / preset_wait,
								time - lastPresetTime);
					}
					
					drawActivateKeyHint();
				}
				
				GL11.glColor4d(1, 1, 1, 1);
			}
		});
	}
	
	/**
	 * Draw the overload without ARB blending, when the machine does not support it.
	 */
	private void drawOverloadLegacy(float overload) {
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		
		//Draw plain background
		color4d(1, 1, 1, 0.8);
		RenderUtils.loadTexture(TEX_BACK_OVERLOAD);
		HudUtils.rect(WIDTH, HEIGHT);
		
		//Start drawing blend
		RenderUtils.loadTexture(TEX_FRONT_OVERLOAD);
		float uOffset = GameTimer.getTime() / 10000.0f * WIDTH;
		color4d(1, 1, 1, 0.8);
		
		final double x0 = 30, width2 = WIDTH - x0 - 20;
		HudUtils.rect(x0, 0, uOffset, 0, width2, HEIGHT, width2, HEIGHT);
		//End drawing blend
		
		//Draw Highlight
		color4d(1, 1, 1, 0.3 + 0.35 * (Math.sin(GameTimer.getTime() / 200.0) + 1));
		RenderUtils.loadTexture(TEX_BACK_OVERLOAD);
		HudUtils.rect(WIDTH, HEIGHT);
		
		GL11.glEnable(GL11.GL_ALPHA_TEST);
	}
	
	private void drawOverload(float overload) {
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		
		//Draw plain background
		color4d(1, 1, 1, 0.8);
		RenderUtils.loadTexture(TEX_BACK_OVERLOAD);
		HudUtils.rect(WIDTH, HEIGHT);
		
		//Start drawing blend
		RenderUtils.loadTexture(TEX_MASK);
		int maskID = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		RenderUtils.loadTexture(TEX_FRONT_OVERLOAD);
		int frontID = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		
		ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE2_ARB);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, frontID);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, EXTTextureEnvCombine.GL_COMBINE_EXT);
		GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_COMBINE_RGB_EXT, GL11.GL_REPLACE);
		
		ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE3_ARB);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, maskID);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_COMBINE_ALPHA_EXT, GL11.GL_REPLACE);
		
		double uOffset = (GameTimer.getTime() % 10000L) / 10000.0d;
		color4d(1, 1, 1, 0.8);
		GL11.glBegin(GL11.GL_QUADS);
			ARBMultitexture.glMultiTexCoord2dARB(ARBMultitexture.GL_TEXTURE2_ARB, 0.0f + uOffset, 1.0f);
			ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE3_ARB, 0.0f, 1.0f);  
			GL11.glVertex3d(0.0f, 0.0f, 0.0f);
		  
			ARBMultitexture.glMultiTexCoord2dARB(ARBMultitexture.GL_TEXTURE2_ARB, 0.0f + uOffset, 0.0f);
			ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE3_ARB, 0.0f, 0.0f);
			GL11.glVertex3d(0.0, HEIGHT * 1.0, 0.0);
		  
			ARBMultitexture.glMultiTexCoord2dARB(ARBMultitexture.GL_TEXTURE2_ARB, 1.0f + uOffset, 0.0f);
			ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE3_ARB, 1.0f, 0.0f); 
			GL11.glVertex3d(WIDTH * 1.0, HEIGHT * 1.0, 0.0);
		  
			ARBMultitexture.glMultiTexCoord2dARB(ARBMultitexture.GL_TEXTURE2_ARB, 1.0f + uOffset, 1.0f);
			ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE3_ARB, 1.0f, 1.0f);
			GL11.glVertex3d(WIDTH * 1.0, 0.0, 0.0);
		GL11.glEnd();
		
		//Restore texture states
		ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE2_ARB);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		
		ARBMultitexture.glActiveTextureARB(ARBMultitexture.GL_TEXTURE3_ARB);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		
		//End drawing blend
		
		//Draw Highlight
		color4d(1, 1, 1, 0.3 + 0.35 * (Math.sin(GameTimer.getTime() / 200.0) + 1));
		RenderUtils.loadTexture(TEX_OVERLOAD_HIGHLIGHT);
		HudUtils.rect(WIDTH, HEIGHT);
		
		GL11.glEnable(GL11.GL_ALPHA_TEST);
	}
	
	private void drawNormal(float overload) {
		RenderUtils.loadTexture(TEX_BACK_NORMAL);
		
		color4d(1, 1, 1, .8);
		HudUtils.rect(WIDTH, HEIGHT);
		
		//Overload progress
		final double X0 = 0, Y0 = 21, WIDTH = 943, HEIGHT = 104;
		
		autoLerp(overrideColors, overload);
		double len = overload * WIDTH;
		
		RenderUtils.loadTexture(TEX_MASK);
		subHud(X0 + WIDTH - len, Y0, len, HEIGHT);
	}
	
	private void drawCPBar(float prog) {
		RenderUtils.loadTexture(TEX_CP);
		
		//We need a cut-angle effect so this must be done manually
		
		autoLerp(cpColors, prog);
		
		prog = 0.16f + prog * 0.8f;
		
		final double OFF = 103 * sin41, X0 = 47, Y0 = 30, WIDTH = 883, HEIGHT = 84;
		Tessellator t = Tessellator.instance;
		double len = WIDTH * prog, len2 = len - OFF;
		
		GL11.glCullFace(GL11.GL_BACK);
		
		t.startDrawingQuads();
		addVertex(X0 + (WIDTH - len), Y0);
		addVertex(X0 + (WIDTH - len2), Y0 + HEIGHT);
		addVertex(X0 + WIDTH, Y0 + HEIGHT);
		addVertex(X0 + WIDTH, Y0);
		t.draw();
		
		GL11.glCullFace(GL11.GL_BACK);
	}
	
	
	
	final Color CRL_P_BACK = new Color().setColor4i(48, 48, 48, 160);
	final Color temp = new Color();
	
	private void drawPresetHint(double progress, long untilLast) {
		final double x0 = 580, y0 = 136;
		final double size = 52, step = size + 10;
		
		double x = x0, y = y0;
		
		int cur = PresetData.get(
			Minecraft.getMinecraft().thePlayer).getCurrentID();
		
		double alpha;
		if(untilLast > 3000 && progress < 0.2) {
			alpha = progress / 0.2;
		} else if(progress > 0.8) {
			alpha = (1 - progress) / 0.2;
		} else {
			alpha = 1;
		}
		alpha *= 0.75;
		
		for(int i = 0; i < 4; ++i) {
			CRL_P_BACK.a = alpha;
			CRL_P_BACK.bind();
			HudUtils.colorRect(x, y, size, size);
			
			temp.a = Math.max(0.05, alpha * 0.8);
			
			//TODO: This approach seems to be buggy in SOME machines :(
			Font.font.draw("§L" + (i + 1), x + 2 + size / 2, y + 5, 46, temp.asHexColor(), Align.CENTER);
			
			temp.bind();
			if(i == cur)
				HudUtils.drawRectOutline(x, y, size, size, 3);
			
			x += step;
		}
		
	}
	
	private void drawActivateKeyHint() {
		final double x0 = 850, y0 = 140;
		String str = ClientHandler.getActivateKeyHint();
		
		Font.font.draw(str, x0, y0, 50, 0xffffffff, Align.RIGHT);
	}
	
	private void color4d(double r, double g, double b, double a) {
		GL11.glColor4d(r, g, b, mAlpha * a);
	}
	
	private void subHud(double x, double y, double width, double height) {
		Tessellator t = Tessellator.instance;
		t.startDrawingQuads();
		addVertex(x, 		 y);
		addVertex(x, 		 y + height);
		addVertex(x + width, y + height);
		addVertex(x + width, y);
		t.draw();
	}
	
	private void addVertex(double x, double y) {
		double width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH),
		        height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
		Tessellator.instance.addVertexWithUV(x, y, -90, x / width, y / height);
	}
	
	private void lerpBindColor(Color a, Color b, double factor) {
		color4d(lerp(a.r, b.r, factor), lerp(a.g, b.g, factor), lerp(a.b, b.b, factor), lerp(a.a, b.a, factor));
	}
	
	private void autoLerp(List<ProgColor> list, double prog) {
		for(int i = 0; i < list.size(); ++i) {
			ProgColor cur = list.get(i);
			if(cur.prog >= prog) {
				if(i == 0) {
					list.get(i).color.bind();
				} else {
					ProgColor last = list.get(i - 1);
					lerpBindColor(last.color, cur.color, (prog - last.prog) / (cur.prog - last.prog));
				}
				return;
			}
		}
		throw new RuntimeException("bad progress: " + prog); //Should never reach here
	}
	
	private double lerp(double a, double b, double factor) {
		return a * (1 - factor) + b * factor;
	}
	
	private float balance(float from, float to, float max) {
		float delta = to - from;
		delta = Math.signum(delta) * Math.min(max, Math.abs(delta));
		
		return from + delta;
	}
	
	private static ResourceLocation tex(String name) {
		return new ResourceLocation("academy:textures/guis/cpbar/" + name + ".png");
	}
	
	private static class ProgColor {
		double prog;
		Color color;
		
		public ProgColor(double _p, Color _c) {
			prog = _p;
			color = _c;
		}
	}
	
}
