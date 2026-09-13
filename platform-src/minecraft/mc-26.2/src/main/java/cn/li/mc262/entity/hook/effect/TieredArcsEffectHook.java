package cn.li.mc262.entity.hook.effect;

import net.minecraft.world.entity.Entity;

import cn.li.mcbase.entity.hook.effect.ScriptedEffectHook;

import cn.li.mc262.entity.ScriptedEffectEntity;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import java.util.Iterator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public final class TieredArcsEffectHook implements ScriptedEffectHook {
    private static final double[] DEFAULT_HEIGHTS = {2.0D, 1.8D, 1.5D, 1.0D, 0.5D, 0.0D, -0.1D};
    private static final int[] DEFAULT_DELAYS = {0, 1, 3, 4, 6, 7, 8};

    @Override
    public void onClientTick(Entity raw, ClientLevel level) {
        if (!(raw instanceof ScriptedEffectEntity entity)) {
            return;
        }
        ScriptedEffectSpec spec = entity.getEffectSpec();
        double[] heights = spec == null
                ? DEFAULT_HEIGHTS
                : spec.getDoubleArrayParam("tier-heights", DEFAULT_HEIGHTS);
        int[] delays = spec == null
                ? DEFAULT_DELAYS
                : spec.getIntArrayParam("tier-delays", DEFAULT_DELAYS);
        for (int i = 0; i < Math.min(heights.length, delays.length); i++) {
            if (entity.getAgeTicks() == delays[i]) {
                spawnTier(entity, heights[i], spec);
            }
        }

        Iterator<ScriptedEffectEntity.ArcData> iterator = entity.mutableActiveArcs().iterator();
        while (iterator.hasNext()) {
            ScriptedEffectEntity.ArcData arc = iterator.next();
            float[] origin = arc.strands[0][0];
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    entity.getX() + origin[0],
                    entity.getY() + origin[1],
                    entity.getZ() + origin[2],
                    0.0D, 0.0D, 0.0D);
            if (--arc.lifeTicks <= 0) {
                iterator.remove();
            }
        }
    }

    private static void spawnTier(ScriptedEffectEntity entity, double height, ScriptedEffectSpec spec) {
        RandomSource random = entity.getEffectRandom();
        int base = spec == null ? 3 : spec.getIntParam("tier-batch-base", 3);
        int randomPart = spec == null ? 2 : spec.getIntParam("tier-batch-random", 2);
        int count = Math.max(1, base + (randomPart > 0 ? random.nextInt(randomPart) : 0));
        int life = spec == null ? 3 : spec.getIntParam("arc-life-ticks", 3);
        int mainPoints = spec == null ? 7 : Math.max(3, spec.getIntParam("main-points", 7));
        int branchBase = spec == null ? 1 : Math.max(0, spec.getIntParam("branch-count-base", 1));
        int branchRandom = spec == null ? 2 : Math.max(0, spec.getIntParam("branch-count-random", 2));
        double radiusBase = spec == null ? 0.5D : spec.getDoubleParam("tier-radius-base", 0.5D);
        double radiusRandom = spec == null ? 0.1D : spec.getDoubleParam("tier-radius-random", 0.1D);
        double originY = spec == null ? -1.0D : spec.getDoubleParam("tier-origin-offset-y", -1.0D);
        for (int n = 0; n < count; n++) {
            double theta = random.nextDouble() * Math.PI * 2.0D;
            double radius = radiusBase + random.nextDouble() * radiusRandom;
            float phase = random.nextFloat() * (float) (Math.PI * 2.0D);
            float[][] main = new float[mainPoints][3];
            for (int i = 0; i < main.length; i++) {
                float t = (float) i / (main.length - 1);
                float randomWobble = (random.nextFloat() - 0.5F) * 0.12F * (1.0F - t);
                float zigzag = (float) Math.sin(phase + i * 2.45F) * 0.055F * (1.0F - t * 0.4F);
                float wobble = randomWobble + zigzag;
                main[i][0] = (float) (Math.sin(theta) * radius * (1.0D - 0.22D * t)
                        + Math.cos(theta) * wobble);
                main[i][1] = (float) (originY + height + 0.26D * t
                        - Math.sin(t * Math.PI) * 0.12D * t);
                main[i][2] = (float) (Math.cos(theta) * radius * (1.0D - 0.22D * t)
                        - Math.sin(theta) * wobble);
            }
            int branchCount = branchBase + (branchRandom > 0 ? random.nextInt(branchRandom) : 0);
            float[][][] strands = new float[1 + branchCount][][];
            strands[0] = main;
            for (int branchIndex = 0; branchIndex < branchCount; branchIndex++) {
                int attach = 1 + random.nextInt(Math.max(1, main.length - 2));
                int branchPoints = 3 + random.nextInt(2);
                float[][] branch = new float[branchPoints][3];
                float branchTheta = (float) (theta + (random.nextBoolean() ? 1.0D : -1.0D)
                        * (0.55D + random.nextDouble() * 0.4D));
                float branchLength = 0.12F + random.nextFloat() * 0.09F;
                for (int i = 0; i < branchPoints; i++) {
                    float t = (float) i / (branchPoints - 1);
                    float wobble = (float) Math.sin(phase * 0.7F + i * 2.1F) * 0.025F;
                    branch[i][0] = main[attach][0]
                            + (float) Math.sin(branchTheta) * branchLength * t + wobble;
                    branch[i][1] = main[attach][1] + 0.05F * t + 0.03F * t * t;
                    branch[i][2] = main[attach][2]
                            + (float) Math.cos(branchTheta) * branchLength * t - wobble;
                }
                strands[branchIndex + 1] = branch;
            }
            entity.mutableActiveArcs().add(new ScriptedEffectEntity.ArcData(
                    strands,
                    Math.max(1, life),
                    phase,
                    random.nextFloat() * 13.0F));
        }
    }
}
