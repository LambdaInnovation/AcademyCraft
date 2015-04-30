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
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.EXTTextureEnvCombine;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.ForcePreloadTexture;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.utils.Color;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;

/**
 * TODO May crash running on machines that don't have ARB extention. Need wrapping?
 * @author WeAthFolD
 */
@RegistrationClass
@ForcePreloadTexture
public class CPBar extends Widget {
	
	static final float WIDTH = 964, HEIGHT = 147;
	
	static double sin41 = Math.sin(42.0 / 180 * Math.PI), tan41 = Math.tan(42.0 / 180 * Math.PI);
	
	public static ResourceLocation
		TEX_BACK_NORMAL = tex("back_normal"),
		TEX_BACK_OVERLOAD = tex("back_overload"),
		TEX_CP = tex("cp"),
		TEX_FRONT_OVERLOAD = tex("front_overload"),
		TEX_OVERLOADED = tex("overloaded"),
		TEX_OVERLOAD_HIGHLIGHT = tex("highlight_overload"),
		TEX_MASK = tex("mask");
	
	List<ProgColor> cpColors = new ArrayList(), overrideColors = new ArrayList();

	public CPBar() {
		transform.setSize(WIDTH, HEIGHT);
		transform.scale = 0.24f;
		
		initEvents();
		
		cpColors.add(new ProgColor(0.0, new Color(0xfffb2a2a)));
		cpColors.add(new ProgColor(0.35, new Color(0xffffae44)));
		cpColors.add(new ProgColor(1.0, new Color(0xffffffff)));
		
		overrideColors.add(new ProgColor(0.0, new Color(0x05dfdfdf)));
		overrideColors.add(new ProgColor(0.75, new Color(0x18f0d49d)));
		overrideColors.add(new ProgColor(1.0, new Color(0x18f56464)));
	}
	
	private void initEvents() {
		regEventHandler(new FrameEventHandler() {
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				double test = (Math.sin(Minecraft.getSystemTime() / 2000.0) + 1) * 0.5;
				
				double override = test * 2;
				if(override < 1.0) {
					drawNormal(override);
				} else {
					drawOverload(override);
				}
				
				drawCPBar(0.5);
			}
		});
	}
	
	private void drawOverload(double override) {
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		
		//Draw plain background
		GL11.glColor4d(1, 1, 1, 0.8);
		HudUtils.drawRect(TEX_BACK_OVERLOAD, WIDTH, HEIGHT);
		
		//Start drawing blend
		RenderUtils.loadTexture(TEX_MASK);
		int maskID = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		RenderUtils.loadTexture(TEX_FRONT_OVERLOAD);
		int frontID = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		
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
		GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, EXTTextureEnvCombine.GL_COMBINE_ALPHA_EXT);
		GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_COMBINE_ALPHA_EXT, GL11.GL_REPLACE);
		
		float uOffset = Minecraft.getSystemTime() / 10000.0f;
		GL11.glColor4d(1, 1, 1, 0.8);
		GL11.glBegin(GL11.GL_QUADS);
			ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE2_ARB, 0.0f + uOffset, 1.0f);
			ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE3_ARB, 0.0f, 1.0f);  
			GL11.glVertex3d(0.0f, 0.0f, 0.0f);
		  
			ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE2_ARB, 0.0f + uOffset, 0.0f);
			ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE3_ARB, 0.0f, 0.0f);
			GL11.glVertex3d(0.0, HEIGHT * 1.0, 0.0);
		  
			ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE2_ARB, 1.0f + uOffset, 0.0f);
			ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE3_ARB, 1.0f, 0.0f); 
			GL11.glVertex3d(WIDTH * 1.0, HEIGHT * 1.0, 0.0);
		  
			ARBMultitexture.glMultiTexCoord2fARB(ARBMultitexture.GL_TEXTURE2_ARB, 1.0f + uOffset, 1.0f);
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
		
		GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_COMBINE_RGB_EXT, GL11.GL_INCR);
		GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, EXTTextureEnvCombine.GL_COMBINE_ALPHA_EXT, GL11.GL_INCR);
		//End drawing blend
		
		//Draw Highlight
		GL11.glColor4d(1, 1, 1, 0.3 + 0.35 * (Math.sin(Minecraft.getSystemTime() / 200.0) + 1));
		HudUtils.drawRect(TEX_OVERLOAD_HIGHLIGHT, WIDTH, HEIGHT);
		
		GL11.glEnable(GL11.GL_ALPHA_TEST);
	}
	
	private void drawNormal(double override) {
		HudUtils.drawRect(TEX_BACK_NORMAL, WIDTH, HEIGHT);
		
		//Overload progress
		final double X0 = 0, Y0 = 21, WIDTH = 943, HEIGHT = 104;
		
		autoLerp(overrideColors, override);
		double len = override * WIDTH;
		HudUtils.drawModalRect(X0 + WIDTH - len, Y0, len, HEIGHT);
	}
	
	private void drawCPBar(double prog) {
		RenderUtils.loadTexture(TEX_CP);
		
		//We need a cut-angle effect so this must be done manually
		sin41 = Math.sin(44 / 180.0 * Math.PI);
		
		autoLerp(cpColors, prog);
		
		prog = 0.16 + prog * 0.84; //Keep the largest one.
		
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
	
	private void addVertex(double x, double y) {
		double width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH),
		        height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
		Tessellator.instance.addVertexWithUV(x, y, -90, x / width, y / height);
	}
	
	private void lerpBindColor(Color a, Color b, double factor) {
		GL11.glColor4d(lerp(a.r, b.r, factor), lerp(a.g, b.g, factor), lerp(a.b, b.b, factor), lerp(a.a, b.a, factor));
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
		throw new RuntimeException(); //Should never reach here
	}
	
	private double lerp(double a, double b, double factor) {
		return a * (1 - factor) + b * factor;
	}
	
	private static ResourceLocation tex(String name) {
		return new ResourceLocation("academy:textures/cpbar/" + name + ".png");
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
