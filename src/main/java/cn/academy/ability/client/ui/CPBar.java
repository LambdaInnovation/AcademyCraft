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

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.ctrl.ClientHandler;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.event.PresetSwitchEvent;
import cn.academy.core.client.ACRenderingHelper;
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
import cn.liutils.util.client.shader.ShaderProgram;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;
import cn.liutils.util.helper.Font.Align;
import cn.liutils.util.helper.GameTimer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

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

	ResourceLocation overlayTexture;
	
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
				AbilityData aData = AbilityData.get(player);
				Category c = aData.getCategory();
				overlayTexture = c == null ? null : c.getOverlayIcon();
				
				boolean active = cpData.isActivated();
				
				// Calculate alpha
				long time = GameTimer.getTime();
				if(time - lastDrawTime > 300L) {
					showTime = time;
				}
				
				long deltaTime = Math.min(100L, time - lastDrawTime);
				
				final long BLENDIN_TIME = 200L;
				mAlpha = (time - showTime < BLENDIN_TIME) ? (float) (time - showTime) / BLENDIN_TIME :
					(active ? 1.0f : Math.max(0.0f, 1 - (time - lastDrawTime) / 200.0f));
				
				float poverload = mAlpha > 0 ? cpData.getOverload() / cpData.getMaxOverload() : 0;
				bufferedOverload = balance(bufferedOverload, poverload, deltaTime * 1E-3f * O_BALANCE_SPEED);
				
				float pcp = mAlpha > 0 ? cpData.getCP() / cpData.getMaxCP() : 0;
				bufferedCP = balance(bufferedCP, pcp, deltaTime * 1E-3f * CP_BALANCE_SPEED);
				
				if(mAlpha > 0) {
					/* Draw CPBar */ {
						if(bufferedOverload < 1) {
							drawNormal(bufferedOverload);
						} else {
							drawOverload(bufferedOverload);
						}
						
						if(chProvider != null && !chProvider.alive())
							chProvider = null;
						
						float estmCons = chProvider == null ? 0 : chProvider.getConsumption() * 
							(cpData.isOverloaded() ? cpData.OVERLOAD_CP_MUL : 1); // Takes account of overloading
						//System.out.println(chProvider + "/" + estmCons);
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
				
				if(active)
					lastDrawTime = time;
				
				GL11.glColor4d(1, 1, 1, 1);
			}
		});
	}
	
	private void drawOverload(float overload) {
		//Draw plain background
		color4d(1, 1, 1, 0.8);
		RenderUtils.loadTexture(TEX_BACK_OVERLOAD);
		HudUtils.rect(WIDTH, HEIGHT);
		
		// Draw back
		color4d(1, 1, 1, 1);
		shaderOverloaded.useProgram();
		shaderOverloaded.updateTexOffset((GameTimer.getTime() % 10000L) / 10000.0f);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		RenderUtils.loadTexture(TEX_MASK);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		RenderUtils.loadTexture(TEX_FRONT_OVERLOAD);
		
		final double x0 = 30, width2 = WIDTH - x0 - 20;
		HudUtils.rect(x0, 0, 0, 0, width2, HEIGHT, width2, HEIGHT);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		
		GL20.glUseProgram(0);
		
		// Highlight
		color4d(1, 1, 1, 0.3 + 0.35 * (Math.sin(GameTimer.getTime() / 200.0) + 1));
		RenderUtils.loadTexture(TEX_OVERLOAD_HIGHLIGHT);
		HudUtils.rect(WIDTH, HEIGHT);
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
		if(overlayTexture == null)
			return;
		
		//We need a cut-angle effect so this must be done manually
		autoLerp(cpColors, prog);
		
		prog = 0.16f + prog * 0.8f;
		
		final double OFF = 103 * sin41, X0 = 47, Y0 = 30, WIDTH = 883, HEIGHT = 84;
		Tessellator t = Tessellator.instance;
		double len = WIDTH * prog, len2 = len - OFF;
		
		shaderCPBar.useProgram();
		
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		RenderUtils.loadTexture(overlayTexture);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		RenderUtils.loadTexture(TEX_CP);
		
		t.startDrawingQuads();
		addVertex(X0 + (WIDTH - len), Y0);
		addVertex(X0 + (WIDTH - len2), Y0 + HEIGHT);
		addVertex(X0 + WIDTH, Y0 + HEIGHT);
		addVertex(X0 + WIDTH, Y0);
		t.draw();
		
		GL20.glUseProgram(0);
		
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + 4);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
	}
	
	final Color CRL_P_BACK = new Color().setColor4i(48, 48, 48, 160),
			CRL_P_FORE = new Color().setColor4i(255, 255, 255, 200);
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
			if(i == cur) {
				ACRenderingHelper.drawGlow(x, y, size, size, 5, CRL_P_FORE);
				//HudUtils.drawRectOutline(x, y, size, size, 3);
			}
			
			x += step;
		}
		
	}
	
	static final Color 
		CRL_KH_BACK = new Color().setColor4i(65, 65, 65, 70), 
		CRL_KH_GLOW = new Color().setColor4i(255, 255, 255, 40);
	
	private void drawActivateKeyHint() {
		String str = ClientHandler.getActivateKeyHint();
		
		if(str != null) {
			final double x0 = 500, y0 = 140, fsize = 44, MARGIN = 8;
			CRL_KH_BACK.bind();
			double len = Font.font.strLen(str, fsize);
			HudUtils.colorRect(x0 - MARGIN - len, y0 - MARGIN, len + MARGIN * 2, fsize + MARGIN * 2);
			ACRenderingHelper.drawGlow(x0 - MARGIN - len, y0 - MARGIN, len + MARGIN * 2, fsize + MARGIN * 2, 5, CRL_KH_GLOW);
			Font.font.draw(str, x0, y0, fsize, 0xa0ffffff, Align.RIGHT);
		}
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
	
	private static class ShaderOverloaded extends ShaderProgram {
		
		final int locTexOffset;
		
		private ShaderOverloaded() {
			this.linkShader(new ResourceLocation("liutils:shaders/simple.vert"), GL20.GL_VERTEX_SHADER);
			this.linkShader(new ResourceLocation("academy:shaders/cpbar_overload.frag"), GL20.GL_FRAGMENT_SHADER);
			this.compile();
			
			useProgram();
			GL20.glUniform1i(getUniformLocation("samplerTex"), 0);
			GL20.glUniform1i(getUniformLocation("samplerMask"), 4);
			GL20.glUseProgram(0);
			
			locTexOffset = getUniformLocation("texOffset");
		}
		
		public void updateTexOffset(float val) {
			GL20.glUniform1f(locTexOffset, val);
		}
		
	}
	
	private static class ShaderCPBar extends ShaderProgram {
		
		private ShaderCPBar() {
			this.linkShader(new ResourceLocation("liutils:shaders/simple.vert"), GL20.GL_VERTEX_SHADER);
			this.linkShader(new ResourceLocation("academy:shaders/cpbar_cp.frag"), GL20.GL_FRAGMENT_SHADER);
			this.compile();
			
			useProgram();
			GL20.glUniform1i(getUniformLocation("samplerTex"), 0);
			GL20.glUniform1i(getUniformLocation("samplerIcon"), 4);
			GL20.glUseProgram(0);
		}
		
	}
	
	static ShaderCPBar shaderCPBar = new ShaderCPBar();
	static ShaderOverloaded shaderOverloaded = new ShaderOverloaded();
	
}
