package cn.academy.vanilla.meltdowner.client.render;

import cn.academy.core.Resources;

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