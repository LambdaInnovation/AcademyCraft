package cn.li.mcbase.runtime;

/**
 * Central holder for runtime adapter seams installed by platform modules.
 */
public final class RuntimeAccessorRegistry {
    private static volatile ServerAccessAdapter serverAccess;
    private static volatile WorldAccessAdapter worldAccess;
    private static volatile EntityLookupAdapter entityLookup;
    private static volatile DamageSourceAdapter damageSource;

    private RuntimeAccessorRegistry() {
    }

    public static void installServerAccess(ServerAccessAdapter adapter) {
        serverAccess = adapter;
    }

    public static void installWorldAccess(WorldAccessAdapter adapter) {
        worldAccess = adapter;
    }

    public static void installEntityLookup(EntityLookupAdapter adapter) {
        entityLookup = adapter;
    }

    public static void installDamageSource(DamageSourceAdapter adapter) {
        damageSource = adapter;
    }

    public static ServerAccessAdapter getServerAccess() {
        return serverAccess;
    }

    public static WorldAccessAdapter getWorldAccess() {
        return worldAccess;
    }

    public static EntityLookupAdapter getEntityLookup() {
        return entityLookup;
    }

    public static DamageSourceAdapter getDamageSource() {
        return damageSource;
    }
}
