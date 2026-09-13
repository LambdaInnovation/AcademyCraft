package cn.li.mcbase.entity.spec;

public final class ScriptedProjectileSpec {
    private final String defaultItemId;
    private final double gravity;
    private final double damage;
    private final double pickupDistanceSqr;
    private final boolean dropItemOnDiscard;
    private final String onHitBlockHook;
    private final String onHitEntityHook;
    private final String onAnchoredTickHook;
    private final String onAnchoredHurtHook;

    public ScriptedProjectileSpec(String defaultItemId,
                                  double gravity,
                                  double damage,
                                  double pickupDistanceSqr,
                                  boolean dropItemOnDiscard,
                                  String onHitBlockHook,
                                  String onHitEntityHook,
                                  String onAnchoredTickHook,
                                  String onAnchoredHurtHook) {
        this.defaultItemId = defaultItemId;
        this.gravity = gravity;
        this.damage = damage;
        this.pickupDistanceSqr = pickupDistanceSqr;
        this.dropItemOnDiscard = dropItemOnDiscard;
        this.onHitBlockHook = onHitBlockHook;
        this.onHitEntityHook = onHitEntityHook;
        this.onAnchoredTickHook = onAnchoredTickHook;
        this.onAnchoredHurtHook = onAnchoredHurtHook;
    }

    public String getDefaultItemId() {
        return defaultItemId;
    }

    public double getGravity() {
        return gravity;
    }

    public double getDamage() {
        return damage;
    }

    public double getPickupDistanceSqr() {
        return pickupDistanceSqr;
    }

    public boolean isDropItemOnDiscard() {
        return dropItemOnDiscard;
    }

    public String getOnHitBlockHook() {
        return onHitBlockHook;
    }

    public String getOnHitEntityHook() {
        return onHitEntityHook;
    }

    public String getOnAnchoredTickHook() {
        return onAnchoredTickHook;
    }

    public String getOnAnchoredHurtHook() {
        return onAnchoredHurtHook;
    }
}