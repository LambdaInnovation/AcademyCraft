package cn.li.mc1201.entity;

import cn.li.mcbase.entity.IScriptedEffectEntity;


import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcbase.entity.hook.effect.OwnerOffsetEffectHook;
import cn.li.mc1201.entity.hook.effect.OwnerOrbitEffectHook;
import cn.li.mcbase.entity.hook.effect.ScriptedEffectHook;
import cn.li.mcbase.entity.hook.effect.ScriptedEffectHooks;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import clojure.lang.IFn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ScriptedEffectEntity extends Entity implements IScriptedEffectEntity {
    private static final int BALLISTIC_MAX_LIFE = 120;
    private static final String LIFE_TICKS_OVERRIDE_TAG = "lifeTicksOverride";
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
        SynchedEntityData.defineId(ScriptedEffectEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_LIFE_TICKS_OVERRIDE =
        SynchedEntityData.defineId(ScriptedEffectEntity.class, EntityDataSerializers.INT);
    private UUID ownerUuid;
    private int age;
    private int lifeTicksOverride = -1;
    private final List<ArcData> activeArcs = new ArrayList<>();
    private boolean ballisticStateInitialized;
    private double ballisticCurrentY;
    private double ballisticVelY;
    private double ballisticStartY;
    private double ballisticMaxY;
    private double ballisticInitVel = 0.92D;

    public ScriptedEffectEntity(EntityType<? extends ScriptedEffectEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static ScriptedEffectEntity create(Level level, Player owner, String entityRegistryName) {
        ScriptedEffectEntity entity = new ScriptedEffectEntity(
            ScriptedEntitySpecAccess.requireEntityType(entityRegistryName, ScriptedEffectEntity.class),
                level
        );
        entity.setOwnerPlayer(owner);
        entity.setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
        return entity;
    }

    private ScriptedEffectSpec getSpec() {
        return ScriptedEntitySpecAccess.getScriptedEffectSpec(this.getType());
    }

    public ScriptedEffectSpec getEffectSpec() {
        return getSpec();
    }

    private static String normalizeHook(String hookName) {
        return hookName == null ? "" : hookName;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_LIFE_TICKS_OVERRIDE, -1);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            setOwnerUuid(tag.getUUID("owner"));
        }
        age = tag.getInt("age");
        ballisticStateInitialized = tag.getBoolean("motionStateInitialized");
        ballisticCurrentY = tag.getDouble("motionCurrentY");
        ballisticVelY = tag.getDouble("motionVelY");
        ballisticStartY = tag.getDouble("motionStartY");
        ballisticMaxY = tag.getDouble("motionMaxY");
        ballisticInitVel = tag.contains("motionInitVel") ? tag.getDouble("motionInitVel") : 0.92D;
        lifeTicksOverride = tag.contains(LIFE_TICKS_OVERRIDE_TAG) ? tag.getInt(LIFE_TICKS_OVERRIDE_TAG) : -1;
        activeArcs.clear();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID("owner", ownerUuid);
        }
        tag.putInt("age", age);
        tag.putBoolean("motionStateInitialized", ballisticStateInitialized);
        tag.putDouble("motionCurrentY", ballisticCurrentY);
        tag.putDouble("motionVelY", ballisticVelY);
        tag.putDouble("motionStartY", ballisticStartY);
        tag.putDouble("motionMaxY", ballisticMaxY);
        tag.putDouble("motionInitVel", ballisticInitVel);
        if (lifeTicksOverride > 0) {
            tag.putInt(LIFE_TICKS_OVERRIDE_TAG, lifeTicksOverride);
        }
    }

    private static double clamp01(double v) {
        if (v < 0.0D) {
            return 0.0D;
        }
        if (v > 1.0D) {
            return 1.0D;
        }
        return v;
    }

    private void returnLandingItem(ScriptedEffectSpec spec, Player owner) {
        if (level().isClientSide() || owner == null || owner.getAbilities().instabuild) {
            return;
        }

        String itemId = spec.getStringParam("return-item-id", "");
        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
        if (resourceLocation == null) {
            return;
        }

        Item item = BuiltInRegistries.ITEM.getOptional(resourceLocation).orElse(null);
        if (item == null) {
            return;
        }

        ItemStack returned = new ItemStack(item);
        if (owner.getMainHandItem().isEmpty()) {
            owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, returned);
        } else if (!owner.getInventory().add(returned)) {
            owner.drop(returned, false);
        }
    }

    private boolean tickVerticalBallisticMotion(ScriptedEffectSpec spec, Player owner) {
        if (owner == null) {
            this.ballisticStateInitialized = false;
            return false;
        }

        double gravity = spec.getDoubleParam("gravity", 0.06D);
        double initVel = spec.getDoubleParam("init-vel", 0.92D);

        if (!this.ballisticStateInitialized) {
            this.ballisticStateInitialized = true;
            this.ballisticStartY = owner.getY();
            this.ballisticCurrentY = this.ballisticStartY;
            this.ballisticInitVel = initVel;
            this.ballisticVelY = owner.getDeltaMovement().y + initVel;
            this.ballisticMaxY = this.ballisticCurrentY;
        }

        this.ballisticVelY -= gravity;
        this.ballisticCurrentY += this.ballisticVelY;
        this.ballisticMaxY = Math.max(this.ballisticMaxY, this.ballisticCurrentY);
        this.setPos(owner.getX(), this.ballisticCurrentY, owner.getZ());

        if ((this.ballisticCurrentY < owner.getY() && this.ballisticVelY < 0.0D) || this.tickCount > BALLISTIC_MAX_LIFE) {
            this.ballisticStateInitialized = false;
            returnLandingItem(spec, owner);
            // Client-side-only cosmetic callback (matches upstream EntityCoinThrowing's
            // `getEntityWorld().isRemote` check) — e.g. the "heads or tails" flavor
            // message registered as entity_coin_throwing's :on-landed-fn hook-param.
            if (level().isClientSide()) {
                Object onLanded = spec.getHookParams().get("on-landed-fn");
                if (onLanded instanceof IFn fn) {
                    fn.invoke(owner);
                }
            }
            this.discard();
            return true;
        }

        return false;
    }

    public boolean hasMotionProgress() {
        ScriptedEffectSpec spec = getSpec();
        if (spec == null) {
            return false;
        }
        return "vertical-ballistic".equals(normalizeHook(spec.getEffectHook())) && this.ballisticStateInitialized;
    }

    public double getMotionProgress() {
        if (!hasMotionProgress()) {
            return 0.0D;
        }

        Player owner = getOwnerPlayer();
        if (owner == null) {
            return 0.0D;
        }

        if (this.ballisticVelY > 0.0D) {
            return ((this.ballisticInitVel - this.ballisticVelY) / this.ballisticInitVel) * 0.5D;
        }

        return Math.min(1.0D, 0.5D + ((this.ballisticMaxY - this.ballisticCurrentY) / (this.ballisticMaxY - this.ballisticStartY)) * 0.5D);
    }

    @Override
    public void tick() {
        super.tick();
        ScriptedEffectSpec spec = getSpec();
        String effectHook = normalizeHook(spec == null ? null : spec.getEffectHook());

        Player owner = getOwnerPlayer();
        ScriptedEffectHook hook = ScriptedEffectHooks.resolve(effectHook);
        // Server-authoritative hooks (OwnerOrbitEffectHook; the server-spawned
        // LightShield shield's OwnerOffsetEffectHook) drive the position on
        // the server-owned instance; the client renders the vanilla-synced
        // position and must not snap or re-compute it — client-side
        // re-computation left the shield at its spawn point (inside the
        // player) whenever the owner lookup hiccuped, blinking it. The
        // client-spawned JetEngine diamond shield stays client-driven.
        boolean serverDrivenHook = hook instanceof OwnerOrbitEffectHook
                || ("md-shield".equals(effectHook) && hook instanceof OwnerOffsetEffectHook);
        if ((spec == null || spec.isFollowOwner()) && owner != null
                && !(serverDrivenHook && level().isClientSide())) {
            setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
            setYRot(owner.getYRot());
            setXRot(owner.getXRot());
        }

        boolean discardedByMotionProfile = false;
        if (spec != null && "vertical-ballistic".equals(effectHook)) {
            discardedByMotionProfile = tickVerticalBallisticMotion(spec, owner);
        }

        if (!discardedByMotionProfile) {
            if (level().isClientSide() && level() instanceof ClientLevel clientLevel) {
                if (!serverDrivenHook) {
                    hook.onClientTick(this, clientLevel);
                }
            } else {
                hook.onServerTick(this, level());
            }
        }

        if (discardedByMotionProfile) {
            return;
        }

        age++;
        int lifeTicks;
        if (spec != null) {
            // Read the synced override — the client-owned instance must die at
            // the same tick as the server one (spawn-time life overrides like
            // the LightShield shield's max-active+margin would otherwise be
            // lost client-side, killing the shield early).
            int syncedOverride = this.entityData.get(DATA_LIFE_TICKS_OVERRIDE);
            lifeTicks = syncedOverride > 0 ? syncedOverride : spec.getLifeTicks();
        } else if (this instanceof ScriptedRayEntity rayEntity) {
            ScriptedRaySpec raySpec = rayEntity.getRaySpec();
            lifeTicks = raySpec == null ? 15 : raySpec.getLifeTicks();
        } else if (this instanceof ScriptedMarkerEntity markerEntity) {
            ScriptedMarkerSpec markerSpec = markerEntity.getMarkerSpec();
            lifeTicks = markerSpec == null ? 15 : markerSpec.getLifeTicks();
        } else {
            lifeTicks = 15;
        }
        if (age >= lifeTicks) {
            discard();
        }
    }

    public int getAgeTicks() {
        return age;
    }

    /**
     * The life this instance will actually die at: the synced spawn-time
     * override when one was set, otherwise the spec's. Renderers whose curves
     * are expressed as a fraction of life (EntityMdBall.getAlpha) need this,
     * and reading the spec alone would give them the wrong answer for every
     * entity spawned with an override.
     */
    public int getEffectiveLifeTicks() {
        int syncedOverride = this.entityData.get(DATA_LIFE_TICKS_OVERRIDE);
        if (syncedOverride > 0) {
            return syncedOverride;
        }
        ScriptedEffectSpec spec = getEffectSpec();
        return spec == null ? 15 : spec.getLifeTicks();
    }

    public List<ArcData> getActiveArcs() {
        return Collections.unmodifiableList(activeArcs);
    }

    public List<ArcData> mutableActiveArcs() {
        return activeArcs;
    }

    public RandomSource getEffectRandom() {
        return this.random;
    }

    public Player getOwnerPlayer() {
        UUID ownerId = getOwnerUuid();
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    public UUID getOwnerUuid() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public void setOwnerPlayer(Player owner) {
        setOwnerUuid(owner == null ? null : owner.getUUID());
    }

    public void setOwnerUuid(UUID uuid) {
        ownerUuid = uuid;
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    public void setLifeTicksOverride(int lifeTicks) {
        this.lifeTicksOverride = Math.max(1, lifeTicks);
        this.entityData.set(DATA_LIFE_TICKS_OVERRIDE, Math.max(1, lifeTicks));
    }

    public static final class ArcData {
        public final float[][][] strands;
        public int lifeTicks;
        public final float phase;
        public final float flickerSeed;

        public ArcData(float[][][] strands, int lifeTicks, float phase, float flickerSeed) {
            this.strands = strands;
            this.lifeTicks = lifeTicks;
            this.phase = phase;
            this.flickerSeed = flickerSeed;
        }
    }
}
