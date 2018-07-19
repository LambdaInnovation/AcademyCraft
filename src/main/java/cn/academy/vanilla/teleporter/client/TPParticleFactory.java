package cn.academy.vanilla.teleporter.client;

import cn.academy.core.Resources;
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
