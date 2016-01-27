/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.client;

import cn.academy.core.client.Resources;
import cn.lambdalib.particle.Particle;
import cn.lambdalib.particle.ParticleFactory;
import cn.lambdalib.util.generic.RandUtils;
import net.minecraft.world.World;

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
