package cn.li.mc262.entity;


import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcbase.entity.spec.ScriptedProjectileSpec;
import cn.li.mcver.ResourceLocations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ScriptedProjectileEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> DATA_ANCHORED =
            SynchedEntityData.defineId(ScriptedProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Direction> DATA_ANCHOR_FACE =
            SynchedEntityData.defineId(ScriptedProjectileEntity.class, EntityDataSerializers.DIRECTION);
    private boolean anchored;
    private BlockPos anchorPos;
    private Direction anchorFace = Direction.UP;

    public ScriptedProjectileEntity(EntityType<? extends ScriptedProjectileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANCHORED, false);
        builder.define(DATA_ANCHOR_FACE, Direction.UP);
    }

    protected ScriptedProjectileSpec getSpec() {
        return ScriptedEntitySpecAccess.getScriptedProjectileSpec(getType());
    }

    private static String hook(String value) {
        return value == null ? "" : value;
    }

    private Item resolveDefaultItem() {
        ScriptedProjectileSpec spec = getSpec();
        if (spec == null || spec.getDefaultItemId() == null || spec.getDefaultItemId().isBlank()) {
            return Items.AIR;
        }
        try {
            Item item = BuiltInRegistries.ITEM.getValue(ResourceLocations.parse(spec.getDefaultItemId()));
            return item == null ? Items.AIR : item;
        } catch (IllegalArgumentException ignored) {
            return Items.AIR;
        }
    }

    /**
     * Anchored state under the name the client render bridge probes for
     * reflectively (ScriptedRenderAccess.isBehaviorHit). Upstream's
     * EntityMagHook exposes the same flag as {@code isHit} and its renderer
     * swaps to the opened model on it.
     */
    public boolean isBehaviorHit() {
        return isAnchored();
    }

    private boolean isAnchored() {
        return anchored || entityData.get(DATA_ANCHORED);
    }

    private void applyAnchoredState() {
        anchored = true;
        // The synced face (server value) — a client that did not run its own
        // hit detection would otherwise keep the default UP face and flatten
        // the open cross into a bar (upstream syncs HIT_SIDE the same way).
        anchorFace = entityData.get(DATA_ANCHOR_FACE);
        entityData.set(DATA_ANCHORED, true);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        noPhysics = true;
        refreshDimensions();
        applyAnchorRotation(anchorFace);
    }

    /**
     * Face orientation matching upstream EntityMagHook.preRender's switch:
     * DOWN/UP turn the hook vertical, horizontal faces turn it flat against
     * the wall, so the open cross always faces away from the anchored block.
     */
    private void applyAnchorRotation(Direction face) {
        switch (face) {
            case DOWN -> setXRot(-90.0F);
            case UP -> setXRot(90.0F);
            case NORTH -> { setYRot(0.0F); setXRot(0.0F); }
            case SOUTH -> { setYRot(180.0F); setXRot(0.0F); }
            case WEST -> { setYRot(-90.0F); setXRot(0.0F); }
            case EAST -> { setYRot(90.0F); setXRot(0.0F); }
            default -> { }
        }
        yRotO = getYRot();
        xRotO = getXRot();
    }

    private void dropConfiguredItemAndDiscard() {
        if (!level().isClientSide()) {
            ScriptedProjectileSpec spec = getSpec();
            if (spec == null || spec.isDropItemOnDiscard()) {
                Item item = resolveDefaultItem();
                if (item != Items.AIR) {
                    level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), new ItemStack(item)));
                }
            }
        }
        discard();
    }

    @Override
    public void tick() {
        if (isAnchored()) {
            applyAnchoredState();
        }
        super.tick();
        if (!isAnchored()) {
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide()
                && "drop-when-invalid".equals(hook(getSpec() == null ? null : getSpec().getOnAnchoredTickHook()))
                && (anchorPos == null || level().getBlockState(anchorPos).isAir())) {
            dropConfiguredItemAndDiscard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        ScriptedProjectileSpec spec = getSpec();
        if (result.getType() == HitResult.Type.BLOCK
                && !isAnchored()
                && "anchor".equals(hook(spec == null ? null : spec.getOnHitBlockHook()))) {
            BlockHitResult bhr = (BlockHitResult) result;
            // Upstream stores res.getBlockPos() — the ACTUAL hit block. The
            // raw hit location lies exactly ON the block face, and
            // BlockPos.containing() of a face-plane point floors to the block
            // on the NEAR side of the face — the air in front of the wall —
            // so the anchored-tick isAir check then immediately dropped the
            // hook as an item on every side hit. The hook also sits HALF A
            // BLOCK OFF the face (upstream preRender) so it does not embed
            // into the block it bit.
            anchorPos = bhr.getBlockPos();
            Direction face = bhr.getDirection();
            anchorFace = face;
            entityData.set(DATA_ANCHOR_FACE, face);
            setPos(Vec3.atCenterOf(anchorPos)
                    .add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.51D)));
            applyAnchoredState();
            return;
        }
        super.onHit(result);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        ScriptedProjectileSpec spec = getSpec();
        if (!"damage-and-discard".equals(hook(spec == null ? null : spec.getOnHitEntityHook()))) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (!level().isClientSide() && target != owner && !(target instanceof ScriptedProjectileEntity)
                && level() instanceof ServerLevel serverLevel) {
            float damage = (float) Math.max(0.0D, spec == null ? 0.0D : spec.getDamage());
            if (damage > 0.0F) {
                DamageSource source = owner instanceof Player player
                        ? damageSources().playerAttack(player)
                        : damageSources().thrown(this, owner == null ? this : owner);
                target.hurtServer(serverLevel, source, damage);
            }
            dropConfiguredItemAndDiscard();
        }
    }

    @Override
    protected Item getDefaultItem() {
        return resolveDefaultItem();
    }

    @Override
    protected double getDefaultGravity() {
        if (anchored) {
            return 0.0D;
        }
        ScriptedProjectileSpec spec = getSpec();
        return spec == null ? 0.05D : Math.max(0.0D, spec.getGravity());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return isAnchored() ? EntityDimensions.scalable(1.0F, 1.0F) : super.getDimensions(pose);
    }

    @Override
    public boolean isPickable() {
        return isAnchored();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        ScriptedProjectileSpec spec = getSpec();
        if (isAnchored() && source.getEntity() != null
                && "discard-when-hurt".equals(hook(spec == null ? null : spec.getOnAnchoredHurtHook()))) {
            dropConfiguredItemAndDiscard();
            return true;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Anchored", anchored);
        if (anchorPos != null) {
            output.putLong("AnchorPos", anchorPos.asLong());
            output.putString("AnchorFace", anchorFace.getSerializedName());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        anchored = input.getBooleanOr("Anchored", false);
        entityData.set(DATA_ANCHORED, anchored);
        input.getLong("AnchorPos").ifPresent(value -> anchorPos = BlockPos.of(value));
        Direction loadedFace = Direction.byName(input.getStringOr("AnchorFace", "up"));
        anchorFace = loadedFace == null ? Direction.UP : loadedFace;
        entityData.set(DATA_ANCHOR_FACE, anchorFace);
        if (anchored) {
            applyAnchoredState();
        }
    }
}
