package cn.academy.vanilla.teleporter.client;

import cn.academy.core.Resources;
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
