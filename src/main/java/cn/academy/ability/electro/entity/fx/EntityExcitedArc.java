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
		addDaemonHandler(new LifeTime(this, life));
		setPosition(begin.xCoord, begin.yCoord, begin.zCoord);
		len = begin.distanceTo(end);
		System.out.println(DebugUtils.formatArray(posX, posY, posZ) + " " + traceDist);
		this.ignoreFrustumCheck = true;
		this.isSync = false;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		//System.out.println("upd " + worldObj.isRemote + " " + this.traceDist);
	}
	
	public static final class ThinArcRender extends RenderElecArc {
		public ThinArcRender() {
			width = 0.3;
		}
		
		@Override
		protected void drawAtOrigin(EntityArcBase ent) {
			EntityExcitedArc arc = (EntityExcitedArc) ent;
			arc.traceDist = arc.len;
			//System.out.println("Drawing " + arc.traceDist);
			super.drawAtOrigin(ent);
		}
	}
	
}
