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
package cn.academy.ability.electro.client.render.entity;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.electro.entity.fx.EntityArcS;
import cn.liutils.template.client.render.entity.RenderIcon;

/**
 * @author WeathFolD
 *
 */
public final class RenderArcEff extends RenderIcon {
	
	Random rand = new Random();

	public RenderArcEff() {
		super(null);
		this.setBlend(.8f);
	}
	
	@Override
	public void doRender(Entity ent, double par2, double par4,
			double par6, float par8, float par9) {
		if(((EntityArcS)ent).show)
			super.doRender(ent, par2, par4, par6, par8, par9);
	}

	@Override
	protected void postTranslate(Entity ent) {
		EntityArcS arc = (EntityArcS) ent;
		
		//GL11.glRotated(arc.rotOffset, 1, 1, 1);
		long time = Minecraft.getSystemTime();
		if(time - arc.lastChangeTime > arc.FRAME_RATE) {
			arc.lastChangeTime = time;
			arc.texIndex = rand.nextInt(arc.texs.length);
		}
		
		this.setSize(arc.size);
		this.icon = arc.texs[arc.texIndex % arc.texs.length];
	}
	
	@Override
	protected void firstTranslate(Entity ent) {
		EntityArcS arc = (EntityArcS) ent;
		GL11.glRotated(arc.roll, 0, 0, 1);
	}
	
}
