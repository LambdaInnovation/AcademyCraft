package cn.li.mc262.client.render.effect;

import cn.li.mc262.entity.ScriptedEffectEntity;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import cn.li.mcver.ResourceLocations;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

public final class ScriptedEffectBillboardRenderer<T extends Entity>
        extends AbstractScriptedGeometryRenderer<T> {
    public ScriptedEffectBillboardRenderer(EntityRendererProvider.Context context) {
        this(context, "effect-billboard");
    }

    public ScriptedEffectBillboardRenderer(EntityRendererProvider.Context context, String rendererId) {
        super(context, rendererId);
    }

    @Override
    public void extractRenderState(T entity, ScriptedEntityRenderState<T> state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ScriptedEffectSpec spec = ScriptedRenderAccess.getEffectSpec(entity);
        // The spec's life is not the life this instance dies at: spawn-time
        // overrides (ElectronBomb's 20/5-tick ball, ScatterBomb's 240) are
        // synced on the entity, and curves expressed as a fraction of life
        // read wrong without them.
        state.lifeTicks = entity instanceof ScriptedEffectEntity effect
                ? effect.getEffectiveLifeTicks()
                : (spec == null ? 15 : spec.getLifeTicks());
        state.activeArcs = ScriptedRenderAccess.getActiveArcs(entity);
    }

    @Override
    public void submit(ScriptedEntityRenderState<T> state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        switch (state.rendererKey) {
            case "ring-lines" -> submitRings(state, poseStack, collector);
            case "polyline-arc" -> submitPolyline(state, poseStack, collector);
            case "billboard-cross" -> submitCross(state, poseStack, collector, camera);
            case "animated-billboard" -> submitAnimatedBillboard(state, poseStack, collector, camera);
            case "md-ball" -> submitMdBall(state, poseStack, collector, camera);
            case "spinning-double-sided" -> submitSpinningCoin(state, poseStack, collector);
            case "tiered-zigzag" -> TieredZigzagArcRenderer.submit(state, poseStack, collector);
            case "spinning-shield" -> submitShield(state, poseStack, collector, false);
            case "diamond-pyramid" -> submitShield(state, poseStack, collector, true);
            default -> throw new IllegalArgumentException(
                    "Unsupported mc-26.2 scripted effect renderer: " + state.rendererId
                            + " (key=" + state.rendererKey + ")");
        }
    }

    private static void submitCross(ScriptedEntityRenderState<?> state, PoseStack stack,
                                    SubmitNodeCollector collector, CameraRenderState camera) {
        float alpha = lifeAlpha(state);
        float size = Math.max(0.01F, planFloat(state.rendererId, "size", 0.6F));
        int r = Mth.clamp(planInt(state.rendererId, "color-r", 180), 0, 255);
        int g = Mth.clamp(planInt(state.rendererId, "color-g", 220), 0, 255);
        int b = Mth.clamp(planInt(state.rendererId, "color-b", 255), 0, 255);
        int a = (int) (255.0F * alpha);
        stack.pushPose();
        stack.mulPose(camera.orientation);
        collector.submitCustomGeometry(stack, RenderTypes.LINES_TRANSLUCENT, (pose, vc) -> {
            Matrix4f matrix = pose.pose();
            line(vc, matrix, -size, 0, 0, size, 0, 0, r, g, b, a);
            line(vc, matrix, 0, -size, 0, 0, size, 0, r, g, b, a);
        });
        stack.popPose();
    }

    private static void submitRings(ScriptedEntityRenderState<?> state, PoseStack stack,
                                    SubmitNodeCollector collector) {
        collector.submitCustomGeometry(stack, RenderTypes.LINES_TRANSLUCENT, (pose, vc) -> {
            Matrix4f matrix = pose.pose();
            for (int layer = 0; layer < 3; layer++) {
                float cycle = (state.ageTicks + state.partialTick + layer * 1.2F) % 3.6F;
                float radius = 0.4F + cycle * 0.5F;
                int alpha = (int) (200.0F * (1.0F - cycle / 3.6F));
                for (int i = 0; i < 16; i++) {
                    double a0 = Math.PI * 2.0D * i / 16.0D;
                    double a1 = Math.PI * 2.0D * (i + 1) / 16.0D;
                    line(vc, matrix,
                            (float) Math.cos(a0) * radius, 0.02F, (float) Math.sin(a0) * radius,
                            (float) Math.cos(a1) * radius, 0.02F, (float) Math.sin(a1) * radius,
                            145, 95, 255, alpha);
                }
            }
        });
    }

    private static void submitPolyline(ScriptedEntityRenderState<?> state, PoseStack stack,
                                       SubmitNodeCollector collector) {
        float progress = Mth.clamp((state.ageTicks + state.partialTick)
                / Math.max(1.0F, state.lifeTicks), 0.0F, 1.0F);
        int segments = Mth.clamp(planInt(state.rendererId, "segments", 20), 2, 80);
        float length = Math.max(0.1F, planFloat(state.rendererId, "length", 20.0F));
        float amplitude = Math.max(0.0F, planFloat(state.rendererId, "wiggle-amp", 0.5F));
        float frequency = Math.max(0.0F, planFloat(state.rendererId, "wiggle-freq", 7.0F));
        int alpha = (int) (255.0F * Math.min(Mth.clamp(progress / 0.2F, 0, 1),
                Mth.clamp((1.0F - progress) / 0.2F, 0, 1)));
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        stack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        collector.submitCustomGeometry(stack, RenderTypes.LINES_TRANSLUCENT, (pose, vc) -> {
            Matrix4f matrix = pose.pose();
            float px = 0, py = 0, pz = 0;
            for (int i = 1; i <= segments; i++) {
                float t = (float) i / segments;
                float wave = state.entityId * 0.37F + state.ageInTicks * 0.35F + t * frequency;
                float x = (float) Math.sin(wave) * amplitude * 0.2F;
                float y = (float) Math.cos(wave * 1.17F) * amplitude * 0.12F;
                float z = length * t;
                line(vc, matrix, px, py, pz, x, y, z, 145, 210, 255, alpha);
                px = x; py = y; pz = z;
            }
        });
        stack.popPose();
    }

    private static void submitAnimatedBillboard(ScriptedEntityRenderState<?> state, PoseStack stack,
                                                SubmitNodeCollector collector, CameraRenderState camera) {
        String prefix = planParamString(state.rendererId, "texture-prefix", "");
        int frames = Math.max(1, planInt(state.rendererId, "frame-count", 1));
        float frameMs = Math.max(1.0F, planFloat(state.rendererId, "frame-ms", 50.0F));
        int frame = Mth.clamp((int) (((state.ageTicks + state.partialTick) * 50.0F) / frameMs), 0, frames - 1);
        Identifier texture = parseTexture(prefix + frame + ".png");
        if (texture == null) return;
        float half = Math.max(0.01F, planFloat(state.rendererId, "half-size", 0.5F));
        stack.pushPose();
        stack.translate(0.0F, planFloat(state.rendererId, "offset-y", 0.0F),
                planFloat(state.rendererId, "offset-z", 0.0F));
        stack.mulPose(camera.orientation);
        submitQuad(stack, collector, texture, half, half, 255);
        stack.popPose();
    }

    /**
     * EntityMdBall.R: a soft glow quad behind a brighter core quad, with the
     * alpha model from EntityMdBall.getAlpha and the random texture/alpha
     * flicker from updateRenderTick. The generic animated-billboard kind draws
     * one opaque quad on a fixed frame cycle, which is neither.
     */
    private static void submitMdBall(ScriptedEntityRenderState<?> state, PoseStack stack,
                                     SubmitNodeCollector collector, CameraRenderState camera) {
        String prefix = planParamString(state.rendererId, "texture-prefix", "");
        String glowId = planParamString(state.rendererId, "glow-texture", "");
        int frames = Math.max(1, planInt(state.rendererId, "frame-count", 1));
        float glowSize = planFloat(state.rendererId, "glow-size-factor", 0.35F);
        float coreSize = planFloat(state.rendererId, "core-size-factor", 0.25F);
        float holdAlpha = planFloat(state.rendererId, "alpha-hold", 0.6F);
        float attackSeconds = Math.max(0.01F, planFloat(state.rendererId, "alpha-attack-seconds", 0.3F));
        float burstSeconds = planFloat(state.rendererId, "alpha-burst-seconds", 0.4F);
        float blendSeconds = Math.max(0.01F, planFloat(state.rendererId, "alpha-blend-seconds", 0.15F));

        float ageTicks = state.ageTicks + state.partialTick;
        float ageSeconds = ageTicks * 0.05F;
        float lifeSeconds = state.lifeTicks * 0.05F;
        float alpha = mdBallAlpha(ageSeconds, lifeSeconds, holdAlpha, attackSeconds,
                burstSeconds, blendSeconds);
        float size = mdBallSize(ageSeconds, lifeSeconds);

        // updateRenderTick(): the texture is re-rolled on roughly a quarter of
        // frames and alphaWiggle random-walks in [0, 1]. Both are driven off a
        // per-entity hash so the renderer stays stateless while still
        // flickering rather than cycling.
        long flickerStep = (long) (ageTicks * 3.0F);
        int frame = Math.floorMod(hashNoise(state.entityId, flickerStep), frames);
        float wiggle = Math.floorMod(hashNoise(state.entityId * 31, flickerStep + 7L), 1000) / 1000.0F;

        Identifier core = parseTexture(prefix + frame + ".png");
        Identifier glow = parseTexture(glowId);
        if (core == null) return;

        stack.pushPose();
        stack.mulPose(camera.orientation);
        if (glow != null) {
            submitMdBallQuad(stack, collector, glow, glowSize * size, alpha * (0.3F + wiggle * 0.7F));
        }
        submitMdBallQuad(stack, collector, core, coreSize * size, alpha * (0.8F + 0.2F * wiggle));
        stack.popPose();
    }


    /**
     * EntityMdBall.getAlpha(), all four branches. The port only had the attack
     * ramp and the hold, which is all a long-lived ball ever shows -- but
     * ElectronBomb's ball lives 20 ticks (1.0s) and its improved form 5 ticks
     * (0.25s), where the rest of the curve is the whole point: the ball flares
     * from 0.6 to full over the 0.25s before it dies and then drops to nothing
     * in the last 0.15s.
     *
     * The branch order is upstream's and matters. Nothing clamps the burst
     * ramp, so a ball whose life is shorter than burstTime never reaches the
     * attack branch at all -- the 5-tick one opens at 0.84 and climbs.
     */
    private static float mdBallAlpha(float ageSeconds, float lifeSeconds, float hold,
                                     float attackSeconds, float burstSeconds, float blendSeconds) {
        if (ageSeconds > lifeSeconds - blendSeconds) {
            return Math.max(0.0F, 1.0F - (ageSeconds - (lifeSeconds - blendSeconds)) / blendSeconds);
        }
        if (ageSeconds > lifeSeconds - burstSeconds) {
            float t = (ageSeconds - (lifeSeconds - burstSeconds)) / (burstSeconds - blendSeconds);
            return hold + (1.0F - hold) * t;
        }
        if (ageSeconds < attackSeconds) {
            return hold * (ageSeconds / attackSeconds);
        }
        return hold;
    }

    /**
     * EntityMdBall.getSize(), all three branches: the ball holds at 1.0 for
     * most of its life, swells to 1.5 over the 200 ms before its last 100 ms,
     * and collapses to nothing over those final 100 ms. The port kept the
     * size constant and faded only the alpha, so a dying ball read as being
     * wiped out of existence at full size instead of collapsing in on itself.
     */
    private static float mdBallSize(float ageSeconds, float lifeSeconds) {
        if (ageSeconds > lifeSeconds - 0.1F) {
            float t = (ageSeconds - (lifeSeconds - 0.1F)) / 0.1F;
            return Math.max(0.0F, 1.5F * (1.0F - t));
        }
        if (ageSeconds > lifeSeconds - 0.3F) {
            float t = (ageSeconds - (lifeSeconds - 0.3F)) / 0.2F;
            return 1.0F + 0.5F * t;
        }
        return 1.0F;
    }

    private static int hashNoise(int a, long b) {
        long h = a * 0x9E3779B97F4A7C15L ^ (b + 0x165667B19E3779F9L);
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return (int) h;
    }

    private static void submitMdBallQuad(PoseStack stack, SubmitNodeCollector collector,
                                         Identifier texture, float halfSize, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        if (a <= 0) return;
        // RenderIcon's quad spans y in [-0.25, +0.75] of its size, i.e. it sits
        // a quarter of its height above the entity position.
        float bottom = -halfSize * 0.5F;
        float top = halfSize * 1.5F;
        collector.submitCustomGeometry(stack, RenderTypes.entityTranslucent(texture), (pose, vc) -> {
            Matrix4f m = pose.pose();
            vertex(vc, pose, m, -halfSize, bottom, 0, 0, 1, a);
            vertex(vc, pose, m, halfSize, bottom, 0, 1, 1, a);
            vertex(vc, pose, m, halfSize, top, 0, 1, 0, a);
            vertex(vc, pose, m, -halfSize, top, 0, 0, 0, a);
        });
    }

    private static void submitSpinningCoin(ScriptedEntityRenderState<?> state, PoseStack stack,
                                           SubmitNodeCollector collector) {
        Identifier front = parseTexture(planParamString(state.rendererId, "front-texture", ""));
        Identifier back = parseTexture(planParamString(state.rendererId, "back-texture", ""));
        if (front == null || back == null) return;
        float scale = Math.max(0.01F, planFloat(state.rendererId, "scale", 0.3F));
        float period = Math.max(1.0F, planFloat(state.rendererId, "rotation-period-ms", 300.0F));
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        stack.translate(planFloat(state.rendererId, "offset-x", -0.63F),
                planFloat(state.rendererId, "offset-y", 1.0F),
                planFloat(state.rendererId, "offset-z", 0.3F));
        stack.scale(scale, scale, scale);
        stack.mulPose(Axis.YP.rotation((state.ageTicks + state.partialTick) * 50.0F
                * ((float) Math.PI * 2.0F / period)));
        submitQuad(stack, collector, front, 0.5F, 0.5F, 255);
        stack.mulPose(Axis.YP.rotationDegrees(180.0F));
        submitQuad(stack, collector, back, 0.5F, 0.5F, 255);
        stack.popPose();
    }


    // RenderMdShield accumulates `rotation += lerpf(0.8, 2, min(ticks/30, 1)) * dt`
    // with dt in SECONDS, i.e. 0.04 -> 0.1 degrees per tick over the first 30
    // ticks and 0.1 from then on. This is that ramp's integral.
    private static float shieldSpinDegrees(float age) {
        float s1 = 0.1F;
        float s0 = s1 * 0.4F;
        float r = 30.0F;
        float degrees = age <= r
                ? s0 * age + (s1 - s0) * age * age / (2.0F * r)
                : s0 * r + (s1 - s0) * r / 2.0F + s1 * (age - r);
        return degrees % 360.0F;
    }

    private static void submitShield(ScriptedEntityRenderState<?> state, PoseStack stack,
                                     SubmitNodeCollector collector, boolean pyramid) {
        Identifier texture = parseTexture(planParamString(state.rendererId, "texture", ""));
        if (texture == null) return;
        float age = state.ageTicks + state.partialTick;
        float scale;
        int alpha;
        if (pyramid) {
            // RenderDiamondShield never reads ticksExisted: flat glScalef(1.5),
            // opaque, no growth and no fade-in. (EntityDiamondShield.SIZE = 1.8
            // is its bounding box, not its render scale.) It also draws with
            // depth test off, which this platform cannot express without a
            // custom pipeline -- left depth-tested for now.
            scale = Math.max(0.01F, planFloat(state.rendererId, "scale", 1.5F));
            alpha = 255;
        } else {
            scale = Math.max(0.01F, planFloat(state.rendererId, "scale", 1.8F))
                    * (0.2F + 0.8F * Mth.clamp(age / 15.0F, 0, 1));
            alpha = (int) (255.0F * Mth.clamp(age / 6.0F, 0, 1));
        }
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(-state.yRot));
        stack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        stack.scale(scale, scale, scale);
        if (!pyramid) {
            stack.mulPose(Axis.ZP.rotationDegrees(shieldSpinDegrees(age)));
            submitQuad(stack, collector, texture, 0.5F, 0.5F, alpha);
        } else {
            collector.submitCustomGeometry(stack, RenderTypes.entityTranslucent(texture), (pose, vc) -> {
                Matrix4f m = pose.pose();
                // Upstream mesh UVs: {0,0}{1,1}{0,0}{1,1} on the rim and {0,1}
                // on the shared apex, so the texture runs continuously across
                // the faces and the pyramid reads as one gem.
                float[][] rim = {{-1,0,0},{0,-1,0},{1,0,0},{0,1,0}};
                float[][] rimUv = {{0,0},{1,1},{0,0},{1,1}};
                for (int i = 0; i < 4; i++) {
                    int j = (i + 1) % 4;
                    vertex(vc, pose, m, rim[i][0], rim[i][1], 0, rimUv[i][0], rimUv[i][1], alpha);
                    vertex(vc, pose, m, rim[j][0], rim[j][1], 0, rimUv[j][0], rimUv[j][1], alpha);
                    vertex(vc, pose, m, 0, 0, 1, 0, 1, alpha);
                    vertex(vc, pose, m, 0, 0, 1, 0, 1, alpha);
                }
            });
        }
        stack.popPose();
    }

    private static void submitQuad(PoseStack stack, SubmitNodeCollector collector,
                                   Identifier texture, float halfWidth, float halfHeight, int alpha) {
        collector.submitCustomGeometry(stack, RenderTypes.entityTranslucent(texture), (pose, vc) -> {
            Matrix4f m = pose.pose();
            vertex(vc, pose, m, -halfWidth, -halfHeight, 0, 0, 1, alpha);
            vertex(vc, pose, m, halfWidth, -halfHeight, 0, 1, 1, alpha);
            vertex(vc, pose, m, halfWidth, halfHeight, 0, 1, 0, alpha);
            vertex(vc, pose, m, -halfWidth, halfHeight, 0, 0, 0, alpha);
        });
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose pose,
                               Matrix4f matrix, float x, float y, float z, float u, float v, int alpha) {
        vc.addVertex(matrix, x, y, z).setColor(255, 255, 255, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0)
                .setNormal(pose, 0, 0, 1);
    }

    private static Identifier parseTexture(String id) {
        if (id == null || id.isBlank()) return null;
        try {
            return ResourceLocations.parse(id);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static float lifeAlpha(ScriptedEntityRenderState<?> state) {
        return Mth.clamp(1.0F - (state.ageTicks + state.partialTick)
                / Math.max(1.0F, state.lifeTicks), 0.0F, 1.0F);
    }
}
