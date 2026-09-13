package cn.li.mc1201.client.render.effect;

import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ScriptedRenderAccess {

    private ScriptedRenderAccess() {
    }

    static ScriptedEffectSpec getEffectSpec(Entity entity) {
        return ScriptedEntitySpecAccess.getScriptedEffectSpec(entity.getType());
    }

    static ScriptedMarkerSpec getMarkerSpec(Entity entity) {
        return ScriptedEntitySpecAccess.getScriptedMarkerSpec(entity.getType());
    }

    static ScriptedRaySpec getRaySpec(Entity entity) {
        return ScriptedEntitySpecAccess.getScriptedRaySpec(entity.getType());
    }

    static int getAgeTicks(Entity entity) {
        return invokeInt(entity, "getAgeTicks", 0);
    }

    static int getEffectiveLifeTicks(Entity entity, int defaultValue) {
        return invokeInt(entity, "getEffectiveLifeTicks", defaultValue);
    }

    static String getSyncedBlockId(Entity entity) {
        return invokeString(entity, "getSyncedBlockId", "minecraft:stone");
    }

    static boolean isBehaviorHit(Entity entity) {
        return invokeBoolean(entity, "isBehaviorHit", false);
    }

    private static boolean invokeBoolean(Object target, String methodName, boolean defaultValue) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result instanceof Boolean b ? b : defaultValue;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    static List<ArcDataView> getActiveArcs(Entity entity) {
        List<?> arcs = invokeList(entity, "getActiveArcs");
        if (arcs.isEmpty()) {
            return Collections.emptyList();
        }
        List<ArcDataView> out = new ArrayList<>(arcs.size());
        for (Object arc : arcs) {
            ArcDataView view = ArcDataView.from(arc);
            if (view != null) {
                out.add(view);
            }
        }
        return out;
    }

    private static int invokeInt(Object target, String methodName, int defaultValue) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result instanceof Number n ? n.intValue() : defaultValue;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String invokeString(Object target, String methodName, String defaultValue) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result == null ? defaultValue : result.toString();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static List<?> invokeList(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result instanceof List<?> list ? list : Collections.emptyList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    static final class ArcDataView {
        final float[][][] strands;
        final int lifeTicks;
        final float phase;
        final float flickerSeed;

        private ArcDataView(float[][][] strands, int lifeTicks, float phase, float flickerSeed) {
            this.strands = strands;
            this.lifeTicks = lifeTicks;
            this.phase = phase;
            this.flickerSeed = flickerSeed;
        }

        static ArcDataView from(Object arcObj) {
            try {
                Field strandsField = arcObj.getClass().getField("strands");
                Field lifeTicksField = arcObj.getClass().getField("lifeTicks");
                Field phaseField = arcObj.getClass().getField("phase");
                Field flickerSeedField = arcObj.getClass().getField("flickerSeed");

                Object strandsObj = strandsField.get(arcObj);
                Object lifeObj = lifeTicksField.get(arcObj);
                Object phaseObj = phaseField.get(arcObj);
                Object seedObj = flickerSeedField.get(arcObj);

                if (!(strandsObj instanceof float[][][] strands)) {
                    return null;
                }
                int lifeTicks = lifeObj instanceof Number n ? n.intValue() : 0;
                float phase = phaseObj instanceof Number n ? n.floatValue() : 0.0F;
                float flickerSeed = seedObj instanceof Number n ? n.floatValue() : 0.0F;
                return new ArcDataView(strands, lifeTicks, phase, flickerSeed);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
