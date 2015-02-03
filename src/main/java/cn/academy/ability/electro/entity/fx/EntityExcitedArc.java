/**
 * 
 */
package cn.academy.ability.electro.entity.fx;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.RenderElecArc;
import cn.academy.ability.electro.entity.EntityArcBase;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.motion.LifeTime;
import cn.liutils.util.DebugUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 */
@SideOnly(Side.CLIENT)
@RegistrationClass
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityExcitedArc extends EntityArcBase {
	
	@RegEntity.Render
	public static ThinArcRender render;
	
	public final double len;

	public EntityExcitedArc(World world, Vec3 begin, Vec3 end, int life) {
		super(world);
		this.clearDaemonHandlers();
		addEffectUpdate();
		addDaemonHandler(new LifeTime(this, life));
		setPosition(begin.xCoord, begin.yCoord, begin.zCoord);
		Vec3 dv = begin.subtract(end);
		rotationYaw = -(float) (Math.atan2(dv.xCoord, dv.zCoord) * 180 / Math.PI);
		double tmp = dv.xCoord * dv.xCoord + dv.zCoord * dv.zCoord;
		rotationPitch = (float) (Math.atan2(dv.yCoord, Math.sqrt(tmp)) * 180 / Math.PI);
		
		len = Math.sqrt(tmp + dv.yCoord * dv.yCoord);
		this.ignoreFrustumCheck = true;
		this.isSync = false;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
	}
	
	public static final class ThinArcRender extends RenderElecArc {
		public ThinArcRender() {
			width = 0.3;
		}
		
		@Override
		protected void drawAtOrigin(EntityArcBase ent) {
			EntityExcitedArc arc = (EntityExcitedArc) ent;
			arc.traceDist = arc.len;
			super.drawAtOrigin(ent);
		}
	}
	
}
