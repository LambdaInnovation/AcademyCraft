/**
 * 
 */
package cn.academy.ability.electro.entity.fx;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.CubePointFactory;
import cn.academy.ability.electro.client.render.IPointFactory.NormalVert;
import cn.academy.core.proxy.ACClientProps;
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
	
	public static class Strong extends ChargeEffectS {
		public Strong(Entity target, int lifeTime, int _itensity) {
			super(target, lifeTime, _itensity);
		}
		
		@Override
		protected ResourceLocation[] getTexs() {
			return ACClientProps.ANIM_ARC_W;
		}
		
		@Override
		protected float getSize() {
			return .9f;
		}
	}
	
	List<EntityArcS> ents = new ArrayList<EntityArcS>();

	int itensity;
	CubePointFactory factory;
	
	public boolean draw = true;
	double ht;
	
	public ChargeEffectS(Entity target, int lifeTime, int _itensity) {
		super(target);
		init(lifeTime, _itensity, target.width, target.height);
	}
	
	public ChargeEffectS(World world, double x, double y, double z, int lifeTime, int _itensity, double _ht) {
		super(world, x, y, z);
		init(lifeTime, _itensity, 1, _ht);
	}
	
	private void init(int lifeTime, int _itensity, double _w, double _ht) {
		itensity = _itensity;
		factory = new CubePointFactory(_w, _ht, _w);
		factory.setCentered(true);
		addDaemonHandler(new LifeTime(this, lifeTime));
		addDaemonHandler(new Update());
		ht = _ht;
		initEff();
	}
	
	private void initEff() {
		int n = itensity + rand.nextInt((int)(itensity * 0.4));
		for(int i = 0; i < n; ++i) {
			EntityArcS arc = EntityArcS.get(worldObj);
			arc.setPosition(posX, posY, posZ);
			arc.texs = getTexs();
			NormalVert vt = factory.next();
			arc.addDaemonHandler(new FollowEntity(arc, this)
				.setOffset(vt.vert.xCoord, vt.vert.yCoord - 2, vt.vert.zCoord));
			arc.setSize(getSize());
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
						fe.setOffset(vt.vert.xCoord, vt.vert.yCoord - ht, vt.vert.zCoord);
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
	
	protected ResourceLocation[] getTexs() {
		return  ACClientProps.ANIM_SMALL_ARC;
	}
	
	protected float getSize() {
		return 0.5f;
	}

}
