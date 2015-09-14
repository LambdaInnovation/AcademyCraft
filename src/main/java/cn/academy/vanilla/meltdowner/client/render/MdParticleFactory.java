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
package cn.academy.vanilla.meltdowner.client.render;

import cn.academy.core.client.Resources;
import cn.liutils.render.particle.Particle;
import cn.liutils.render.particle.ParticleFactory;
import cn.liutils.render.particle.decorators.ParticleDecorator;
import cn.liutils.util.generic.RandUtils;

/**
 * @author WeAthFolD
 */
public class MdParticleFactory extends ParticleFactory {
	
	static Particle template;
	static {
		template = new Particle();
		template.texture = Resources.getTexture("effects/md_particle");
	}
	public static MdParticleFactory INSTANCE = new MdParticleFactory(template);

	private MdParticleFactory(Particle _template) {
		super(_template);
		
		this.addDecorator(new ParticleDecorator() {

			@Override
			public void decorate(Particle particle) {
				int life = RandUtils.rangei(25, 55);
				particle.fadeAfter(life, 20);
				particle.color.a = RandUtils.ranged(0.3, 0.6);
				particle.size = RandUtils.rangef(0.05f, 0.07f);
			}
			
		});
	}

}
