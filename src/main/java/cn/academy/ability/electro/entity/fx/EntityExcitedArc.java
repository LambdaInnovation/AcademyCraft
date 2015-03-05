/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.electro.entity.fx;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.entity.ThinArcRender;
import cn.academy.ability.electro.entity.EntityArcBase;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.motion.LifeTime;
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
	@SideOnly(Side.CLIENT)
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
	
}
