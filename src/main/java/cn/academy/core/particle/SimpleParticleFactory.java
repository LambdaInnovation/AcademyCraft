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
package cn.academy.core.particle;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class SimpleParticleFactory extends AbstractParticleFactory {
	
	public World world;
	public Particle template;
	
	public Vec3 
		pos = Vec3.createVectorHelper(0, 0, 0),
		vel = Vec3.createVectorHelper(0.0, 0.0, 0.0);
	
	public SimpleParticleFactory(Particle _template) {
		template = _template;
	}

	@Override
	public Particle next() {
		Particle p = this.queryParticle();
		p.worldObj = world;
		p.fromTemplate(template);
		
		p.setPosition(pos.xCoord, pos.yCoord, pos.zCoord);
		
		p.motionX = vel.xCoord;
		p.motionY = vel.yCoord;
		p.motionZ = vel.zCoord;
		return p;
	}

}
