/**
 * 
 */
package cn.academy.ability.electro.entity.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.entity.RenderArcEff;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.util.misc.EntityPool;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityArcS extends EntityX {
	
	public final static int FRAME_RATE = 100; //0.1s per change
	
	public boolean show;
	public float size;
	
	public int texIndex;
	public long lastChangeTime;
	
	public final int roll = rand.nextInt(360);
	
	public ResourceLocation[] texs = ACClientProps.ANIM_SMALL_ARC;
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderArcEff render;
	
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
}
