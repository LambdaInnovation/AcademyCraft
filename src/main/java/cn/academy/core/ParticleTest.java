package cn.academy.core;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import cn.academy.core.particle.Particle;
import cn.academy.core.particle.SimpleParticleFactory;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.liutils.entityx.MotionHandler;
import cn.liutils.entityx.handlers.Rigidbody;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.relauncher.Side;

//@RegistrationClass
@RegEventHandler(Bus.FML)
public class ParticleTest {
	
	SimpleParticleFactory factory;
	Random rand = new Random();
	
	public ParticleTest() {
		Particle template = new Particle();
		template.texture = new ResourceLocation("academy:textures/effects/is_test.png");
		template.size = 0.6f;
		
		factory = new SimpleParticleFactory(template) {
			@Override
			public Particle next() {
				Particle ret = super.next();
				ret.addMotionHandler(new Rigidbody());
				final long time = GenericUtils.randIntv(3000, 4000);
				ret.addMotionHandler(new MotionHandler<Particle>() {
					@Override
					public String getID() {
						return "FadeOut";
					}
					@Override
					public void onStart() {
						getTarget().color.a = 1.0;
					}
					@Override
					public void onUpdate() {
						getTarget().color.a = Math.max(0.0, 1.0 - (double)getTarget().getParticleLife() / time);
						//System.out.println(getTarget().color.a);
						//getTarget().color.a = 0.3f;
						if(getTarget().getParticleLife() > time + 1000) {
							getTarget().setDead();
						}
					}
				});
				return ret;
			}
		};
	}
	
	@SubscribeEvent
	public void playerTick(PlayerTickEvent event) {
		if(event.phase == Phase.END && event.side == Side.CLIENT) {
			EntityPlayer player = event.player;
			factory.world = player.worldObj;
			
			int n = GenericUtils.randIntv(4, 8);
			for(int i = 0; i < n; ++i) {
				double theta = rand.nextDouble() * Math.PI * 2;
				double r = 1.5 + GenericUtils.randIntv(-0.1, 0.1);
				double dx = r * Math.sin(theta), dy = GenericUtils.randIntv(-2, -1.5), dz = r * Math.cos(theta);
				factory.pos = Vec3.createVectorHelper(player.posX + dx, player.posY + dy, player.posZ + dz);
				factory.vel = Vec3.createVectorHelper(GenericUtils.randIntv(-0.02, 0.02), GenericUtils.randIntv(0.02, 0.03), GenericUtils.randIntv(-0.02, 0.02));
				player.worldObj.spawnEntityInWorld(factory.next());
			}
		}
	}
}
