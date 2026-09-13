package cn.li.mcbase.client.font.msdf;

/**
 * Client-tick driven glow breathing for MSDF text (optional UI polish).
 */
public final class MsdfGlowAnimator {

    private static volatile boolean active;
    private static volatile float baseRadius = 0.1f;
    private static volatile float amplitude = 0.05f;
    private static volatile float periodSeconds = 2.0f;
    private static volatile long startMillis;

    private MsdfGlowAnimator() {
    }

    public static void startBreathing(final float radius, final float amp, final float periodSec) {
        baseRadius = Math.max(0.0f, radius);
        amplitude = Math.max(0.0f, amp);
        periodSeconds = Math.max(0.25f, periodSec);
        startMillis = System.currentTimeMillis();
        active = true;
        MsdfTextFx.setGlowRadius(baseRadius);
    }

    public static void stop() {
        active = false;
        MsdfTextFx.setGlowRadius(0.0f);
    }

    public static boolean isActive() {
        return active;
    }

    public static void clientTick() {
        if (!active) {
            return;
        }
        final float t = (System.currentTimeMillis() - startMillis) / 1000.0f;
        final float phase = (float) (Math.sin((t / periodSeconds) * Math.PI * 2.0) * 0.5 + 0.5);
        MsdfTextFx.setGlowRadius(baseRadius + amplitude * phase);
    }
}
