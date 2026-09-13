package cn.li.fabric262.shim;

import cn.li.mcver.ResourceLocations;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Fabric-side helper for registering particle types directly into BuiltInRegistries.
 * Uses FabricParticleTypes.simple() because SimpleParticleType's constructor is
 * protected under Fabric/Yarn mappings.
 */
public final class FabricParticleHelper {
    private FabricParticleHelper() {
    }

    public static SimpleParticleType registerParticle(String namespace, String path, boolean alwaysShow) {
        SimpleParticleType type = FabricParticleTypes.simple(alwaysShow);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, ResourceLocations.of(namespace, path), type);
        return type;
    }
}
