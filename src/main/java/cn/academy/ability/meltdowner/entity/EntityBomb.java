/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.meltdowner.entity;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.motion.CollisionCheck;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
public class EntityBomb extends EntityMdBall {
	
	float dmg;

	public EntityBomb(EntityPlayer player, float _dmg) {
		super(player);
		dmg = _dmg;
		init();
		this.execAfter(15, new EntityCallback<EntityMdBall>() {

			@Override
			public void execute(EntityMdBall ball) {
				removeDaemonHandler("followent");
				//setState(BallState.ACTIVE);

				MovingObjectPosition ret = GenericUtils.tracePlayer(spawner, 40.0);
				double tx, ty, tz;
				if(ret == null) {
					Motion3D mo = new Motion3D(spawner, true).move(40.0);
					tx = mo.posX;
					ty = mo.posY;
					tz = mo.posZ;
				} else {
					tx = ret.hitVec.xCoord;
					ty = ret.hitVec.yCoord;
					tz = ret.hitVec.zCoord;
					if(ret.typeOfHit == MovingObjectType.ENTITY) {
						ty += ret.entityHit.height * 0.7;
					}
				}
				tx -= posX;
				ty -= posY;
				tz -= posZ;
				
				setHeading(tx, ty, tz, 0.9);
				
				addCollisionCheck();
			}
			
		});
	}

	public EntityBomb(World world) {
		super(world);
		this.execAfter(15, new EntityCallback<EntityMdBall>() {

			@Override
			public void execute(EntityMdBall ball) {
				removeDaemonHandler("followent");
				addCollisionCheck();
				//setState(BallState.ACTIVE);
			}
			
		});
		init();
	}
	
	private void init() {
		this.fadeTime = 13;
	}
	
	private void addCollisionCheck() {
		CollisionCheck collider = new CollisionCheck(this) {
			@Override
			protected void onCollided(MovingObjectPosition res) {
				if(res.typeOfHit == MovingObjectType.ENTITY) {
					res.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(spawner), dmg);
				}
				EntityBomb.this.setDead();
			}
		}.setSelector(new IEntitySelector() {

			@Override
			public boolean isEntityApplicable(Entity var1) {
				return var1 instanceof EntityLivingBase && !var1.equals(spawner);
			}
			
		});
		
		addDaemonHandler(collider);
	}
	
	@Override
	public boolean doesFollow() { return ticksExisted <= 15; }

}
