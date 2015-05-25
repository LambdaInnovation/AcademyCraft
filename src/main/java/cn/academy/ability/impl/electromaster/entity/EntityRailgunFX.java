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
package cn.academy.ability.impl.electromaster.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import cn.academy.core.client.render.ray.RendererRayComposite;
import cn.academy.core.entity.EntityRayBase;
import cn.academy.core.registry.RegACKeyHandler;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util.generic.VecUtils;
import cn.liutils.util.helper.KeyHandler;
import cn.liutils.util.helper.Motion3D;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@RegEntity
public class EntityRailgunFX extends EntityRayBase {
	
	@RegACKeyHandler(defaultKey = Keyboard.KEY_L, name = "MeltDowner")
	public static KH keyHandler;
	
	public EntityRailgunFX(EntityPlayer player) {
		this(player.worldObj);
		new Motion3D(player, true).applyToEntity(this);
		
		this.life = 40;
		
		ignoreFrustumCheck = true;
	}

	public EntityRailgunFX(World world) {
		super(world);
	}
	
	@Override
	protected void onFirstUpdate() {
		super.onFirstUpdate();
		worldObj.playSound(posX, posY, posZ, "academy:elec.railgun", 0.5f, 1.0f, false);
	}
	
	public static class KH extends KeyHandler {
		
		@Override
		public void onKeyDown() {
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			player.worldObj.spawnEntityInWorld(new EntityRailgunFX(player));
		}
		
	}

}
