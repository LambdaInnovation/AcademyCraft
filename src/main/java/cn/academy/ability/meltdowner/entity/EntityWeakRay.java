/**
 * 
 */
package cn.academy.ability.meltdowner.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.meltdowner.client.render.RenderWeakRay;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
@RegEntity.HasRender
public class EntityWeakRay extends EntityMdRayBase {
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderWeakRay render;
	
	final float dmg;

	public EntityWeakRay(EntityPlayer _spawner, EntityMdBall ball, float _dmg, float scatRange) {
		super(_spawner, ball);
		this.rotationYaw += (float) GenericUtils.randIntv(-scatRange, scatRange);
		this.rotationPitch += (float) GenericUtils.randIntv(-scatRange * .5, scatRange * .5);
		dmg = _dmg;
	}

	public EntityWeakRay(World world) {
		super(world);
		dmg = 1;
		this.ignoreFrustumCheck = true;
	}

	@Override
	protected void handleCollision(MovingObjectPosition mop) {
		if(mop.typeOfHit == MovingObjectType.ENTITY) {
			if(mop.entityHit instanceof EntityLivingBase) {
				EntityLivingBase elb = (EntityLivingBase) mop.entityHit;
				elb.hurtResistantTime = -1;
			}
			mop.entityHit.attackEntityFrom(DamageSource.causeMobDamage(getSpawner()), dmg);
		}
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if(ticksExisted > 20)
			setDead();
	}
	
	@Override
	public boolean isNearPlayer() {
		return false;
	}
	
	@Override
	public double getAlpha() {
		return ticksExisted > 10 ? Math.max(0, 1 - (double)(ticksExisted - 10) / 10) : 1.0;
	}

	@Override
	public ResourceLocation[] getTexData() {
		return ACClientProps.ANIM_MD_RAY_S;
	}

}
