/**
 * 
 */
package cn.academy.ability.electro.entity.fx;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.CubePointFactory;
import cn.academy.ability.electro.client.render.IPointFactory.NormalVert;
import cn.liutils.api.entityx.FakeEntity;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.api.entityx.motion.FollowEntity;
import cn.liutils.api.entityx.motion.LifeTime;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 */
@SideOnly(Side.CLIENT)
public class ChargeEffectS extends FakeEntity {
	
	List<EntityArcS> ents = new ArrayList<EntityArcS>();

	int itensity;
	CubePointFactory factory;
	
	public boolean draw = true;
	
	public ChargeEffectS(Entity target, int lifeTime, int _itensity) {
		this(target, lifeTime, _itensity, 2.0);
	}
	
	public ChargeEffectS(Entity target, int lifeTime, int _itensity, double _ht) {
		super(target);
		init(lifeTime, _itensity, _ht);
	}
	
	public ChargeEffectS(World world, double x, double y, double z, int lifeTime, int _itensity, double _ht) {
		super(world, x, y, z);
		init(lifeTime, _itensity, _ht);
	}
	
	private void init(int lifeTime, int _itensity, double _ht) {
		itensity = _itensity;
		factory = new CubePointFactory(1.2, _ht, 1.2);
		addDaemonHandler(new LifeTime(this, lifeTime));
		addDaemonHandler(new Update());
		initEff();
	}
	
	private void initEff() {
		int n = itensity + rand.nextInt((int)(itensity * 0.4));
		for(int i = 0; i < n; ++i) {
			EntityArcS arc = EntityArcS.get(worldObj);
			arc.setPosition(posX, posY, posZ);
			NormalVert vt = factory.next();
			arc.addDaemonHandler(new FollowEntity(arc, this)
				.setOffset(vt.vert.xCoord - .6, vt.vert.yCoord - 2, vt.vert.zCoord - .6));
			ents.add(arc);
			worldObj.spawnEntityInWorld(arc);
		}
	}
	
	@Override
    public void setDead() {
    	super.setDead();
    	for(EntityArcS ent : ents) {
    		ent.setDead();
    	}
    }

	private class Update extends MotionHandler<ChargeEffectS> {
		
		int untilCheck;
		int checkTime;
		boolean show = false;
		int showFrom = 18, showTo = 28;
		int hideFrom = 4, hideTo = 8;

		public Update() {
			super(ChargeEffectS.this);
		}

		@Override
		public void onCreated() {}

		@Override
		public void onUpdate() {
			if(++untilCheck > checkTime) {
				untilCheck = 0;
				show = !show;
				if(show) {
					checkTime = GenericUtils.randIntv(showFrom, showTo);
				} else {
					checkTime = GenericUtils.randIntv(hideFrom, hideTo);
					for(EntityArcS ent : ents) {
						FollowEntity fe = (FollowEntity) ent.getDaemonHandler("followent");
						
						NormalVert vt = factory.next();
						fe.setOffset(vt.vert.xCoord - .6, vt.vert.yCoord - 2, vt.vert.zCoord - .6);
					}
				}
				for(EntityArcS arc : ents) {
					arc.show = show && draw;
				}
			}
		}

		@Override
		public String getID() {
			return "check";
		}
		
	}

}
