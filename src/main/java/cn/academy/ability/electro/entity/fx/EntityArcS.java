/**
 * 
 */
package cn.academy.ability.electro.entity.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.template.client.render.entity.RenderIcon;
import cn.liutils.util.misc.EntityPool;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@SideOnly(Side.CLIENT)
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityArcS extends EntityX {
	
	static final ResourceLocation[] TEX = ACClientProps.ANIM_SMALL_ARC;
	
	final static int FRAME_RATE = 100; //0.1s per change
	
	public boolean show;
	public float size;
	
	int texIndex;
	long lastChangeTime;
	
	public final int roll = rand.nextInt(360);
	
	@RegEntity.Render
	public static ArcRender render;
	
	private static EntityPool<EntityArcS> pool = new EntityPool<EntityArcS>() {

		@Override
		public EntityArcS createEntity(World world) {
			return new EntityArcS(world);
		}

		@Override
		public void resetEntity(EntityArcS ent) {
			ent.reset();
		}
		
	};
	
	public static EntityArcS get(World world) {
		if(!pool.isActive()) {
			pool.activate();
		}
		return pool.getEntity(world);
	}

	private EntityArcS(World world) {
		super(world);
		reset();
		this.ignoreFrustumCheck = true;
	}

	public EntityArcS setSize(float s) {
		size = s;
		return this;
	}
	
	public void reset() {
		show = true;
		size = 0.2F;
		posX = posY = posZ = 0;
		this.setCurMotion(null);
		this.clearDaemonHandlers();
		ticksExisted = 0;
		lastChangeTime = Minecraft.getSystemTime();
		setSize(size, size);
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	public static final class ArcRender extends RenderIcon {

		public ArcRender() {
			super(null);
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
			if(time - arc.lastChangeTime > FRAME_RATE) {
				arc.lastChangeTime = time;
				arc.texIndex = arc.rand.nextInt(TEX.length);
			}
			
			this.icon = TEX[arc.texIndex];
		}
		
		@Override
		protected void firstTranslate(Entity ent) {
			EntityArcS arc = (EntityArcS) ent;
			GL11.glRotated(arc.roll, 0, 0, 1);
		}
		
	}
	
}
