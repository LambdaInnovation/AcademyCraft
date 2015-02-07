/**
 * 
 */
package cn.academy.ability.electro.entity;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.client.render.RenderElecArc;
import cn.academy.ability.electro.entity.fx.ChargeEffectS;
import cn.academy.ability.electro.skill.SkillItemCharge;
import cn.academy.api.data.AbilityData;
import cn.academy.core.util.EnergyUtils;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity
@RegEntity.HasRender
public class EntityChargingArc extends EntityArcBase {
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static CARender renderer;

	public EntityChargingArc(final AbilityData data) {
		super(data.getPlayer());
		System.out.println("owwwe");
		this.setCurMotion(new MotionHandler(this) {
			
			int lastX, lastY, lastZ;
			boolean last = false;
			
			@SideOnly(Side.CLIENT)
			ChargeEffectS ces = null;
			
			double upt = CatElectro.itemCharge.getEPT(data);
			@Override public void onCreated() {}
			@Override public void onUpdate() {
				MovingObjectPosition mop = GenericUtils.tracePlayer(data.getPlayer(), 6.0);
				boolean b = false;
				if(mop != null && mop.typeOfHit == MovingObjectType.BLOCK && 
						(b = EnergyUtils.isReceiver(worldObj, mop.blockX, mop.blockY, mop.blockZ))) {
					EnergyUtils.tryCharge(worldObj, mop.blockX, mop.blockY, mop.blockZ, upt);
				} else {
					//TODO Hurt player?
				}
				System.out.println("drrrrrrr");
				if(worldObj.isRemote) {
					updateRenderEff(mop, b);
				}
			}
			@Override public String getID() {
				return "main";
			}
			
			@SideOnly(Side.CLIENT)
			private void updateRenderEff(MovingObjectPosition mop, boolean b) {
				if(b) {
					if(last && (lastX != mop.blockX || lastY != mop.blockY || lastZ != mop.blockZ)) {
						lastX = mop.blockX;
						lastY = mop.blockY;
						lastZ = mop.blockZ;
						ces.setDead();
						ces = null;
					}
					if(ces == null) {
						ces = new ChargeEffectS(worldObj, 
								mop.blockX + .5, mop.blockY + 2, mop.blockZ + .5, 1000, 5, 1);
						worldObj.spawnEntityInWorld(ces);
					}
				} else {
					if(ces != null) {
						ces.setDead();
						ces = null;
					}
				}
				last = b;
			}
			
		});
	}

	public EntityChargingArc(World world) {
		super(world);
	}
	
	public static class CARender extends RenderElecArc {
		{
			this.width = 0.25F;
		}
	}

}
