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
package cn.academy.categories.meltdowner.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.input.Keyboard;

import cn.academy.generic.client.render.RendererRaySimple;
import cn.academy.generic.entity.EntityRayBase;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.key.IKeyHandler;
import cn.liutils.registry.AttachKeyHandlerRegistry.RegAttachKeyHandler;
import cn.liutils.render.material.Material;
import cn.liutils.util.ClientUtils;
import cn.liutils.util.space.Motion3D;

/**
 * @author WeAthFolD
 */
@RegistrationClass
@RegEntity
@RegEntity.HasRender
public class EntityMdSmallRay extends EntityRayBase {
	
	@RegEntity.Render
	public static Render renderer = RendererRaySimple.createFromName("mdray_s");

	public EntityMdSmallRay(EntityPlayer player) {
		super(player.worldObj);
		new Motion3D(player, true).applyToEntity(this);
		ignoreFrustumCheck = true;
	}

	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}

	@RegAttachKeyHandler(clazz = KH.class)
	public static int k = Keyboard.KEY_U;
	
	public static class KH implements IKeyHandler {

		@Override
		public void onKeyDown(int keyCode, boolean tickEnd) {
			if(!tickEnd && ClientUtils.isPlayerInGame()) {
				Minecraft mc = Minecraft.getMinecraft();
				mc.theWorld.spawnEntityInWorld(new EntityMdSmallRay(mc.thePlayer));
			}
		}

		@Override
		public void onKeyUp(int keyCode, boolean tickEnd) {}
		@Override
		public void onKeyTick(int keyCode, boolean tickEnd) {}
		
	}
	
}
