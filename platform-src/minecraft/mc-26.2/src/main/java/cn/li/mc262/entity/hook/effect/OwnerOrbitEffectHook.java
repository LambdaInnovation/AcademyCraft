package cn.li.mc262.entity.hook.effect;

import cn.li.mcbase.entity.hook.effect.ScriptedEffectHook;

import cn.li.mc262.entity.ScriptedEffectEntity;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class OwnerOrbitEffectHook implements ScriptedEffectHook {
    private static final double DEFAULT_RANGE_FROM = 0.8D;
    private static final double DEFAULT_RANGE_TO = 1.3D;
    private static final double DEFAULT_Y_FROM = -1.2D;
    private static final double DEFAULT_Y_TO = 0.2D;
    private final Map<UUID, OrbitState> states = new ConcurrentHashMap<>();

    @Override
    public void onServerTick(Entity raw, Level level) {
        if (!(raw instanceof ScriptedEffectEntity entity)) {
            return;
        }
        if (!entity.isAlive()) {
            states.remove(entity.getUUID());
            return;
        }
        Player owner = entity.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        ScriptedEffectSpec spec = entity.getEffectSpec();
        OrbitState state = states.computeIfAbsent(entity.getUUID(), ignored -> createState(entity, owner));
        double phaseStep = parameter(spec, "phase-step", 0.18D);
        state.phase += phaseStep;
        double wobbleXz = parameter(spec, "wobble-xz", 0.03D);
        double wobbleY = parameter(spec, "wobble-y", 0.04D)
                * Math.cos(state.phase * parameter(spec, "wobble-y-freq", 1.4D)
                           + parameter(spec, "wobble-y-phase-shift", Math.PI / 3.5D));
        entity.setPos(owner.getX() + state.x + wobbleXz * Math.sin(state.phase),
                owner.getY() + state.y + wobbleY,
                owner.getZ() + state.z + wobbleXz * Math.cos(state.phase));
        entity.setYRot(owner.getYRot());
        entity.setXRot(owner.getXRot());
    }

    private static OrbitState createState(ScriptedEffectEntity entity, Player owner) {
        UUID uuid = entity.getUUID();
        RandomSource random = RandomSource.create(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
        ScriptedEffectSpec spec = entity.getEffectSpec();
        double spread = Math.PI * parameter(spec, "theta-spread-factor", 0.45D);
        double theta = Math.toRadians(-owner.getYRot()) + range(random, -spread, spread);
        double radius = range(random,
                parameter(spec, "range-from", DEFAULT_RANGE_FROM),
                parameter(spec, "range-to", DEFAULT_RANGE_TO));
        return new OrbitState(Math.sin(theta) * radius,
                range(random, parameter(spec, "y-from", DEFAULT_Y_FROM),
                        parameter(spec, "y-to", DEFAULT_Y_TO)),
                Math.cos(theta) * radius,
                range(random, 0.0D, Math.PI * 2.0D));
    }

    private static double parameter(ScriptedEffectSpec spec, String key, double fallback) {
        return spec == null ? fallback : spec.getDoubleParam(key, fallback);
    }

    private static double range(RandomSource random, double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    private static final class OrbitState {
        private final double x;
        private final double y;
        private final double z;
        private double phase;

        private OrbitState(double x, double y, double z, double phase) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.phase = phase;
        }
    }
}
