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
package cn.academy.test.md;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.academy.core.registry.RegACKeyHandler;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.entityx.EntityAdvanced;
import cn.liutils.template.client.render.entity.RenderIcon;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.helper.KeyHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
@RegEntity
@RegEntity.HasRender
public class EntityMdBall extends EntityAdvanced {
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static R renderer;
	
	@RegACKeyHandler(defaultKey = Keyboard.KEY_J, name = "MdBallTest")
	public static KH key;
	
	static final int MAX_TETXURES = 5;
	
	//Client-side data
	int texID;
	
	long spawnTime;
	long lastTime;
	double alpha = 0.8;
	double accel;
	
	double offsetX, offsetY, offsetZ;
	
	public EntityMdBall(World world) {
		super(world);
		spawnTime = Minecraft.getSystemTime();
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		setDead();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
	}
	
	@SideOnly(Side.CLIENT)
	private void updateRenderTick() {
		final double maxAccel = 4;
		long time = Minecraft.getSystemTime();
		long life = time - spawnTime;
		
		//Alpha wiggling
		if(lastTime != 0) {
			long dt = time - lastTime;
			if(rand.nextInt(8) < 3) {
				accel = RandUtils.ranged(-maxAccel, maxAccel);
				//System.out.println("AccelChange=>" + accel);
			}
			
			//System.out.println("AV=>" + alphaVel);
			alpha += accel * dt / 1000.0;
			if(alpha > 1) alpha = 1;
			if(alpha < 0) alpha = 0;
		}
		lastTime = time;
		
		//Texture wiggling
		if(rand.nextInt(8) < 2) {
			texID = rand.nextInt(MAX_TETXURES);
		}
		
		//Surrounding
		float phase = life / 300.0f;
		offsetX = 0.03 * MathHelper.sin(phase);
		offsetZ = 0.03 * MathHelper.cos(phase);
		offsetY = 0.04 * MathHelper.cos((float) (phase * 1.4 + Math.PI / 3.5));
	}
	
	public static class KH extends KeyHandler {
		
		@Override
		public void onKeyDown() {
			World world = getMC().theWorld;
			EntityPlayer player = getPlayer();
			
			EntityMdBall ball = new EntityMdBall(world);
			ball.setPosition(player.posX, player.posY, player.posZ);
			
			world.spawnEntityInWorld(ball);
		}
	}
	
	@SideOnly(Side.CLIENT)
	public static class R extends RenderIcon {
		
		ResourceLocation[] textures;
		ResourceLocation glowTexture;

		public R() {
			super(null);
			textures = Resources.getEffectSeq("mdball", MAX_TETXURES);
			glowTexture = Resources.getTexture("effects/mdball/glow");
		}
		
		@Override
		public void doRender(Entity par1Entity, double par2, double par4,
				double par6, float par8, float par9) {
			EntityMdBall ent = (EntityMdBall) par1Entity;
			ent.updateRenderTick();
			
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			GL11.glPushMatrix();
			{
			
				GL11.glTranslated(ent.offsetX, ent.offsetY, ent.offsetZ);
				
				//Glow texture
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
				this.color.a = 0.3 + ent.alpha * 0.7;
				this.icon = glowTexture;
				this.setSize(0.7f);
				super.doRender(par1Entity, par2, par4, par6, par8, par9);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				
				//Core
				this.color.a = 0.6 + 0.1 * ent.alpha;
				this.icon = textures[ent.texID];
				this.setSize(0.5f);
				super.doRender(par1Entity, par2, par4, par6, par8, par9);
			
			}
			GL11.glPopMatrix();
			GL11.glEnable(GL11.GL_ALPHA_TEST);
		}
		
	}

}
