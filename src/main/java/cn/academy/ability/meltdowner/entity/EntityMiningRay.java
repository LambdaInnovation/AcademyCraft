/**
 * 
 */
package cn.academy.ability.meltdowner.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * @author WeathFolD
 *
 */
public class EntityMiningRay extends EntityMdRayBase {

	/**
	 * @param _spawner
	 */
	public EntityMiningRay(EntityLivingBase _spawner) {
		super(_spawner);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param _spawner
	 * @param ball
	 */
	public EntityMiningRay(EntityPlayer _spawner, EntityMdBall ball) {
		super(_spawner, ball);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param world
	 */
	public EntityMiningRay(World world) {
		super(world);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see cn.academy.ability.meltdowner.entity.EntityMdRayBase#handleCollision(net.minecraft.util.MovingObjectPosition)
	 */
	@Override
	protected void handleCollision(MovingObjectPosition mop) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see cn.academy.ability.meltdowner.entity.EntityMdRayBase#getTexData()
	 */
	@Override
	protected ResourceLocation[] getTexData() {
		// TODO Auto-generated method stub
		return null;
	}

}
