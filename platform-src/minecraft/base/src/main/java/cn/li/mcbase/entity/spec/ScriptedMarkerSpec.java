package cn.li.mcbase.entity.spec;

public final class ScriptedMarkerSpec {
    private final int lifeTicks;
    private final boolean followTarget;
    private final boolean ignoreDepth;
    private final boolean available;
    private final String rendererId;
    private final String hookId;

    public ScriptedMarkerSpec(int lifeTicks,
                              boolean followTarget,
                              boolean ignoreDepth,
                              boolean available,
                              String rendererId,
                              String hookId) {
        this.lifeTicks = Math.max(1, lifeTicks);
        this.followTarget = followTarget;
        this.ignoreDepth = ignoreDepth;
        this.available = available;
        this.rendererId = rendererId == null ? "" : rendererId;
        this.hookId = hookId == null ? "" : hookId;
    }

    public int getLifeTicks() {
        return lifeTicks;
    }

    public boolean isFollowTarget() {
        return followTarget;
    }

    public boolean isIgnoreDepth() {
        return ignoreDepth;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getRendererId() {
        return rendererId;
    }

    public String getHookId() {
        return hookId;
    }
}
