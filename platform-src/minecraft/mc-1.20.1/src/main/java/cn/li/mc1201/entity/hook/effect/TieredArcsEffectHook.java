package cn.li.mc1201.entity.hook.effect;

import net.minecraft.world.entity.Entity;

import cn.li.mcbase.entity.hook.effect.ScriptedEffectHook;

import cn.li.mc1201.entity.ScriptedEffectEntity;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.List;

public final class TieredArcsEffectHook implements ScriptedEffectHook {
    private static final int DEFAULT_ARC_LIFE_TICKS = 3;
    private static final int DEFAULT_TIER_BATCH_BASE = 3;
    private static final int DEFAULT_TIER_BATCH_RANDOM = 2;
    private static final double DEFAULT_TIER_RADIUS_BASE = 0.5D;
    private static final double DEFAULT_TIER_RADIUS_RANDOM = 0.1D;
    private static final double DEFAULT_TIER_THETA_MAX = Math.PI * 2.0D;
    private static final double DEFAULT_TIER_ORIGIN_OFFSET_Y = 0.0D;
    private static final int DEFAULT_BRANCH_COUNT_BASE = 1;
    private static final int DEFAULT_BRANCH_COUNT_RANDOM = 2;
    private static final int DEFAULT_BRANCH_POINTS_BASE = 3;
    private static final int DEFAULT_BRANCH_POINTS_RANDOM = 2;
    private static final float DEFAULT_SIDE_AMP_BASE = 0.06F;
    private static final float DEFAULT_SIDE_AMP_RANDOM = 0.05F;
    private static final float DEFAULT_RISE_BASE = 0.26F;
    private static final float DEFAULT_RISE_RANDOM = 0.1F;
    private static final float DEFAULT_REBOUND_BASE = 0.12F;
    private static final float DEFAULT_REBOUND_RANDOM = 0.07F;
    private static final float DEFAULT_BRANCH_THETA_BASE = 0.55F;
    private static final float DEFAULT_BRANCH_THETA_RANDOM = 0.4F;
    private static final float DEFAULT_BRANCH_LEN_BASE = 0.12F;
    private static final float DEFAULT_BRANCH_LEN_RANDOM = 0.09F;
    private static final float DEFAULT_BRANCH_WOBBLE_AMP = 0.025F;
    private static final float DEFAULT_ARC_DAMP_FACTOR = 0.65F;
    private static final float DEFAULT_ARC_WOBBLE_FREQ = 6.8F;
    private static final float DEFAULT_ARC_PULL_FACTOR = 0.22F;
    private static final float DEFAULT_BRANCH_WOBBLE_PHASE_MUL = 0.7F;
    private static final float DEFAULT_BRANCH_WOBBLE_TIME_MUL = 5.2F;
    private static final float DEFAULT_BRANCH_VEL_Y = 0.05F;
    private static final float DEFAULT_BRANCH_ACCEL_Y = 0.03F;
    private static final float DEFAULT_PHASE_MAX = (float) Math.PI * 2.0F;
    private static final float DEFAULT_FLICKER_SEED_SCALE = 13.0F;
    private static final int DEFAULT_MAIN_POINTS = 7;
    private static final int DEFAULT_BRANCH_ATTACH_START = 2;
    private static final int DEFAULT_BRANCH_ATTACH_RANDOM_SPAN_SUB = 3;

    private static final double[] DEFAULT_TIER_HEIGHTS = {2.0D, 1.8D, 1.5D, 1.0D, 0.5D, 0.0D, -0.1D};
    private static final int[] DEFAULT_TIER_DELAYS = {0, 1, 3, 4, 6, 7, 8};

    @Override
    public void onClientTick(Entity raw, ClientLevel level) {
        if (!(raw instanceof ScriptedEffectEntity entity)) {
            return;
        }
        spawnTieredArcs(entity);
        spawnArcParticles(entity, level);
        tickArcLifetimes(entity);
    }

    private void spawnTieredArcs(ScriptedEffectEntity entity) {
        int age = entity.getAgeTicks();
        ScriptedEffectSpec spec = entity.getEffectSpec();
        double[] tierHeights = spec == null ? DEFAULT_TIER_HEIGHTS : spec.getDoubleArrayParam("tier-heights", DEFAULT_TIER_HEIGHTS);
        int[] tierDelays = spec == null ? DEFAULT_TIER_DELAYS : spec.getIntArrayParam("tier-delays", DEFAULT_TIER_DELAYS);
        int tiers = Math.min(tierHeights.length, tierDelays.length);

        for (int i = 0; i < tiers; i++) {
            if (age == tierDelays[i]) {
                spawnTierAtHeight(entity, tierHeights[i]);
            }
        }
    }

    private void spawnTierAtHeight(ScriptedEffectEntity entity, double height) {
        ScriptedEffectSpec spec = entity.getEffectSpec();
        RandomSource random = entity.getEffectRandom();
        List<ScriptedEffectEntity.ArcData> arcs = entity.mutableActiveArcs();

        int batchBase = spec == null
            ? DEFAULT_TIER_BATCH_BASE
            : spec.getIntParam("tier-batch-base", DEFAULT_TIER_BATCH_BASE);
        int batchRandom = spec == null
            ? DEFAULT_TIER_BATCH_RANDOM
            : spec.getIntParam("tier-batch-random", DEFAULT_TIER_BATCH_RANDOM);
        double radiusBase = spec == null
            ? DEFAULT_TIER_RADIUS_BASE
            : spec.getDoubleParam("tier-radius-base", DEFAULT_TIER_RADIUS_BASE);
        double radiusRandom = spec == null
            ? DEFAULT_TIER_RADIUS_RANDOM
            : spec.getDoubleParam("tier-radius-random", DEFAULT_TIER_RADIUS_RANDOM);
        double tierThetaMax = spec == null
            ? DEFAULT_TIER_THETA_MAX
            : spec.getDoubleParam("tier-theta-max", DEFAULT_TIER_THETA_MAX);
        double tierOriginOffsetY = spec == null
            ? DEFAULT_TIER_ORIGIN_OFFSET_Y
            : spec.getDoubleParam("tier-origin-offset-y", DEFAULT_TIER_ORIGIN_OFFSET_Y);

        int randomBatchPart = batchRandom <= 0 ? 0 : random.nextInt(batchRandom);
        int batch = Math.max(1, batchBase + randomBatchPart);
        while (batch-- > 0) {
            double radiusJitter = radiusRandom <= 0.0D ? 0.0D : random.nextDouble() * radiusRandom;
            double radius = radiusBase + radiusJitter;
            double theta = random.nextDouble() * tierThetaMax;
            double ox = radius * Math.sin(theta);
            double oz = radius * Math.cos(theta);
            arcs.add(createArcData(entity, ox, height + tierOriginOffsetY, oz));
        }
    }

    private ScriptedEffectEntity.ArcData createArcData(ScriptedEffectEntity entity, double ox, double oy, double oz) {
        RandomSource random = entity.getEffectRandom();

        ScriptedEffectSpec spec = entity.getEffectSpec();
        float phaseMax = spec == null
            ? DEFAULT_PHASE_MAX
            : (float) spec.getDoubleParam("phase-max", DEFAULT_PHASE_MAX);
        float flickerSeedScale = spec == null
            ? DEFAULT_FLICKER_SEED_SCALE
            : (float) spec.getDoubleParam("flicker-seed-scale", DEFAULT_FLICKER_SEED_SCALE);
        float phase = random.nextFloat() * phaseMax;
        float flickerSeed = random.nextFloat() * flickerSeedScale;
        float sideAmpBase = spec == null
            ? DEFAULT_SIDE_AMP_BASE
            : (float) spec.getDoubleParam("side-amp-base", DEFAULT_SIDE_AMP_BASE);
        float sideAmpRandom = spec == null
            ? DEFAULT_SIDE_AMP_RANDOM
            : (float) spec.getDoubleParam("side-amp-random", DEFAULT_SIDE_AMP_RANDOM);
        float riseBase = spec == null
            ? DEFAULT_RISE_BASE
            : (float) spec.getDoubleParam("rise-base", DEFAULT_RISE_BASE);
        float riseRandom = spec == null
            ? DEFAULT_RISE_RANDOM
            : (float) spec.getDoubleParam("rise-random", DEFAULT_RISE_RANDOM);
        float reboundBase = spec == null
            ? DEFAULT_REBOUND_BASE
            : (float) spec.getDoubleParam("rebound-base", DEFAULT_REBOUND_BASE);
        float reboundRandom = spec == null
            ? DEFAULT_REBOUND_RANDOM
            : (float) spec.getDoubleParam("rebound-random", DEFAULT_REBOUND_RANDOM);
        float arcDampFactor = spec == null
            ? DEFAULT_ARC_DAMP_FACTOR
            : (float) spec.getDoubleParam("arc-damp-factor", DEFAULT_ARC_DAMP_FACTOR);
        float arcWobbleFreq = spec == null
            ? DEFAULT_ARC_WOBBLE_FREQ
            : (float) spec.getDoubleParam("arc-wobble-freq", DEFAULT_ARC_WOBBLE_FREQ);
        float arcPullFactor = spec == null
            ? DEFAULT_ARC_PULL_FACTOR
            : (float) spec.getDoubleParam("arc-pull-factor", DEFAULT_ARC_PULL_FACTOR);
        float branchWobblePhaseMul = spec == null
            ? DEFAULT_BRANCH_WOBBLE_PHASE_MUL
            : (float) spec.getDoubleParam("branch-wobble-phase-mul", DEFAULT_BRANCH_WOBBLE_PHASE_MUL);
        float branchWobbleTimeMul = spec == null
            ? DEFAULT_BRANCH_WOBBLE_TIME_MUL
            : (float) spec.getDoubleParam("branch-wobble-time-mul", DEFAULT_BRANCH_WOBBLE_TIME_MUL);
        float branchVelY = spec == null
            ? DEFAULT_BRANCH_VEL_Y
            : (float) spec.getDoubleParam("branch-vel-y", DEFAULT_BRANCH_VEL_Y);
        float branchAccelY = spec == null
            ? DEFAULT_BRANCH_ACCEL_Y
            : (float) spec.getDoubleParam("branch-accel-y", DEFAULT_BRANCH_ACCEL_Y);
        int mainPoints = spec == null
            ? DEFAULT_MAIN_POINTS
            : spec.getIntParam("main-points", DEFAULT_MAIN_POINTS);
        mainPoints = Math.max(2, mainPoints);

        float[][] main = new float[mainPoints][3];
        float baseTheta = (float) Math.atan2(ox, oz);
        float sideAmpRandomPart = sideAmpRandom <= 0.0F ? 0.0F : random.nextFloat() * sideAmpRandom;
        float sideAmp = sideAmpBase + sideAmpRandomPart;
        float riseRandomPart = riseRandom <= 0.0F ? 0.0F : random.nextFloat() * riseRandom;
        float rise = riseBase + riseRandomPart;
        float reboundRandomPart = reboundRandom <= 0.0F ? 0.0F : random.nextFloat() * reboundRandom;
        float rebound = reboundBase + reboundRandomPart;

        for (int i = 0; i < mainPoints; i++) {
            float t = (float) i / (float) (mainPoints - 1);
            float damp = 1.0F - (arcDampFactor * t);
            float wobble = (float) Math.sin((t * arcWobbleFreq) + phase) * sideAmp * damp;
            float reboundCurve = (float) Math.sin(t * Math.PI) * rebound;
            float y = (float) (oy + (rise * t) - (reboundCurve * t));

            float radialX = (float) Math.sin(baseTheta);
            float radialZ = (float) Math.cos(baseTheta);
            float tangentX = radialZ;
            float tangentZ = -radialX;

            float pull = 1.0F - (arcPullFactor * t);
            main[i][0] = (float) (ox * pull + tangentX * wobble);
            main[i][1] = y;
            main[i][2] = (float) (oz * pull + tangentZ * wobble);
        }

        int branchCountBase = spec == null
            ? DEFAULT_BRANCH_COUNT_BASE
            : spec.getIntParam("branch-count-base", DEFAULT_BRANCH_COUNT_BASE);
        int branchCountRandom = spec == null
            ? DEFAULT_BRANCH_COUNT_RANDOM
            : spec.getIntParam("branch-count-random", DEFAULT_BRANCH_COUNT_RANDOM);
        int branchPointsBase = spec == null
            ? DEFAULT_BRANCH_POINTS_BASE
            : spec.getIntParam("branch-points-base", DEFAULT_BRANCH_POINTS_BASE);
        int branchPointsRandom = spec == null
            ? DEFAULT_BRANCH_POINTS_RANDOM
            : spec.getIntParam("branch-points-random", DEFAULT_BRANCH_POINTS_RANDOM);

        int randomBranchPart = branchCountRandom <= 0 ? 0 : random.nextInt(branchCountRandom);
        int branchCount = Math.max(0, branchCountBase + randomBranchPart);
        float[][][] strands = new float[1 + branchCount][][];
        strands[0] = main;

        float branchThetaBase = spec == null
            ? DEFAULT_BRANCH_THETA_BASE
            : (float) spec.getDoubleParam("branch-theta-base", DEFAULT_BRANCH_THETA_BASE);
        float branchThetaRandom = spec == null
            ? DEFAULT_BRANCH_THETA_RANDOM
            : (float) spec.getDoubleParam("branch-theta-random", DEFAULT_BRANCH_THETA_RANDOM);
        float branchLenBase = spec == null
            ? DEFAULT_BRANCH_LEN_BASE
            : (float) spec.getDoubleParam("branch-len-base", DEFAULT_BRANCH_LEN_BASE);
        float branchLenRandom = spec == null
            ? DEFAULT_BRANCH_LEN_RANDOM
            : (float) spec.getDoubleParam("branch-len-random", DEFAULT_BRANCH_LEN_RANDOM);
        float branchWobbleAmp = spec == null
            ? DEFAULT_BRANCH_WOBBLE_AMP
            : (float) spec.getDoubleParam("branch-wobble-amp", DEFAULT_BRANCH_WOBBLE_AMP);
        int branchAttachStart = spec == null
            ? DEFAULT_BRANCH_ATTACH_START
            : spec.getIntParam("branch-attach-start", DEFAULT_BRANCH_ATTACH_START);
        int branchAttachRandomSpanSub = spec == null
            ? DEFAULT_BRANCH_ATTACH_RANDOM_SPAN_SUB
            : spec.getIntParam("branch-attach-random-span-sub", DEFAULT_BRANCH_ATTACH_RANDOM_SPAN_SUB);

        for (int b = 0; b < branchCount; b++) {
            int attachSpan = Math.max(1, mainPoints - branchAttachRandomSpanSub);
            int attachIdx = Math.min(mainPoints - 1, Math.max(0, branchAttachStart + random.nextInt(attachSpan)));
            float[] attach = main[attachIdx];
            int randomPointsPart = branchPointsRandom <= 0 ? 0 : random.nextInt(branchPointsRandom);
            int branchPoints = Math.max(2, branchPointsBase + randomPointsPart);
            float[][] branch = new float[branchPoints][3];

            float branchThetaRandomPart = branchThetaRandom <= 0.0F ? 0.0F : random.nextFloat() * branchThetaRandom;
            float branchTheta = baseTheta + ((b == 0 ? 1.0F : -1.0F) * (branchThetaBase + branchThetaRandomPart));
            float branchLenRandomPart = branchLenRandom <= 0.0F ? 0.0F : random.nextFloat() * branchLenRandom;
            float branchLen = branchLenBase + branchLenRandomPart;

            for (int i = 0; i < branchPoints; i++) {
                float t = (float) i / (float) (branchPoints - 1);
                float forkWobble = (float) Math.sin((phase * branchWobblePhaseMul) + (t * branchWobbleTimeMul)) * branchWobbleAmp;
                branch[i][0] = attach[0] + (float) Math.sin(branchTheta) * branchLen * t + forkWobble;
                branch[i][1] = attach[1] + (branchVelY * t) - (branchAccelY * t * t);
                branch[i][2] = attach[2] + (float) Math.cos(branchTheta) * branchLen * t - forkWobble;
            }

            strands[1 + b] = branch;
        }

        int arcLifeTicks = entity.getEffectSpec() == null
            ? DEFAULT_ARC_LIFE_TICKS
            : entity.getEffectSpec().getIntParam("arc-life-ticks", DEFAULT_ARC_LIFE_TICKS);
        return new ScriptedEffectEntity.ArcData(strands, arcLifeTicks, phase, flickerSeed);
    }

    private void tickArcLifetimes(ScriptedEffectEntity entity) {
        Iterator<ScriptedEffectEntity.ArcData> it = entity.mutableActiveArcs().iterator();
        while (it.hasNext()) {
            ScriptedEffectEntity.ArcData arc = it.next();
            arc.lifeTicks--;
            if (arc.lifeTicks <= 0) {
                it.remove();
            }
        }
    }

    private void spawnArcParticles(ScriptedEffectEntity entity, ClientLevel level) {
        for (ScriptedEffectEntity.ArcData arc : entity.mutableActiveArcs()) {
            float[] p0 = arc.strands[0][0];
            double px = entity.getX() + p0[0];
            double py = entity.getY() + p0[1];
            double pz = entity.getZ() + p0[2];
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0.0, 0.0, 0.0);

            for (int i = 1; i < arc.strands.length; i++) {
                float[][] branch = arc.strands[i];
                float[] tip = branch[branch.length - 1];
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        entity.getX() + tip[0], entity.getY() + tip[1], entity.getZ() + tip[2],
                        0.0, 0.0, 0.0);
            }
        }
    }
}
