package cn.li.mc262.entity;


import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcbase.clj.ClojureInterop;
import cn.li.mcbase.entity.spec.ScriptedBlockBodySpec;
import cn.li.mcver.ResourceLocations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ScriptedBlockBodyEntity extends ScriptedProjectileEntity implements cn.li.mcbase.entity.IScriptedBlockBodyEntity {
    private static final String IMPACT_DETONATION = "impact-detonation";
    private static final String BEHAVIOR_REGISTRY_NS = "cn.li.mcmod.spi.entity-behavior-registry";
    private static final String ENTITY_DAMAGE_NS = "cn.li.mcmod.platform.entity-damage";

    static {
        ClojureInterop.requireNamespace(BEHAVIOR_REGISTRY_NS);
        ClojureInterop.requireNamespace(ENTITY_DAMAGE_NS);
    }

    private static final EntityDataAccessor<String> DATA_BLOCK_ID =
            SynchedEntityData.defineId(ScriptedBlockBodyEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_GRAVITY =
            SynchedEntityData.defineId(ScriptedBlockBodyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(ScriptedBlockBodyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_PLACE =
            SynchedEntityData.defineId(ScriptedBlockBodyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BEHAVIOR_HIT =
            SynchedEntityData.defineId(ScriptedBlockBodyEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean initialized;
    private int despawnCountdown = -1;

    public ScriptedBlockBodyEntity(EntityType<? extends ScriptedProjectileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BLOCK_ID, "minecraft:stone");
        builder.define(DATA_GRAVITY, 0.05F);
        builder.define(DATA_DAMAGE, 0.0F);
        builder.define(DATA_PLACE, false);
        builder.define(DATA_BEHAVIOR_HIT, false);
    }

    public ScriptedBlockBodySpec getBlockBodySpec() {
        return ScriptedEntitySpecAccess.getScriptedBlockBodySpec(getType());
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        ScriptedBlockBodySpec spec = getBlockBodySpec();
        if (spec != null) {
            entityData.set(DATA_BLOCK_ID, normalizeBlockId(spec.getDefaultBlockId()));
            entityData.set(DATA_GRAVITY, (float) Math.max(0.0D, spec.getGravity()));
            entityData.set(DATA_DAMAGE, (float) Math.max(0.0D, spec.getDamage()));
            entityData.set(DATA_PLACE, spec.isPlaceWhenCollide());
        }
        initialized = true;
    }

    private static String normalizeBlockId(String blockId) {
        return blockId == null || blockId.isBlank() ? "minecraft:stone" : blockId;
    }

    public String getSyncedBlockId() {
        ensureInitialized();
        return normalizeBlockId(entityData.get(DATA_BLOCK_ID));
    }

    public void setSyncedBlockId(String blockId) {
        ensureInitialized();
        entityData.set(DATA_BLOCK_ID, normalizeBlockId(blockId));
    }

    public void setPlaceWhenCollide(boolean placeWhenCollide) {
        ensureInitialized();
        entityData.set(DATA_PLACE, placeWhenCollide);
    }

    public boolean isBehaviorHit() {
        return entityData.get(DATA_BEHAVIOR_HIT);
    }

    public void forceBehaviorHit() {
        if (hasImpactDetonationBehavior()) {
            markBehaviorHit(true);
        }
    }

    private boolean hasImpactDetonationBehavior() {
        ScriptedBlockBodySpec spec = getBlockBodySpec();
        return spec != null && IMPACT_DETONATION.equals(spec.getBehaviorId());
    }

    private boolean isMagManipBlockBody() {
        ScriptedBlockBodySpec spec = getBlockBodySpec();
        return spec != null && "magmanip-block".equals(spec.getHookId());
    }

    private void markBehaviorHit(boolean heavy) {
        if (!level().isClientSide() && despawnCountdown < 0) {
            entityData.set(DATA_BEHAVIOR_HIT, true);
            despawnCountdown = 10;
            String soundPath = behaviorValue(heavy ? "heavy-sound" : "light-sound", "");
            if (!soundPath.isBlank()) {
                SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(
                    ResourceLocations.of(cn.li.mcmod.ModId.ID, soundPath)
                );
                if (sound != null) {
                    playSound(sound, 0.5F, 1.0F);
                }
            }
            spawnBehaviorParticles();
        }
    }

    private void spawnBehaviorParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        String particlePath = behaviorValue("particle", "");
        var particleType = BuiltInRegistries.PARTICLE_TYPE.getValue(
            ResourceLocations.of(cn.li.mcmod.ModId.ID, particlePath)
        );
        if (!(particleType instanceof SimpleParticleType fragType)) {
            return;
        }
        int count = 18 + random.nextInt(10);
        for (int i = 0; i < count; i++) {
            double speed = 0.08 + random.nextDouble() * 0.10;
            double speedSq = speed * speed;
            double vx = random.nextDouble() * speed;
            double vxSq = vx * vx;
            double vy = random.nextDouble() * Math.sqrt(Math.max(0.0, speedSq - vxSq));
            double vz = Math.sqrt(Math.max(0.0, speedSq - vxSq - vy * vy));
            vx *= random.nextBoolean() ? 1 : -1;
            vy *= random.nextBoolean() ? 1 : -1;
            vz *= random.nextBoolean() ? 1 : -1;
            serverLevel.sendParticles(fragType, getX(), getY(), getZ(),
                0, vx, vy + 0.2, vz, 0.0);
        }
    }

    @Override
    public void tick() {
        ensureInitialized();
        super.tick();
        ScriptedBlockBodySpec spec = getBlockBodySpec();
        if (spec != null && spec.getDrag() < 1.0D) {
            setDeltaMovement(getDeltaMovement().scale(spec.getDrag()));
        }
        if (despawnCountdown > 0 && --despawnCountdown == 0 && !level().isClientSide()) {
            discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        ensureInitialized();
        if (hasImpactDetonationBehavior() && tickCount < 50) {
            return 0.0D;
        }
        return entityData.get(DATA_GRAVITY);
    }

    @Override
    public boolean isPickable() {
        return hasImpactDetonationBehavior() || super.isPickable();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide()) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (target == owner) {
            return;
        }
        if (hasImpactDetonationBehavior()) {
            boolean heavy = target instanceof ScriptedBlockBodyEntity other
                && other.hasImpactDetonationBehavior();
            markBehaviorHit(heavy);
            return;
        }
        float damage = Math.max(0.0F, entityData.get(DATA_DAMAGE));
        if (damage > 0.0F && dispatchBehaviorDamage(owner, target, damage) == null) {
            DamageSource source = damageSources().thrown(this, owner == null ? this : owner);
            target.hurt(source, damage);
        }
    }

    private Object dispatchBehaviorDamage(Entity owner, Entity target, float damage) {
        ScriptedBlockBodySpec spec = getBlockBodySpec();
        if (spec == null || spec.getBehaviorId().isBlank()) {
            return null;
        }
        try {
            String worldId = level().dimension().identifier().toString();
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
        if (result.getType() != HitResult.Type.BLOCK || level().isClientSide()) {
            return;
        }
        if (hasImpactDetonationBehavior()) {
            markBehaviorHit(false);
        } else if (entityData.get(DATA_PLACE)) {
            placeBlock((BlockHitResult) result);
        }
    }

    private void placeBlock(BlockHitResult hit) {
        BlockState state = resolveBlockState();
        if (state == null) {
            discard();
            return;
        }
        Level currentLevel = level();
        BlockPos origin = hit.getBlockPos();
        if (placeIfReplaceable(currentLevel, origin, state)) {
            discard();
            return;
        }
        BlockPos adjacent = origin.relative(hit.getDirection());
        if (placeIfReplaceable(currentLevel, adjacent, state)) {
            discard();
            return;
        }
        boolean magManip = isMagManipBlockBody();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (magManip
                        ? (dx == 0 || dy == 0 || dz == 0)
                        : (dx == 0 && dy == 0 && dz == 0)) {
                        continue;
                    }
                    if (placeIfReplaceable(currentLevel, origin.offset(dx, dy, dz), state)) {
                        discard();
                        return;
                    }
                }
            }
        }
        if (magManip) {
            BlockPos candidate = origin;
            for (int remaining = 10; remaining > 0; remaining--) {
                if (placeIfReplaceable(currentLevel, candidate, state)) {
                    discard();
                    return;
                }
                candidate = candidate.relative(hit.getDirection());
            }
        }
        discard();
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

    private BlockState resolveBlockState() {
        try {
            Block block = BuiltInRegistries.BLOCK.getValue(ResourceLocations.parse(getSyncedBlockId()));
            return block == null ? null : block.defaultBlockState();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    protected Item getDefaultItem() {
        try {
            Item item = BuiltInRegistries.ITEM.getValue(ResourceLocations.parse(getSyncedBlockId()));
            return item == null ? Items.AIR : item;
        } catch (IllegalArgumentException ignored) {
            return Items.AIR;
        }
    }

    private String behaviorValue(String key, String fallback) {
        try {
            Object value = ClojureInterop.invoke(
                BEHAVIOR_REGISTRY_NS,
                "value",
                IMPACT_DETONATION,
                key,
                fallback
            );
            return value == null ? fallback : String.valueOf(value);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        ensureInitialized();
        output.putString("BlockId", getSyncedBlockId());
        output.putFloat("BlockBodyGravity", entityData.get(DATA_GRAVITY));
        output.putFloat("BlockBodyDamage", entityData.get(DATA_DAMAGE));
        output.putBoolean("BlockBodyPlaceWhenCollide", entityData.get(DATA_PLACE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(DATA_BLOCK_ID, normalizeBlockId(input.getStringOr("BlockId", "minecraft:stone")));
        entityData.set(DATA_GRAVITY, Math.max(0.0F, input.getFloatOr("BlockBodyGravity", 0.05F)));
        entityData.set(DATA_DAMAGE, Math.max(0.0F, input.getFloatOr("BlockBodyDamage", 0.0F)));
        entityData.set(DATA_PLACE, input.getBooleanOr("BlockBodyPlaceWhenCollide", false));
        initialized = true;
        if (hasImpactDetonationBehavior() || isMagManipBlockBody()) {
            discard();
        }
    }
}
