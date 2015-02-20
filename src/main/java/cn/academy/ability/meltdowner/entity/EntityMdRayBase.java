/**
 * 
 */
package cn.academy.ability.meltdowner.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.misc.client.render.RendererRayBlended;
import cn.academy.misc.entity.EntityRay;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public abstract class EntityMdRayBase extends EntityRay {
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static RayRender render;

	public EntityMdRayBase(EntityLivingBase _spawner) {
		super(_spawner);
		doAttack();
	}
	
	public EntityMdRayBase(EntityPlayer _spawner, EntityMdBall ball) {
		super(_spawner.worldObj);
		MovingObjectPosition mop = GenericUtils.tracePlayer(_spawner, 20.0);
		double dist = mop == null ? 20.0 : 
			mop.hitVec.distanceTo
			(worldObj.getWorldVec3Pool()
			.getVecFromPool(ball.posX, ball.posY, ball.posZ));
		Motion3D mo = new Motion3D(_spawner, true).move(dist);
		this.setHeading(mo.posX - ball.posX, mo.posY - ball.posY, mo.posZ - ball.posZ, 1.0);
		this.rayLength = (float) dist;
		this.setPosition(ball.posX, ball.posY, ball.posZ);
		
		doAttack();
	}
	
	private void doAttack() {
		MovingObjectPosition mop = this.performTrace();
		if(mop != null) {
			handleCollision(mop);
		}
	}
	
	protected abstract void handleCollision(MovingObjectPosition mop);

	public EntityMdRayBase(World world) {
		super(world);
	}
	
	public boolean doesFollowSpawner() {
		return false;
	}
	
	@SideOnly(Side.CLIENT)
	public static class RayRender extends RendererRayBlended {

		public RayRender() {
			super(new ResourceLocation("academy:textures/effects/mdray.png"),
				  new ResourceLocation("academy:textures/effects/mdray_begin.png"),
				 0.666666);
			this.setWidthFp(0.2);
			this.setWidthTp(0.4);
			this.setAlpha(0.6);
		}
		
	}

}
