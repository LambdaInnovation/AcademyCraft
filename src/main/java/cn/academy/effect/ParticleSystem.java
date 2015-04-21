package cn.academy.effect;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

@RegistrationClass
public class ParticleSystem {
	
	public static Random rand = new Random();
	static int MAX_POOL_SIZE = 200;

	static List<Particle> alive = new LinkedList(), dead = new LinkedList();
	
	public Particle template;
	public Vec3Generator velGen;
	
	public ParticleSystem(Particle _template, Vec3Generator _velGen) {
		template = _template;
		velGen = _velGen;
	}
	
	public Particle next(World world, double x, double y, double z) {
		Particle inst = queryInstance(world);
		inst.fromTemplate(template);
		inst.init(Vec3.createVectorHelper(x, y, z), velGen.next());
		return inst;
	}
	
	private static Particle queryInstance(World world) {
		Particle ret;
		if(!dead.isEmpty()) {
			Iterator<Particle> iter = dead.iterator();
			Particle next = iter.next();
			iter.remove();
			next.worldObj = world;
			next.isDead = false;
			ret = next;
		} else {
			ret = new Particle(world);
		}
		if(alive.size() < MAX_POOL_SIZE) {
			alive.add(ret);
		}
		return ret;
	}
	
	public static interface Vec3Generator {
		Vec3 next();
	};
	
	@RegEventHandler
	public static class EventHandler {
		int tickUntilGC = 0;
		
		@SubscribeEvent
		public void onClientTick(ClientTickEvent event) {
			if(event.phase == Phase.START || ++tickUntilGC < 20) {
				return;
			}
			tickUntilGC = 0;
			
			Iterator<Particle> iter = alive.iterator();
			while(iter.hasNext()) {
				Particle p = iter.next();
				if(p.isDead) {
					iter.remove();
					if(dead.size() < MAX_POOL_SIZE) {
						dead.add(p);
					} // else: drop the instance.
				}
			}
			//System.out.println("Particle GC: " + alive.size() + "(alive), " + dead.size() + "(dead)");
		}
	}
	
}
