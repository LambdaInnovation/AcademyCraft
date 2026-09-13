package cn.li.mcbase.entity.spec;

public final class ScriptedBlockBodySpec {
    private final String defaultBlockId;
    private final double gravity;
    private final double damage;
    private final boolean placeWhenCollide;
    private final String rendererId;
    private final String hookId;
    private final String behaviorId;
    /** Per-tick velocity scale (1.0 = no drag); <1.0 glides to a halt. */
    private final double drag;

    public ScriptedBlockBodySpec(String defaultBlockId,
                                 double gravity,
                                 double damage,
                                 boolean placeWhenCollide,
                                 String rendererId,
                                 String hookId,
                                 String behaviorId) {
        this(defaultBlockId, gravity, damage, placeWhenCollide, rendererId, hookId, behaviorId, 1.0D);
    }

    public ScriptedBlockBodySpec(String defaultBlockId,
                                 double gravity,
                                 double damage,
                                 boolean placeWhenCollide,
                                 String rendererId,
                                 String hookId,
                                 String behaviorId,
                                 double drag) {
        this.defaultBlockId = defaultBlockId == null ? "minecraft:stone" : defaultBlockId;
        this.gravity = gravity;
        this.damage = damage;
        this.placeWhenCollide = placeWhenCollide;
        this.rendererId = rendererId == null ? "" : rendererId;
        this.hookId = hookId == null ? "" : hookId;
        this.behaviorId = behaviorId == null ? "" : behaviorId;
        this.drag = drag <= 0.0D ? 1.0D : drag;
    }

    public String getDefaultBlockId() {
        return defaultBlockId;
    }

    public double getGravity() {
        return gravity;
    }

    public double getDamage() {
        return damage;
    }

    public boolean isPlaceWhenCollide() {
        return placeWhenCollide;
    }

    public String getRendererId() {
        return rendererId;
    }

    public String getHookId() {
        return hookId;
    }

    public String getBehaviorId() {
        return behaviorId;
    }

    public double getDrag() {
        return drag;
    }
}
