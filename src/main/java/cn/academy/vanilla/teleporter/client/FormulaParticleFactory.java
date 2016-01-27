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
import cn.lambdalib.particle.decorators.ParticleDecorator;
import cn.lambdalib.util.generic.RandUtils;
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
