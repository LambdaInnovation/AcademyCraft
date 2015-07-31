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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.client.Resources;
import cn.annoreg.core.Registrant;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.GameTimer;

/**
 * @author WeAthFolD
 */
@Registrant
@RegAuxGui
public class BackgroundMask extends AuxGui {
	
	final ResourceLocation MASK = Resources.getTexture("effects/screen_mask");
	
	final Color CRL_OVERRIDE = new Color().setColor4i(208, 20, 20, 170);
	
	static final double CHANGE_PER_SEC = 1;
	
	double r, g, b, a;
	
	long lastFrame;

	@Override
	public boolean isForeground() {
		return false;
	}

	@Override
	public void draw(ScaledResolution sr) {
		long time = GameTimer.getTime();
		
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		AbilityData aData = AbilityData.get(player);
		CPData cpData = CPData.get(player);
		
		double cr, cg, cb, ca;
		
		Color color = null;
		if(cpData.isOverloaded()) {
			color = CRL_OVERRIDE;
		} else {
			Category cat = aData.getCategory();
			if(cat != null && cpData.isActivated()) color = cat.getColorStyle();
		}
		
		if(color == null) {
			cr = r;
			cg = g;
			cb = b;
			ca = 0;
		} else {
			cr = color.r;
			cg = color.g;
			cb = color.b;
			ca = color.a;
		}
		
		if(ca != 0 || a != 0) {
			r = balanceTo(r, cr, time);
			g = balanceTo(g, cg, time);
			b = balanceTo(b, cb, time);
			a = balanceTo(a, ca, time);
			
			GL11.glColor4d(r, g, b, a);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			RenderUtils.loadTexture(MASK);
			HudUtils.rect(0, 0, sr.getScaledWidth_double(), sr.getScaledHeight_double());
			GL11.glColor4f(1, 1, 1, 1);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
		} else {
			r = cr;
			g = cg;
			b = cb;
		}
		
		lastFrame = time;
	}

	private double balanceTo(double from, double to, long time) {
		double delta = to - from;
		long dt = lastFrame == 0 ? 0 : time - lastFrame;
		delta = Math.signum(delta) * Math.min(Math.abs(delta), dt / 1000.0 * CHANGE_PER_SEC);
		return from + delta;
	}
	
}
