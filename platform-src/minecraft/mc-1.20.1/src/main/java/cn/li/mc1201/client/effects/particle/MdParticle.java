package cn.li.mc1201.client.effects.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Meltdowner md particle — port of upstream MdParticleFactory's template:
 * life 25-55 ticks with a 20-tick fade-out, alpha 76-152, size 0.05-0.07,
 * no gravity -- MdParticleFactory's template leaves Particle.gravity at 0 and
 * nothing re-sets it, so these motes drift on their given velocity alone. (The
 * 0.01 upstream is MineRaysBase's RIGIDBODY gravity on falling block debris, a
 * different thing entirely.) Rendered with the md_particle / md_particle_luck
 * sprites from the mod's particles/<id>.json atlas definitions.
 */
public class MdParticle extends TextureSheetParticle {

    private final float baseAlpha;
    private final int fadeStart;

    protected MdParticle(ClientLevel level, double x, double y, double z,
                         double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.pickSprite(sprites);
        this.lifetime = 25 + this.random.nextInt(31);
        this.fadeStart = this.lifetime - 20;
        this.baseAlpha = (76 + this.random.nextInt(77)) / 255.0F;
        this.quadSize = 0.05F + this.random.nextFloat() * 0.02F;
        this.gravity = 0.0F;
        this.setAlpha(this.baseAlpha);
    }

    @Override
    public void tick() {
        super.tick();
        // fadeAfter(life, 20): alpha ramps down over the last 20 ticks.
        if (this.age > this.fadeStart && this.fadeStart >= 0) {
            float f = (float) (this.lifetime - this.age) / (float) (this.lifetime - this.fadeStart);
            this.setAlpha(this.baseAlpha * Math.max(0.0F, f));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new MdParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
