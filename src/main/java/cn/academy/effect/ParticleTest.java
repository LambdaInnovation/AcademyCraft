package cn.academy.effect;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import cn.academy.effect.ParticleSystem.Vec3Generator;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;

@RegistrationClass
@RegEventHandler
public class ParticleTest {
	
	ParticleSystem ps;
	Random rand = new Random();
	
	public ParticleTest() {
		Particle template = Particle.createTemplate();
		template.texture = new ResourceLocation("academy:textures/effects/md_particle.png");	
//		template.color = new Color(1, 1, 0, 1);
		template.size = 0.2f;
		ps = new ParticleSystem(template, new Vec3Generator() {
			Random rand = new Random();
			@Override
			public Vec3 next() {
				//Create fly-up particles with random horizontal velocity.
				double horvel = GenericUtils.randIntv(0.03, 0.05), vy = GenericUtils.randIntv(0.05, 0.1);
				double phi = rand.nextDouble() * 100;
				double vx = horvel * Math.sin(phi), vz = horvel * Math.cos(phi);
				return Vec3.createVectorHelper(vx, vy, vz);
			}
		});
	}

	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		if(player == null) return;
		if(rand.nextDouble() < 0.2) {
			double yaw = rand.nextDouble() * 100;
			double dh = GenericUtils.randIntv(0.5, 1.5), dy = GenericUtils.randIntv(-1, 1);
			double dx = dh * Math.sin(yaw), dz = dh * Math.cos(yaw);
			
			//Modify the color.
//			double[] c1 = { 62, 146, 225 }, c2 = { 193, 113, 235 };
//			double r = rand.nextDouble(), rr = 1 - r;
//			//lerp the color.
//			Color c = ps.template.color;
//			c.r = (c1[0] * r + c2[1] * rr) / 255;
//			c.g = (c1[1] * r + c2[1] * rr) / 255;
//			c.b = (c1[2] * r + c2[2] * rr) / 255;
			
			player.worldObj.spawnEntityInWorld(ps.next(player.worldObj, player.posX + dx, player.posY + dy, player.posZ + dz));
		}
	}
}
