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
package cn.academy.effect;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.motion.VelocityUpdate;
import cn.liutils.cgui.utils.Color;
import cn.liutils.template.client.render.entity.RenderIcon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegistrationClass
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public final class Particle extends EntityX {
	
	@RegEntity.Render
	public static ParticleRender renderer;
	
	public static ResourceLocation DEFAULT_TEXTURE = new ResourceLocation("academy:textures/effects/circle.png");
	public ResourceLocation texture = DEFAULT_TEXTURE;
	public Color color = Color.WHITE;
	public int lifeTime = 30;
	public float size = 0.5f;
	public boolean needLight = false;
	
	//public boolean collide;

	public Particle(World world) {
		super(world);
	}
	
	public static Particle createTemplate() {
		return new Particle(null);
	}
	
	public void fromTemplate(Particle template) {
		texture = template.texture;
		color = template.color;
		lifeTime = template.lifeTime;
		size = template.size;
		needLight = template.needLight;
		setSize(size, size);
	}
	
	public void init(Vec3 position, Vec3 velocity) {
		this.clearDaemonHandlers();
		this.ticksExisted = 0;
		this.setPosition(position.xCoord, position.yCoord, position.zCoord);
		this.motionX = velocity.xCoord;
		this.motionY = velocity.yCoord;
		this.motionZ = velocity.zCoord;
		
		this.addDaemonHandler(new VelocityUpdate(this));
		this.execAfter(lifeTime, new EntityCallback() {
			@Override
			public void execute(EntityX ent) {
				ent.setDead();
			}
		});
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	public static class ParticleRender extends RenderIcon {

		public ParticleRender() {
			super(null);
		}
		
		@Override
		public void doRender(Entity par1Entity, double par2, double par4,
				double par6, float par8, float par9) {
			Particle p = (Particle) par1Entity;
			this.setColorRGBA(p.color.r, p.color.g, p.color.b, p.color.a);
			this.setSize(p.size);
			this.icon = p.texture;
			this.alpha = Math.max(0, 1 - (double) p.ticksExisted / p.lifeTime);
			this.hasLight = p.needLight;
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			super.doRender(par1Entity, par2, par4, par6, par8, par9);
			GL11.glEnable(GL11.GL_ALPHA_TEST);
		}
		
	}

}
