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
package cn.academy.vanilla.electromaster.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.render.ray.RendererRayComposite;
import cn.academy.core.entity.EntityRayBase;
import cn.academy.vanilla.electromaster.client.effect.ArcFactory;
import cn.academy.vanilla.electromaster.client.effect.ArcFactory.Arc;
import cn.academy.vanilla.electromaster.client.effect.SubArcHandler;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util.client.ViewOptimize;
import cn.liutils.util.client.ViewOptimize.IAssociatePlayer;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.generic.VecUtils;
import cn.liutils.util.helper.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@RegEntity(clientOnly = true)
@SideOnly(Side.CLIENT)
@RegEntity.HasRender
public class EntityRailgunFX extends EntityRayBase {
	
	static final int ARC_SIZE = 15;
	
	@RegEntity.Render
	public static RailgunRender renderer;
	
	static Arc[] templates;
	static {
		ArcFactory factory = new ArcFactory();
		factory.widthShrink = 0.9;
		factory.maxOffset = 0.8;
		factory.passes = 3;
		factory.width = 0.3;
		factory.branchFactor = 0.7;
		
		templates = new Arc[ARC_SIZE];
		for(int i = 0; i < ARC_SIZE; ++i) {
			templates[i] = factory.generate(RandUtils.ranged(2, 3));
		}
	}
	
	SubArcHandler arcHandler = new SubArcHandler(templates);
	
	public EntityRailgunFX(EntityPlayer player) {
		super(player);
		new Motion3D(player, true).applyToEntity(this);
		
		this.life = 50;
		this.blendInTime = 150;
		this.widthShrinkTime = 800;
		this.widthWiggleRadius = 0.3;
		this.maxWiggleSpeed = 0.8;
		this.blendOutTime = 1000;
		this.length = 45.0;
		
		ignoreFrustumCheck = true;
		
		//Build the arc list
		{
			double cur = 1.0;
			double len = this.length;
			
			while(cur <= len) {
				float theta = RandUtils.rangef(0, MathUtils.PI_F * 2);
				double r = RandUtils.ranged(0.1, 0.25);
				Vec3 vec = VecUtils.vec(cur, r * MathHelper.sin(theta), r * MathHelper.cos(theta));
				vec.rotateAroundZ(rotationPitch * MathUtils.PI_F / 180);
				vec.rotateAroundY((270 - rotationYaw) * MathUtils.PI_F / 180);
				arcHandler.generateAt(vec);
				
				cur += RandUtils.ranged(1, 2);
			}
		}
	}
	
	@Override
	protected void onFirstUpdate() {
		super.onFirstUpdate();
		worldObj.playSound(posX, posY, posZ, "academy:em.railgun", 0.5f, 1.0f, false);
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		if(ticksExisted == 30)
			arcHandler.clear();
		
		arcHandler.tick();
	}
	
	public static class RailgunRender extends RendererRayComposite {
		
		Arc[] arcs;

		public RailgunRender() {
			super("railgun");
			glow.startFix = -0.3;
			glow.endFix = 0.3;
			glow.width = 1.1;
			
			cylinderIn.color.setColor4i(241, 240, 222, 200);
			cylinderIn.width = 0.09;
			
			cylinderOut.color.setColor4i(236, 170, 93, 60);
			cylinderOut.width = 0.13;
			
			ArcFactory factory = new ArcFactory();
			factory.widthShrink = 0.9;
			factory.maxOffset = 0.8;
			factory.passes = 3;
			factory.width = 0.3;
			factory.branchFactor = 0.7;
			
			arcs = new Arc[ARC_SIZE];
			for(int i = 0; i < ARC_SIZE; ++i) {
				arcs[i] = factory.generate(RandUtils.ranged(2, 3));
			}
		}
		
		@Override
		public void doRender(Entity ent, double x,
				double y, double z, float a, float b) {
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			ViewOptimize.fix((IAssociatePlayer) ent);
			
			EntityRailgunFX railgun = (EntityRailgunFX) ent;
			
			railgun.arcHandler.drawAll();
			
			GL11.glPopMatrix();
			
			super.doRender(ent, x, y, z, a, b);
		}
		
	}

}
