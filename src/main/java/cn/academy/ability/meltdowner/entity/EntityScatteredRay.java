/**
 * 
 */
package cn.academy.ability.meltdowner.entity;

import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.ability.meltdowner.client.render.RenderMdRayBase;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.entity.EntitySilbarn;
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
@RegEntity
@RegEntity.HasRender
public class EntityScatteredRay extends EntityMdRayBase {
	
	{
		this.execAfter(20, new EntityCallback<EntityScatteredRay>() {
			@Override
			public void execute(EntityScatteredRay ent) {
				ent.setFadeout(10);
			}
		});
	}
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static SRRender renderer;
	
	float dmg;
	
	public EntityScatteredRay(EntityMeltDowner md, EntitySilbarn silbarn) {
		super(md.getSpawner());
		Motion3D mo = new Motion3D(md, 40, true);
		mo.applyToEntity(this);
		setPosition(silbarn.posX, silbarn.posY, silbarn.posZ);
		
		dmg = (float) (GenericUtils.randIntv(0.3, 0.6) * md.dmg);
	}
	
	public MovingObjectPosition performTrace() {
		Motion3D mo = new Motion3D(this, true);
		Vec3 v1 = mo.getPosVec(worldObj), v2 = mo.move(getDefaultRayLen()).getPosVec(worldObj);
		return GenericUtils.rayTraceBlocksAndEntities(GenericUtils.selectorLiving, worldObj, v1, v2, this, getSpawner());
	}

	@SideOnly(Side.CLIENT)
	public EntityScatteredRay(World world) {
		super(world);
		alpha = 0.7;
	}

	@Override
	protected void handleCollision(MovingObjectPosition mop) {
		if(mop.typeOfHit == MovingObjectType.ENTITY) {
			mop.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(getSpawner()), dmg);
		}
	}
	
	@Override
	public boolean isNearPlayer() {
		return false;
	}
	
	@Override
	protected float getDefaultRayLen() {
		return 20.0f;
	}

	@Override
	public ResourceLocation[] getTexData() {
		return ACClientProps.ANIM_MD_RAY_S;
	}
	
	@SideOnly(Side.CLIENT)
	public static class SRRender extends RenderMdRayBase<EntityScatteredRay> {
		{
			this.widthFp = 0.4;
			this.widthTp = 0.8;
		}
		@Override
		protected void drawAtOrigin(EntityScatteredRay ent, double len, boolean firstPerson) {
			super.drawAtOrigin(ent, len, firstPerson);
		}
	}

}
