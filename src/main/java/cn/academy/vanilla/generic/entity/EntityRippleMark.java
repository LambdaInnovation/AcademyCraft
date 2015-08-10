/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.vanilla.generic.entity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import cn.academy.vanilla.generic.client.render.RippleMarkRender;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.entityx.EntityAdvanced;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.GameTimer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegEntity
@RegEntity.HasRender
public class EntityRippleMark extends EntityAdvanced {
	
	@RegEntity.Render
	public static RippleMarkRender renderer;
	
	public final Color color = Color.WHITE();
	public final long creationTime = GameTimer.getTime();

	public EntityRippleMark(World world) {
		super(world);
		setSize(2, 2);
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {}

}
