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

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.test.arc.ArcFactory;
import cn.academy.test.arc.ArcFactory.Arc;
import cn.academy.vanilla.electromaster.client.renderer.SubArcHandler;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.entityx.EntityAdvanced;
import cn.liutils.util.generic.DebugUtils;
import cn.liutils.util.generic.VecUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Spawn a surround arc effect around the specific entity or block.
 * @author WeAthFolD
 */
@Registrant
@SideOnly(Side.CLIENT)
@RegEntity
@RegEntity.HasRender
public class EntitySurroundArc extends EntityAdvanced {
	
	static final int TEMPLATES = 15;
	
	static {
		ArcFactory factory = new ArcFactory();
		factory.widthShrink = 0.9;
		factory.maxOffset = 0.8;
		factory.passes = 3;
		factory.width = 0.2;
		factory.branchFactor = 0.7;
		
		ArcType.THIN.templates = factory.generateList(10, 1.5, 2);
		
		factory.width = 0.3;
		ArcType.NORMAL.templates = factory.generateList(10, 2, 3);
		
		factory.passes = 4;
		factory.width = 0.4;
		factory.maxOffset = 1.1;
		ArcType.BOLD.templates = factory.generateList(10, 3, 4);
	}
	
	public enum ArcType {
		THIN(5), NORMAL(5), BOLD(5);
		
		public Arc[] templates;
		public int count;
		
		ArcType(int _count) {
			count = _count;
		}
	}
	
	@RegEntity.Render
	public static Renderer renderer;
	
	private ArcType arcType = ArcType.NORMAL;
	private final PosObject pos;
	private double arcRange;

	SubArcHandler arcHandler;
	
	public EntitySurroundArc(Entity follow) {
		super(follow.worldObj);
		pos = new EntityPos(follow);
		arcRange = follow.height / 1.8;
	}
	
	public EntitySurroundArc(World world, double x, double y, double z, double _arcRange) {
		super(world);
		pos = new ConstPos(x, y, z);
		arcRange = _arcRange;
	}
	
	public EntitySurroundArc setArcType(ArcType type) {
		arcType = type;
		return this;
	}
	
	@Override
	public void entityInit() {
		ignoreFrustumCheck = true;
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	@Override
	public void onFirstUpdate() {
		// Create the arcs!
		System.out.println("???");
		arcHandler = new SubArcHandler(arcType.templates);
		
		for(int i = 0; i < arcType.count; ++i) {
			double yaw = rand.nextDouble() * Math.PI * 2;
			double pitch = rand.nextDouble() * Math.PI;
			
			double y = Math.sin(pitch),
				zz = Math.sqrt(1 - y * y),
				x = zz * Math.sin(yaw),
				z = zz * Math.cos(yaw);
			
			System.out.println(DebugUtils.formatArray(x, y, z, arcRange));
			arcHandler.generateAt(VecUtils.vec(arcRange * x, arcRange * y, arcRange * z));
		}
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		arcHandler.tick();
		Vec3 vec = pos.getPos();
		setPosition(vec.xCoord, vec.yCoord, vec.zCoord);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		setDead();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {}
	
	private interface PosObject {
		Vec3 getPos();
	}
	
	public static class Renderer extends Render {

		@Override
		public void doRender(Entity entity, double x,
				double y, double z, float a,
				float b) {
			GL11.glPushMatrix();
			
			GL11.glTranslated(x, y, z);
			EntitySurroundArc esa = (EntitySurroundArc) entity;
			if(esa.arcHandler != null) {
				esa.arcHandler.drawAll();
			}
			
			GL11.glPopMatrix();
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity entity) {
			return null;
		}
		
	}
	
	private class EntityPos implements PosObject {
		
		final Entity entity;
		
		public EntityPos(Entity e) {
			entity = e;
		}
		
		@Override
		public Vec3 getPos() {
			return VecUtils.vec(entity.posX, entity.posY, entity.posZ);
		}
	}
	
	private class ConstPos implements PosObject {
		
		final Vec3 vec;
		
		public ConstPos(double x, double y, double z) {
			vec = VecUtils.vec(x, y, z);
		}
		
		@Override
		public Vec3 getPos() {
			return vec;
		}
	}

}
