package cn.li.mc262.entity;

import cn.li.mcbase.entity.IScriptedEffectEntity;


import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import clojure.lang.IFn;
import cn.li.mcbase.entity.hook.effect.OwnerOffsetEffectHook;
import cn.li.mc262.entity.hook.effect.OwnerOrbitEffectHook;
import cn.li.mcbase.entity.hook.effect.ScriptedEffectHook;
import cn.li.mcbase.entity.hook.effect.ScriptedEffectHooks;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import cn.li.mcver.ResourceLocations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ScriptedEffectEntity extends Entity implements IScriptedEffectEntity {
    private static final int BALLISTIC_MAX_LIFE = 120;
    private static final String LIFE_TICKS_OVERRIDE_TAG = "lifeTicksOverride";
    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(ScriptedEffectEntity.class, EntityDataSerializers.STRING);
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
                level);
        entity.setOwnerPlayer(owner);
        if (owner != null) {
            entity.setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
        }
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER_UUID, "");
        builder.define(DATA_LIFE_TICKS_OVERRIDE, -1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("owner", UUIDUtil.CODEC).ifPresent(this::setOwnerUuid);
        age = input.getIntOr("age", 0);
        ballisticStateInitialized = input.getBooleanOr("motionStateInitialized", false);
        ballisticCurrentY = input.getDoubleOr("motionCurrentY", 0.0D);
        ballisticVelY = input.getDoubleOr("motionVelY", 0.0D);
        ballisticStartY = input.getDoubleOr("motionStartY", 0.0D);
        ballisticMaxY = input.getDoubleOr("motionMaxY", 0.0D);
        ballisticInitVel = input.getDoubleOr("motionInitVel", 0.92D);
        lifeTicksOverride = input.getIntOr(LIFE_TICKS_OVERRIDE_TAG, -1);
        if (lifeTicksOverride > 0) {
            entityData.set(DATA_LIFE_TICKS_OVERRIDE, lifeTicksOverride);
        }
        activeArcs.clear();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.storeNullable("owner", UUIDUtil.CODEC, ownerUuid);
        output.putInt("age", age);
        output.putBoolean("motionStateInitialized", ballisticStateInitialized);
        output.putDouble("motionCurrentY", ballisticCurrentY);
        output.putDouble("motionVelY", ballisticVelY);
        output.putDouble("motionStartY", ballisticStartY);
        output.putDouble("motionMaxY", ballisticMaxY);
        output.putDouble("motionInitVel", ballisticInitVel);
        if (lifeTicksOverride > 0) {
            output.putInt(LIFE_TICKS_OVERRIDE_TAG, lifeTicksOverride);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        return false;
    }

    private void returnLandingItem(ScriptedEffectSpec spec, Player owner) {
        if (level().isClientSide() || owner == null || owner.getAbilities().instabuild) {
            return;
        }
        String itemId = spec.getStringParam("return-item-id", "");
        if (itemId.isBlank()) {
            return;
        }
        try {
            Item item = BuiltInRegistries.ITEM.getValue(ResourceLocations.parse(itemId));
            if (item == null) {
                return;
            }
            ItemStack returned = new ItemStack(item);
            if (owner.getMainHandItem().isEmpty()) {
                owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, returned);
            } else if (!owner.getInventory().add(returned)) {
                owner.drop(returned, false);
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid content parameter: omit the optional return item.
        }
    }

    private boolean tickVerticalBallisticMotion(ScriptedEffectSpec spec, Player owner) {
        if (owner == null) {
            ballisticStateInitialized = false;
            return false;
        }
        double gravity = spec.getDoubleParam("gravity", 0.06D);
        double initialVelocity = spec.getDoubleParam("init-vel", 0.92D);
        if (!ballisticStateInitialized) {
            ballisticStateInitialized = true;
            ballisticStartY = owner.getY();
            ballisticCurrentY = ballisticStartY;
            ballisticInitVel = initialVelocity;
            ballisticVelY = owner.getDeltaMovement().y + initialVelocity;
            ballisticMaxY = ballisticCurrentY;
        }
        ballisticVelY -= gravity;
        ballisticCurrentY += ballisticVelY;
        ballisticMaxY = Math.max(ballisticMaxY, ballisticCurrentY);
        setPos(owner.getX(), ballisticCurrentY, owner.getZ());

        if ((ballisticCurrentY < owner.getY() && ballisticVelY < 0.0D)
                || tickCount > BALLISTIC_MAX_LIFE) {
            ballisticStateInitialized = false;
            returnLandingItem(spec, owner);
            if (level().isClientSide()) {
                Object callback = spec.getHookParams().get("on-landed-fn");
                if (callback instanceof IFn fn) {
                    fn.invoke(owner);
                }
            }
            discard();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        ScriptedEffectSpec spec = getSpec();
        String effectHook = normalizeHook(spec == null ? null : spec.getEffectHook());
        Player owner = getOwnerPlayer();
        ScriptedEffectHook hook = ScriptedEffectHooks.resolve(effectHook);
        boolean serverDrivenHook = hook instanceof OwnerOrbitEffectHook
                || ("md-shield".equals(effectHook) && hook instanceof OwnerOffsetEffectHook);

        if ((spec == null || spec.isFollowOwner()) && owner != null
                && !(serverDrivenHook && level().isClientSide())) {
            setPos(owner.getX(), owner.getY() + 1.0D, owner.getZ());
            setYRot(owner.getYRot());
            setXRot(owner.getXRot());
        }

        boolean discardedByMotion = spec != null
                && "vertical-ballistic".equals(effectHook)
                && tickVerticalBallisticMotion(spec, owner);
        if (discardedByMotion) {
            return;
        }

        if (level().isClientSide() && level() instanceof ClientLevel clientLevel) {
            if (!serverDrivenHook) {
                hook.onClientTick(this, clientLevel);
            }
        } else {
            hook.onServerTick(this, level());
        }

        age++;
        int lifeTicks;
        if (spec != null) {
            int syncedOverride = entityData.get(DATA_LIFE_TICKS_OVERRIDE);
            lifeTicks = syncedOverride > 0 ? syncedOverride : spec.getLifeTicks();
        } else if (this instanceof ScriptedRayEntity ray) {
            ScriptedRaySpec raySpec = ray.getRaySpec();
            lifeTicks = raySpec == null ? 15 : raySpec.getLifeTicks();
        } else if (this instanceof ScriptedMarkerEntity marker) {
            ScriptedMarkerSpec markerSpec = marker.getMarkerSpec();
            lifeTicks = markerSpec == null ? 15 : markerSpec.getLifeTicks();
        } else {
            lifeTicks = 15;
        }
        if (age >= lifeTicks) {
            discard();
        }
    }

    public Player getOwnerPlayer() {
        UUID ownerId = getOwnerUuid();
        return ownerId == null ? null : level().getPlayerInAnyDimension(ownerId);
    }

    public UUID getOwnerUuid() {
        String synced = entityData.get(DATA_OWNER_UUID);
        if (!synced.isBlank()) {
            try {
                return UUID.fromString(synced);
            } catch (IllegalArgumentException ignored) {
                // Fall through to the locally cached value.
            }
        }
        return ownerUuid;
    }

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
        entityData.set(DATA_OWNER_UUID, uuid == null ? "" : uuid.toString());
    }

    public void setOwnerPlayer(Player player) {
        setOwnerUuid(player == null ? null : player.getUUID());
    }

    public void setLifeTicksOverride(int lifeTicks) {
        this.lifeTicksOverride = Math.max(1, lifeTicks);
        entityData.set(DATA_LIFE_TICKS_OVERRIDE, this.lifeTicksOverride);
    }

    public int getLifeTicksOverride() {
        return lifeTicksOverride;
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

    public boolean hasMotionProgress() {
        ScriptedEffectSpec spec = getSpec();
        return spec != null
                && "vertical-ballistic".equals(normalizeHook(spec.getEffectHook()))
                && ballisticStateInitialized;
    }

    public float getMotionProgress() {
        if (!hasMotionProgress()) {
            return 0.0F;
        }
        if (ballisticVelY > 0.0D) {
            return (float) Math.max(0.0D,
                    Math.min(0.5D, ((ballisticInitVel - ballisticVelY) / ballisticInitVel) * 0.5D));
        }
        double descent = ballisticMaxY - ballisticStartY;
        if (descent <= 0.0D) {
            return 1.0F;
        }
        return (float) Math.min(1.0D,
                0.5D + ((ballisticMaxY - ballisticCurrentY) / descent) * 0.5D);
    }

    public List<ArcData> getActiveArcs() {
        return Collections.unmodifiableList(activeArcs);
    }

    public List<ArcData> mutableActiveArcs() {
        return activeArcs;
    }

    public RandomSource getEffectRandom() {
        return random;
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
