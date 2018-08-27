package cn.academy.client.render.misc;

import cn.academy.Resources;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.particle.ParticleFactory;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 *
 */
public class TPParticleFactory extends ParticleFactory
{

    static Particle template = new Particle();

    static {
        template.texture = Resources.getTexture("effects/tp_particle");
        template.size = 0.1f;
        template.hasLight = false;
        template.color.set(255, 255, 255, 255);
    }

    public static TPParticleFactory instance = new TPParticleFactory();

    public TPParticleFactory() {
        super(template);
    }

    @Override
    public Particle next(World world) {
        Particle ret = super.next(world);
        ret.size = RandUtils.rangef(0.1f, 0.2f);
        ret.color.setAlpha(RandUtils.rangei(153, 204));
        ret.fadeAfter(20, 20);
        return ret;
    }

}