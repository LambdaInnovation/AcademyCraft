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
public abstract class EntityMdRayBase extends EntityRay {
	
	private boolean attacked = false;

	public EntityMdRayBase(EntityPlayer _spawner) {
		super(_spawner);
	}
	
	public EntityMdRayBase(EntityPlayer _spawner, EntityMdBall ball) {
		super(_spawner);
		resetHeading(ball);
	}
	
	public EntityMdRayBase(World world) {
		super(world);
	}
	
	public void resetHeading(EntityMdBall ball) {
		MovingObjectPosition mop = GenericUtils.tracePlayer(getSpawner(), 20.0);
		double dist = mop == null ? 20.0 : 
			mop.hitVec.distanceTo
			(worldObj.getWorldVec3Pool()
			.getVecFromPool(ball.posX, ball.posY, ball.posZ));
		Motion3D mo = new Motion3D(getSpawner(), true).move(dist);
		double tox = ball.posX, toy = ball.posY + 0.1, toz = ball.posZ;
		this.setHeading(mo.posX - tox, mo.posY - toy, mo.posZ - toz, 1.0);
		this.rayLength = (float) dist;
		this.setPosition(tox, toy, toz);
	}
	
	private void doAttack() {
		MovingObjectPosition mop = this.performTrace();
		if(mop != null) {
			handleCollision(mop);
		}
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if(attackOnSpawn() && !worldObj.isRemote && !attacked) {
			attacked = true;
			doAttack();
		}
	}
	
	public float getDisplayRayLen() { //This enables comlicated display tricks on ray len.
		return Math.min(5f * ticksExisted, rayLength);
	}
	
	protected abstract void handleCollision(MovingObjectPosition mop);
	
	protected abstract ResourceLocation[] getTexData();
	
	@Override
	public boolean doesFollowSpawner() {
		return false;
	}
	
	@Override
	protected float getDefaultRayLen() {
		return 40.0f;
	}
	
	protected boolean attackOnSpawn() { return true; }
	
	@SideOnly(Side.CLIENT)
	public static class RayRender <T extends EntityMdRayBase> extends RendererRayBlended<T> {

		public RayRender() {
			super(null,
				  null,
				 1);
		}
		
		@Override
		protected void drawAtOrigin(T ent, double len, boolean firstPerson) {
			ResourceLocation[] texData = ent.getTexData();
			int i = ent.ticksExisted % (texData.length - 1);
			this.tex = texData[i + 1];
			this.blendTex = texData[0];
			super.drawAtOrigin(ent, len, firstPerson);
		}
		
	}

}
