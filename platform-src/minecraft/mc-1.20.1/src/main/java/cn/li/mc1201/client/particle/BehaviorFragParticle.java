package cn.li.mc1201.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Generic fragment particle for a behavior-driven impact burst.
 *
 * Texture and registration are supplied by the content module.
 *   size     0.1
 *   gravity  0.03
 *   rotation each tick: roll += sin(phi) * 25 deg where phi is per-particle random
 *   lifetime ~60 ticks (not specified in original; chosen to let gravity do its work)
 */
public final class BehaviorFragParticle extends TextureSheetParticle {

    private final float rollSpeed;

    BehaviorFragParticle(ClientLevel level, double x, double y, double z,
                        double vx, double vy, double vz, SpriteSet spriteSet) {
        super(level, x, y, z, vx, vy, vz);
        this.setSprite(spriteSet.get(this.random));
        this.gravity = 0.03F;
        this.quadSize = 0.1F;
        this.lifetime = 60;
        this.roll = this.random.nextFloat() * (float) Math.PI * 2.0F;
        this.oRoll = this.roll;
        double phi = this.random.nextDouble() * Math.PI * 2.0;
        this.rollSpeed = (float) (Math.sin(phi) * Math.toRadians(25.0));
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        this.roll += this.rollSpeed;
        super.tick();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new BehaviorFragParticle(level, x, y, z, vx, vy, vz, this.spriteSet);
        }
    }
}
