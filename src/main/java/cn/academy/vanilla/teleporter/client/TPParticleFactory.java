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

import net.minecraft.world.World;
import cn.academy.core.client.Resources;
import cn.lambdalib.particle.Particle;
import cn.lambdalib.particle.ParticleFactory;
import cn.lambdalib.util.generic.RandUtils;

/**
 * @author WeAthFolD
 *
 */
public class TPParticleFactory extends ParticleFactory {

    static Particle template = new Particle();

    static {
        template.texture = Resources.getTexture("effects/tp_particle");
        template.size = 0.1f;
        template.hasLight = false;
        template.color.setColor4d(1, 1, 1, 1);
    }

    public static TPParticleFactory instance = new TPParticleFactory();

    public TPParticleFactory() {
        super(template);
    }

    @Override
    public Particle next(World world) {
        Particle ret = super.next(world);
        ret.size = RandUtils.rangef(0.1f, 0.2f);
        ret.color.a = RandUtils.ranged(0.6f, 0.8f);
        ret.fadeAfter(20, 20);
        return ret;
    }

}
