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
package cn.academy.ability.meltdowner.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.meltdowner.entity.EntityMdBall;
import cn.liutils.template.client.render.entity.RenderIcon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderMdBall extends RenderIcon {

	public RenderMdBall() {
		super(null);
		setSize(0.5f);
		setHasLight(false);
		setBlend(0.8f);
	}
	
	@Override
	public void doRender(Entity ent, double x, double y,
			double z, float f, float g) {
		EntityPlayer clientPlayer = Minecraft.getMinecraft().thePlayer;
		EntityMdBall ball = (EntityMdBall) ent;
		if(ball.spawner == null)
			return;
		
		boolean firstPerson = 
				Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 
				&& clientPlayer.equals(ball.spawner);
		long time = Minecraft.getSystemTime();
		
		//position hack
		if(ball.doesFollow()) {
			x = ball.offx + (ball.spawner.posX - clientPlayer.posX);
			y = ball.offy - 1.6  + (ball.spawner.posY - clientPlayer.posY);
			z = ball.offz  + (ball.spawner.posZ - clientPlayer.posZ);
		}
		
		icon = ball.getTexture();
		this.alpha = 0.8 * ball.getAlpha();
		
		super.doRender(ent, x, y, z, f, g);
	}
	
}
