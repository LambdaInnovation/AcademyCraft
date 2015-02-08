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
@RegistrationClass
@RegEntity
@RegEntity.HasRender
public class EntityExcitedArc extends EntityArcBase {
	
	@RegEntity.Render
	public static ThinArcRender render;

	public EntityExcitedArc(World world, Vec3 begin, Vec3 end, int life) {
		super(world);
		this.clearDaemonHandlers();
		addDaemonHandler(new LifeTime(this, life));
		this.setByPoint(begin.xCoord, begin.yCoord, begin.zCoord, end.xCoord, end.yCoord, end.zCoord);
	}
	
	public EntityExcitedArc(World world) {
		super(world);
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
	}
	
	@Override
	protected boolean doesPerformTrace() {
		return false;
	}
	
	public static final class ThinArcRender extends RenderElecArc {
		public ThinArcRender() {
			width = 0.3;
		}
	}
	
}
