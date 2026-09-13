package cn.li.mc1201.entity;


import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcver.ResourceLocations;

import cn.li.mcbase.entity.spec.ScriptedBlockBodySpec;
import cn.li.mcbase.clj.ClojureInterop;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ScriptedBlockBodyEntity extends ScriptedProjectileEntity implements cn.li.mcbase.entity.IScriptedBlockBodyEntity {
    private static final String NBT_BLOCK_ID = "BlockId";
    private static final String NBT_GRAVITY = "BlockBodyGravity";
    private static final String NBT_DAMAGE = "BlockBodyDamage";
    private static final String NBT_PLACE_WHEN_COLLIDE = "BlockBodyPlaceWhenCollide";

    private static final String IMPACT_DETONATION = "impact-detonation";
    private static final String BEHAVIOR_REGISTRY_NS = "cn.li.mcmod.spi.entity-behavior-registry";
    private static final String ENTITY_DAMAGE_NS = "cn.li.mcmod.platform.entity-damage";

    static {
        ClojureInterop.requireNamespace(BEHAVIOR_REGISTRY_NS);
        ClojureInterop.requireNamespace(ENTITY_DAMAGE_NS);
    }
    private int behaviorDespawnCountdown = -1;

    private static final EntityDataAccessor<String> DATA_BLOCK_ID =
            SynchedEntityData.defineId(ScriptedBlockBodyEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_GRAVITY =
            SynchedEntityData.defineId(ScriptedBlockBodyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(ScriptedBlockBodyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_PLACE_WHEN_COLLIDE =
            SynchedEntityData.defineId(ScriptedBlockBodyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BEHAVIOR_HIT =
            SynchedEntityData.defineId(ScriptedBlockBodyEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean syncedFieldsInitialized;

    public ScriptedBlockBodyEntity(EntityType<? extends ScriptedProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BLOCK_ID, "minecraft:stone");
        this.entityData.define(DATA_GRAVITY, 0.05F);
        this.entityData.define(DATA_DAMAGE, 0.0F);
        this.entityData.define(DATA_PLACE_WHEN_COLLIDE, false);
        this.entityData.define(DATA_BEHAVIOR_HIT, false);
    }

    @Override
    public void tick() {
        ensureSyncedFields();
        super.tick();
        ScriptedBlockBodySpec spec = getBlockBodySpec();
        double drag = spec == null ? 1.0D : spec.getDrag();
        if (drag < 1.0D) {
            // Per-spec linear drag (silbarn: original's Rigidbody linearDrag
            // 0.8) — glide to a halt and hover (gravity is also delayed 50
            // ticks, see getGravity) so the caster can hit it mid-air.
            this.setDeltaMovement(this.getDeltaMovement().scale(drag));
        }
        if (behaviorDespawnCountdown > 0) {
            behaviorDespawnCountdown--;
            if (behaviorDespawnCountdown == 0 && !this.level().isClientSide) {
                this.discard();
            }
        }
    }

    private boolean hasImpactDetonationBehavior() {
        ScriptedBlockBodySpec spec = getBlockBodySpec();
        return spec != null && IMPACT_DETONATION.equals(spec.getBehaviorId());
    }

    private void markBehaviorHit(boolean heavy) {
        if (behaviorDespawnCountdown >= 0 || this.level().isClientSide) {
            return;
        }
        behaviorDespawnCountdown = 10;
        this.entityData.set(DATA_BEHAVIOR_HIT, true);
        String soundPath = behaviorValue(heavy ? "heavy-sound" : "light-sound", "");
        String soundId = cn.li.mcmod.ModId.ID + ":" + soundPath;
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(ResourceLocations.parse(soundId));
        if (sound != null) {
            this.playSound(sound, 0.5F, 1.0F);
        }
        spawnBehaviorParticles();
    }

    private void spawnBehaviorParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        String particlePath = behaviorValue("particle", "");
        var fragTypeRaw = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocations.of(cn.li.mcmod.ModId.ID, particlePath));
        if (!(fragTypeRaw instanceof SimpleParticleType fragType)) {
            return;
        }
        // Content behavior supplies the particle; generic adapter preserves the sphere distribution.
        int n = 18 + this.random.nextInt(10);
        for (int i = 0; i < n; i++) {
            double vel = 0.08 + this.random.nextDouble() * 0.10;
            double vsq = vel * vel;
            double vx = this.random.nextDouble() * vel;
            double vxsq = vx * vx;
            double vy = this.random.nextDouble() * Math.sqrt(Math.max(0.0, vsq - vxsq));
            double vz = Math.sqrt(Math.max(0.0, vsq - vxsq - vy * vy));
            vx *= this.random.nextBoolean() ? 1 : -1;
            vy *= this.random.nextBoolean() ? 1 : -1;
            vz *= this.random.nextBoolean() ? 1 : -1;
            vy += 0.2; // upward bias matches original
            // count=0: single particle with exact velocity (dx/dy/dz used as vx/vy/vz)
            serverLevel.sendParticles(fragType, this.getX(), this.getY(), this.getZ(),
                    0, vx, vy, vz, 0.0);
        }
    }

    /**
     * Force an in-flight behavior-driven entity to detonate.
     * This models a synthetic collision result whose entityHit is the entity itself,
     * which is why original always plays the "heavy" sound variant here.
     */
    public void forceBehaviorHit() {
        if (hasImpactDetonationBehavior()) {
            markBehaviorHit(true);
        }
    }

    /** Client-synced behavior hit flag, used by the renderer to hide the model. */
    public boolean isBehaviorHit() {
        return this.entityData.get(DATA_BEHAVIOR_HIT);
    }

    private void ensureSyncedFields() {
        if (syncedFieldsInitialized) {
            return;
        }
        ScriptedBlockBodySpec spec = getBlockBodySpec();
        if (spec != null) {
            this.entityData.set(DATA_BLOCK_ID, normalizeBlockId(spec.getDefaultBlockId()));
            this.entityData.set(DATA_GRAVITY, (float) Math.max(0.0D, spec.getGravity()));
            this.entityData.set(DATA_DAMAGE, (float) Math.max(0.0D, spec.getDamage()));
            this.entityData.set(DATA_PLACE_WHEN_COLLIDE, spec.isPlaceWhenCollide());
        }
        syncedFieldsInitialized = true;
    }

    private static String normalizeBlockId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return "minecraft:stone";
        }
        return blockId;
    }

    public String getSyncedBlockId() {
        ensureSyncedFields();
        return normalizeBlockId(this.entityData.get(DATA_BLOCK_ID));
    }

    /**
     * Override the per-instance rendered/placed block after creation.
     *
     * <p>MagManip captures arbitrary configured metal blocks, while the
     * registry spec can only provide a fallback block. Initialize the other
     * spec-backed fields first so setting the block id cannot accidentally
     * leave damage, gravity, or placement at their constructor defaults.</p>
     */
    public void setSyncedBlockId(String blockId) {
        ensureSyncedFields();
        this.entityData.set(DATA_BLOCK_ID, normalizeBlockId(blockId));
    }

    /** Enable or disable block placement for this individual in-flight body. */
    public void setPlaceWhenCollide(boolean placeWhenCollide) {
        ensureSyncedFields();
        this.entityData.set(DATA_PLACE_WHEN_COLLIDE, placeWhenCollide);
    }

    private float getSyncedGravity() {
        ensureSyncedFields();
        return Math.max(0.0F, this.entityData.get(DATA_GRAVITY));
    }

    private float getSyncedDamage() {
        ensureSyncedFields();
        return Math.max(0.0F, this.entityData.get(DATA_DAMAGE));
    }

    private boolean isSyncedPlaceWhenCollide() {
        ensureSyncedFields();
        return this.entityData.get(DATA_PLACE_WHEN_COLLIDE);
    }

    public ScriptedBlockBodySpec getBlockBodySpec() {
        return ScriptedEntitySpecAccess.getScriptedBlockBodySpec(this.getType());
    }

    private boolean isMagManipBlockBody() {
        ScriptedBlockBodySpec spec = getBlockBodySpec();
        return spec != null && "magmanip-block".equals(spec.getHookId());
    }

    @Override
    protected float getGravity() {
        if (hasImpactDetonationBehavior() && this.tickCount < 50) {
            return 0.0F;
        }
        return getSyncedGravity();
    }

    @Override
    public boolean isPickable() {
        return hasImpactDetonationBehavior() || super.isPickable();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        if (target == owner) {
            return;
        }
        if (hasImpactDetonationBehavior()) {
            boolean heavy = target instanceof ScriptedBlockBodyEntity other && other.hasImpactDetonationBehavior();
            markBehaviorHit(heavy);
            return;
        }
        float damage = getSyncedDamage();
        if (damage > 0.0F) {
            Object handled = dispatchBehaviorDamage(owner, target, damage);
            if (handled == null) {
                DamageSource source = this.damageSources().thrown(this, owner == null ? this : owner);
                target.hurt(source, damage);
            }
        }
    }

    /**
     * Give content-owned block-body behaviors a chance to route collision
     * damage through their ability pipeline. A null return means no handler
     * exists and preserves the generic thrown-damage fallback.
     */
    private Object dispatchBehaviorDamage(Entity owner, Entity target, float damage) {
        ScriptedBlockBodySpec spec = getBlockBodySpec();
        if (spec == null || spec.getBehaviorId().isBlank()) {
            return null;
        }
        try {
            String worldId = this.level().dimension().location().toString();
            String ownerId = owner == null ? null : owner.getStringUUID();
            return ClojureInterop.invoke(
                    ENTITY_DAMAGE_NS,
                    "handle-scripted-block-body-hit!",
                    spec.getBehaviorId(),
                    worldId,
                    ownerId,
                    target.getStringUUID(),
                    (double) damage
            );
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() == HitResult.Type.BLOCK) {
            if (hasImpactDetonationBehavior()) {
                markBehaviorHit(false);
            } else if (isSyncedPlaceWhenCollide() && !this.level().isClientSide) {
                placeSyncedBlockOnHit((BlockHitResult) result);
            }
        }
    }

    /**
     * Matches original EntityBlock's CollideEvent handler: try the hit
     * block position, then the face-adjacent position, then surrounding
     * neighbors (the original's eight diagonal candidates for MagManip),
     * placing the synced block wherever the first replaceable position is
     * found. Discards (block is lost) if none are replaceable, matching the
     * original's "EntityBlock Lost" fallback.
     */
    private void placeSyncedBlockOnHit(BlockHitResult result) {
        Level level = this.level();
        BlockState state = resolveSyncedBlockState();
        if (state == null) {
            this.discard();
            return;
        }
        BlockPos origin = result.getBlockPos();
        if (placeIfReplaceable(level, origin, state)) {
            this.discard();
            return;
        }
        BlockPos adjacent = origin.relative(result.getDirection());
        if (placeIfReplaceable(level, adjacent, state)) {
            this.discard();
            return;
        }
        boolean magManip = isMagManipBlockBody();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    // Original EntityBlock checks only the eight diagonal
                    // neighbors. Preserve the broader modern fallback for
                    // unrelated block bodies, but match it for MagManip.
                    if (magManip
                            ? (dx == 0 || dy == 0 || dz == 0)
                            : (dx == 0 && dy == 0 && dz == 0)) {
                        continue;
                    }
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (placeIfReplaceable(level, candidate, state)) {
                        this.discard();
                        return;
                    }
                }
            }
        }
        if (magManip) {
            // Original fallback walks outward along the collided face for up
            // to ten checks when the local candidates are all occupied.
            BlockPos candidate = origin;
            for (int remaining = 10; remaining > 0; remaining--) {
                if (placeIfReplaceable(level, candidate, state)) {
                    this.discard();
                    return;
                }
                candidate = candidate.relative(result.getDirection());
            }
        }
        this.discard();
    }

    private boolean placeIfReplaceable(Level level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        level.setBlock(pos, state, 3);
        // Original EntityBlock placed through ItemBlock#placeBlockAt, which runs
        // onBlockPlacedBy. Without the modern equivalent an oriented block always
        // lands in its default state instead of facing the thrower.
        LivingEntity placer = getOwner() instanceof LivingEntity living ? living : null;
        state.getBlock().setPlacedBy(level, pos, state, placer, new ItemStack(state.getBlock()));
        return true;
    }

    private BlockState resolveSyncedBlockState() {
        try {
            ResourceLocation blockId = ResourceLocations.parse(getSyncedBlockId());
            Block block = BuiltInRegistries.BLOCK.get(blockId);
            return block == null ? null : block.defaultBlockState();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String behaviorValue(String key, String fallback) {
        try {
            Object value = ClojureInterop.invoke(BEHAVIOR_REGISTRY_NS, "value", IMPACT_DETONATION, key, fallback);
            return value == null ? fallback : String.valueOf(value);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    @Override
    protected Item getDefaultItem() {
        try {
            ResourceLocation blockId = ResourceLocations.parse(getSyncedBlockId());
            Item item = BuiltInRegistries.ITEM.get(blockId);
            return item == null ? Items.AIR : item;
        } catch (Exception ignored) {
            return Items.AIR;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ensureSyncedFields();
        tag.putString(NBT_BLOCK_ID, getSyncedBlockId());
        tag.putFloat(NBT_GRAVITY, getSyncedGravity());
        tag.putFloat(NBT_DAMAGE, getSyncedDamage());
        tag.putBoolean(NBT_PLACE_WHEN_COLLIDE, isSyncedPlaceWhenCollide());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(NBT_BLOCK_ID)) {
            this.entityData.set(DATA_BLOCK_ID, normalizeBlockId(tag.getString(NBT_BLOCK_ID)));
        }
        if (tag.contains(NBT_GRAVITY)) {
            this.entityData.set(DATA_GRAVITY, Math.max(0.0F, tag.getFloat(NBT_GRAVITY)));
        }
        if (tag.contains(NBT_DAMAGE)) {
            this.entityData.set(DATA_DAMAGE, Math.max(0.0F, tag.getFloat(NBT_DAMAGE)));
        }
        if (tag.contains(NBT_PLACE_WHEN_COLLIDE)) {
            this.entityData.set(DATA_PLACE_WHEN_COLLIDE, tag.getBoolean(NBT_PLACE_WHEN_COLLIDE));
        }
        if (hasImpactDetonationBehavior() || isMagManipBlockBody()) {
            // Both original entity types intentionally do not survive a
            // save/reload.
            this.discard();
        }
    }
}
