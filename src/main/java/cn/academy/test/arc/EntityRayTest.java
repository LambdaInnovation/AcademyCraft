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
package cn.academy.test.arc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import cn.academy.core.client.render.RendererRayComposite;
import cn.academy.core.entity.EntityRayBase;
import cn.academy.core.registry.RegKeyHandler;
import cn.academy.core.util.KeyHandler;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util3.VecUtils;
import cn.liutils.util3.space.Motion3D;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@RegEntity
@RegEntity.HasRender
public class EntityRayTest extends EntityRayBase {
	
	@RegEntity.Render
	//public static Render renderer = RendererRaySimple.createFromName("mdray_s");
	//public static Render renderer = new RendererRayCylinder();
	public static Render renderer = new RendererRayComposite("railgun");
	
	@RegKeyHandler(defaultKey = Keyboard.KEY_L, name = "MeltDowner")
	public static KH keyHandler;
	
	public EntityRayTest(EntityPlayer player) {
		this(player.worldObj);
		new Motion3D(player, true).applyToEntity(this);
		Vec3 offset = VecUtils.vec(0, 0, 1);
		offset = VecUtils.toGlobalOffset(player, offset);
		
		posX += offset.xCoord;
		posY += offset.yCoord;
		posZ += offset.zCoord;
		
		ignoreFrustumCheck = true;
	}

	public EntityRayTest(World world) {
		super(world);
	}
	
	public static class KH extends KeyHandler {
		
		@Override
		public void onKeyDown() {
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			player.worldObj.spawnEntityInWorld(new EntityRayTest(player));
		}
		
	}

}
