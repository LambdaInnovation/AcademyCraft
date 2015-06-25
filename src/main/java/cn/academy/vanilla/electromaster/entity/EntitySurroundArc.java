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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.IPointFactory;
import cn.academy.core.client.render.CubePointFactory;
import cn.academy.vanilla.electromaster.client.renderer.ArcFactory;
import cn.academy.vanilla.electromaster.client.renderer.SubArcHandler;
import cn.academy.vanilla.electromaster.client.renderer.ArcFactory.Arc;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.entityx.EntityAdvanced;
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
		ArcType.NORMAL.templates = factory.generateList(10, 3, 4);
		
		factory.passes = 3;
		factory.width = 0.35;
		factory.maxOffset =	1.2;
		factory.branchFactor = 0.45;
		ArcType.BOLD.templates = factory.generateList(10, 3.5, 4.5);
	}
	
	public enum ArcType {
		THIN(4), NORMAL(6), BOLD(5);
		
		public Arc[] templates;
		public int count;
		
		ArcType(int _count) {
			count = _count;
		}
	}
	
	@RegEntity.Render
	public static Renderer renderer;
	
	private ArcType arcType = ArcType.BOLD;
	private final PosObject pos;
	
	public int life = 100;

	SubArcHandler arcHandler;
	
	IPointFactory pointFactory;
	
	public EntitySurroundArc(Entity follow) {
		this(follow, 1.3);
	}
	
	public EntitySurroundArc(Entity follow, double sizeMultiplyer) {
		super(follow.worldObj);
		pos = new EntityPos(follow);
		pointFactory = new CubePointFactory(
			follow.width * sizeMultiplyer, 
			follow.height * sizeMultiplyer, 
			follow.width * sizeMultiplyer).setCentered(true);
	}
	
	public EntitySurroundArc(World world, double x, double y, double z, double wl, double h) {
		super(world);
		pos = new ConstPos(x, y, z);
		pointFactory = new CubePointFactory(wl, h, wl).setCentered(true);
	}
	
	public EntitySurroundArc setArcType(ArcType type) {
		arcType = type;
		return this;
	}
	
	public EntitySurroundArc setLife(int life) {
		this.life = life;
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
		arcHandler = new SubArcHandler(arcType.templates);
		arcHandler.frameRate = 0.6;
		arcHandler.switchRate = 0.7;
		
		doGenerate();
	}
	
	private void doGenerate() {
		for(int i = 0; i < arcType.count; ++i) {
			double yaw = rand.nextDouble() * Math.PI * 2;
			double pitch = rand.nextDouble() * Math.PI;
			
			double y = Math.sin(pitch),
				zz = Math.sqrt(1 - y * y),
				x = zz * Math.sin(yaw),
				z = zz * Math.cos(yaw);
			
			arcHandler.generateAt(pointFactory.next());
		}
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if(arcHandler.isEmpty())
			doGenerate();
		
		arcHandler.tick();
		
		pos.tick();
		setPosition(pos.x, pos.y, pos.z);
		rotationYaw = pos.yaw;
		rotationPitch = pos.pitch;
		
		if(ticksExisted == life)
			setDead();
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		setDead();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {}
	
	private abstract class PosObject {
		double x, y, z;
		float yaw, pitch;
		
		void tick() {}
	}
	
	public static class Renderer extends Render {

		@Override
		public void doRender(Entity entity, double x,
				double y, double z, float a,
				float b) {
			EntitySurroundArc esa = (EntitySurroundArc) entity;
			
			if(esa.arcHandler != null) {
				GL11.glPushMatrix();
				
				GL11.glTranslated(x, y, z);
				
				GL11.glRotatef(-esa.rotationYaw, 0, 1, 0);
				esa.arcHandler.drawAll();
				
				GL11.glPopMatrix();
			}
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity entity) {
			return null;
		}
		
	}
	
	private class EntityPos extends PosObject {
		
		final Entity entity;
		final boolean isPlayer;
		
		public EntityPos(Entity e) {
			entity = e;
			isPlayer = e instanceof EntityPlayer;
		}
		
		@Override
		void tick() {
			x = entity.posX;
			y = isPlayer ? entity.posY - 1.6 : entity.posY;
			z = entity.posZ;
			yaw = entity instanceof EntityLivingBase ? ((EntityLivingBase)entity).rotationYawHead : entity.rotationYaw;
			pitch = entity.rotationPitch;
		}
	}
	
	private class ConstPos extends PosObject {
		
		public ConstPos(double _x, double _y, double _z) {
			x = _x;
			y = _y;
			z = _z;
		}
	}

}
