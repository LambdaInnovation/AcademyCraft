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
package cn.academy.misc.entity.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.draw.DrawObject;
import cn.liutils.api.draw.prop.AssignTexture;
import cn.liutils.api.draw.prop.DisableCullFace;
import cn.liutils.api.draw.tess.Rect;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.motion.CollisionCheck;
import cn.liutils.api.entityx.motion.GravityApply;
import cn.liutils.api.entityx.motion.LifeTime;
import cn.liutils.api.entityx.motion.VelocityUpdate;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Fragment effect spawned in client.
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity(clientOnly = true)
@RegEntity.HasRender
@SideOnly(Side.CLIENT)
public class EntitySibarnFrag extends EntityX {
	
	Vec3 axis;
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static RenderSF render;
	
	double spinSpeed;

	public EntitySibarnFrag(World world, double x, double y, double z, double mx, double my, double mz) {
		super(world);
		setSize(0.2f, 0.2f);
		setPosition(x, y, z);
		motionX = mx;
		motionY = my;
		motionZ = mz;
		addDaemonHandler(new LifeTime(this, 50));
		addDaemonHandler(new GravityApply(this, 0.03));
		addDaemonHandler(new CollisionCheck(this));
		addDaemonHandler(new VelocityUpdate(this));
		axis = Vec3.createVectorHelper(rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
		spinSpeed = GenericUtils.randIntv(1, 3);
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	@SideOnly(Side.CLIENT)
	public static class RenderSF extends Render {
		
		DrawObject drawer;
		
		public RenderSF() {
			drawer = new DrawObject();
			Rect rect;
			drawer.addHandler(rect = new Rect(.1, .1));
			drawer.addHandler(new AssignTexture(
				new ResourceLocation("academy:textures/entities/silbarn_frag.png")));
			//drawer.addHandler(DisableLight.instance());
			drawer.addHandler(DisableCullFace.instance());
			rect.setCentered();
		}

		@Override
		public void doRender(Entity ent, double x, double y, double z, float var8, float var9) {
			EntitySibarnFrag esf = (EntitySibarnFrag) ent;
			
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glRotated(Minecraft.getSystemTime() * esf.spinSpeed, 
				esf.axis.xCoord, esf.axis.yCoord, esf.axis.zCoord);
			GL11.glColor4d(1, 1, 1, ent.ticksExisted > 30 ? (1 - (ent.ticksExisted - 30) / 20.0) : 1.0);
			drawer.draw();
			GL11.glPopMatrix();
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity var1) {
			return null;
		}
		
	}

}
