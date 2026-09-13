package cn.li.mcbase.entity.spec;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ScriptedRaySpec {
    private final int lifeTicks;
    private final double length;
    private final double blendInMs;
    private final double blendOutMs;
    private final double innerWidth;
    private final double outerWidth;
    private final double glowWidth;
    private final int startColor;
    private final int endColor;
    private final String rendererId;
    private final String hookId;
    private final Map<String, Object> hookParams;

    public ScriptedRaySpec(int lifeTicks,
                           double length,
                           double blendInMs,
                           double blendOutMs,
                           double innerWidth,
                           double outerWidth,
                           double glowWidth,
                           int startColor,
                           int endColor,
                           String rendererId,
                           String hookId) {
        this(lifeTicks,
            length,
            blendInMs,
            blendOutMs,
            innerWidth,
            outerWidth,
            glowWidth,
            startColor,
            endColor,
            rendererId,
            hookId,
            Collections.emptyMap());
    }

    public ScriptedRaySpec(int lifeTicks,
                           double length,
                           double blendInMs,
                           double blendOutMs,
                           double innerWidth,
                           double outerWidth,
                           double glowWidth,
                           int startColor,
                           int endColor,
                           String rendererId,
                           String hookId,
                           Map<String, Object> hookParams) {
        this.lifeTicks = Math.max(1, lifeTicks);
        this.length = Math.max(0.0D, length);
        this.blendInMs = Math.max(0.0D, blendInMs);
        this.blendOutMs = Math.max(0.0D, blendOutMs);
        this.innerWidth = Math.max(0.001D, innerWidth);
        this.outerWidth = Math.max(0.001D, outerWidth);
        this.glowWidth = Math.max(0.001D, glowWidth);
        this.startColor = startColor;
        this.endColor = endColor;
        this.rendererId = rendererId == null ? "" : rendererId;
        this.hookId = hookId == null ? "" : hookId;
        this.hookParams = hookParams == null ? Collections.emptyMap() : Collections.unmodifiableMap(hookParams);
    }

    public int getLifeTicks() {
        return lifeTicks;
    }

    public double getLength() {
        return length;
    }

    public double getBlendInMs() {
        return blendInMs;
    }

    public double getBlendOutMs() {
        return blendOutMs;
    }

    public double getInnerWidth() {
        return innerWidth;
    }

    public double getOuterWidth() {
        return outerWidth;
    }

    public double getGlowWidth() {
        return glowWidth;
    }

    public int getStartColor() {
        return startColor;
    }

    public int getEndColor() {
        return endColor;
    }

    public String getRendererId() {
        return rendererId;
    }

    public String getHookId() {
        return hookId;
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
