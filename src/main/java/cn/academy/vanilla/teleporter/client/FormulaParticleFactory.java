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
package cn.academy.vanilla.teleporter.client;

import cn.academy.core.client.Resources;
import cn.liutils.render.particle.Particle;
import cn.liutils.render.particle.ParticleFactory;
import cn.liutils.render.particle.decorators.ParticleDecorator;
import cn.liutils.util.generic.RandUtils;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
public class FormulaParticleFactory extends ParticleFactory {
	
	public static final FormulaParticleFactory instance = new FormulaParticleFactory();
	
	static ResourceLocation[] textures = Resources.getEffectSeq("formula", 10);

	private FormulaParticleFactory() {
		super(new Particle());
		this.template.color.setColor4i(220, 220, 220, 255);
		this.template.hasLight = false;
		
		this.addDecorator(new ParticleDecorator() {

			@Override
			public void decorate(Particle particle) {
				particle.size = RandUtils.rangef(1, 1.7f);
				particle.color.a = RandUtils.ranged(0.6, 1.5);
				particle.texture = textures[RandUtils.nextInt(textures.length)];
				particle.fadeInTime = 2;
				particle.fadeAfter(RandUtils.rangei(10, 15), 20);
			}
			
		});
	}

}
