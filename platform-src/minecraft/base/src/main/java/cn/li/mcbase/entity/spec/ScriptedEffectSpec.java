package cn.li.mcbase.entity.spec;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ScriptedEffectSpec {
    private final int lifeTicks;
    private final boolean followOwner;
    private final String rendererId;
    private final String effectHook;
    private final Map<String, Object> hookParams;

    public ScriptedEffectSpec(int lifeTicks, boolean followOwner, String effectHook) {
        this(lifeTicks, followOwner, "effect-billboard", effectHook, Collections.emptyMap());
    }

    public ScriptedEffectSpec(int lifeTicks, boolean followOwner, String rendererId, String effectHook) {
        this(lifeTicks, followOwner, rendererId, effectHook, Collections.emptyMap());
    }

    public ScriptedEffectSpec(int lifeTicks,
                              boolean followOwner,
                              String rendererId,
                              String effectHook,
                              Map<String, Object> hookParams) {
        this.lifeTicks = Math.max(1, lifeTicks);
        this.followOwner = followOwner;
        this.rendererId = rendererId == null ? "" : rendererId;
        this.effectHook = effectHook == null ? "" : effectHook;
        this.hookParams = hookParams == null ? Collections.emptyMap() : Collections.unmodifiableMap(hookParams);
    }

    public int getLifeTicks() {
        return lifeTicks;
    }

    public boolean isFollowOwner() {
        return followOwner;
    }

    public String getRendererId() {
        return rendererId;
    }

    public String getEffectHook() {
        return effectHook;
    }

    public Map<String, Object> getHookParams() {
        return hookParams;
    }

    public double getDoubleParam(String key, double defaultValue) {
        Object value = hookParams.get(key);
        return value instanceof Number number ? number.doubleValue() : defaultValue;
    }

    public int getIntParam(String key, int defaultValue) {
        Object value = hookParams.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    public String getStringParam(String key, String defaultValue) {
        Object value = hookParams.get(key);
        return value instanceof String string ? string : defaultValue;
    }

    public double[] getDoubleArrayParam(String key, double[] defaultValue) {
        Object value = hookParams.get(key);
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return defaultValue;
        }

        double[] result = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Number number)) {
                return defaultValue;
            }
            result[i] = number.doubleValue();
        }
        return result;
    }

    public int[] getIntArrayParam(String key, int[] defaultValue) {
        Object value = hookParams.get(key);
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return defaultValue;
        }

        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Number number)) {
                return defaultValue;
            }
            result[i] = number.intValue();
        }
        return result;
    }
}
