package cn.academy.client.render.particle;

import cn.academy.Resources;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.particle.ParticleFactory;
import cn.lambdalib2.particle.decorators.ParticleDecorator;
import cn.lambdalib2.util.RandUtils;

/**
 * @author WeAthFolD
 */
public class MdParticleFactory extends ParticleFactory
{
    
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
                particle.color.setAlpha(RandUtils.rangei(76, 152));
                particle.size = RandUtils.rangef(0.05f, 0.07f);
            }
            
        });
    }

}