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

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import cn.academy.test.arc.ArcFactory.Arc;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.key.IKeyHandler;
import cn.liutils.entityx.EntityAdvanced;
import cn.liutils.registry.AttachKeyHandlerRegistry.RegAttachKeyHandler;
import cn.liutils.util3.ClientUtils;
import cn.liutils.util3.GenericUtils;
import cn.liutils.util3.space.Motion3D;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@RegEntity
@RegEntity.HasRender
public class EntityArc extends EntityAdvanced {
	
	@RegEntity.Render
	public static Renderer render = new Renderer();
	
	int [] iid;
	int n = GenericUtils.randIntv(1, 3);
	boolean show = true;

	public EntityArc(EntityPlayer player) {
		super(player.worldObj);
		new Motion3D(player, true).applyToEntity(this);
		ignoreFrustumCheck = true;
		iid = new int[n];
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		for(int i = 0; i < iid.length; ++i) {
			if(rand.nextDouble() < 0.5)
				iid[i] = rand.nextInt(100);
		}
		if(show && rand.nextDouble() < 0.2) {
			show = !show;
		}
		else if(!show && rand.nextDouble() < 0.2) {
			show = !show;
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	public static class Renderer extends Render {
		
		Arc[] patterns = new Arc[100];
		
		public Renderer() {
			ArcFactory fac = new ArcFactory();
			for(int i = 0; i < 100; ++i) {
				patterns[i] = fac.generate();
			}
		}
		
		@Override
		public void doRender(Entity e, double x, double y, double z, float f, float g) {
			EntityArc arc = (EntityArc) e;
			if(!arc.show)
				return;
			
			GL11.glPushMatrix();
			
			GL11.glTranslated(x, y, z);
			GL11.glRotatef(arc.rotationYaw + 90, 0, -1, 0);
			GL11.glRotatef(arc.rotationPitch, 0, 0, -1);
			for(int i = 0; i < arc.n; ++i)
				patterns[arc.iid[i]].draw();
			
			GL11.glPopMatrix();
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
			return null;
		}
		
	}
	
	@RegAttachKeyHandler(clazz = KeyHandler.class)
	public static final int keyid = Keyboard.KEY_P;
	
	public static class KeyHandler implements IKeyHandler {

		@Override
		public void onKeyDown(int keyCode, boolean tickEnd) {
			if(!tickEnd && ClientUtils.isPlayerInGame()) {
				EntityPlayer p = Minecraft.getMinecraft().thePlayer;
				EntityArc a = new EntityArc(p);
				p.worldObj.spawnEntityInWorld(a);
			}
		}

		@Override
		public void onKeyUp(int keyCode, boolean tickEnd) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onKeyTick(int keyCode, boolean tickEnd) {
			// TODO Auto-generated method stub
			
		}
		
	}

}
