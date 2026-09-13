package cn.li.mc1211.entity;


import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mcver.ResourceLocations;

import cn.li.mcbase.entity.spec.ScriptedProjectileSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ScriptedProjectileEntity extends ThrowableItemProjectile {

    private static final String HOOK_ANCHOR = "anchor";
    private static final String HOOK_DAMAGE_AND_DISCARD = "damage-and-discard";
    private static final String HOOK_DROP_WHEN_INVALID = "drop-when-invalid";
    private static final String HOOK_DISCARD_WHEN_HURT = "discard-when-hurt";
    private static final EntityDataAccessor<Boolean> DATA_ANCHORED =
            SynchedEntityData.defineId(ScriptedProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Direction> DATA_ANCHOR_FACE =
            SynchedEntityData.defineId(ScriptedProjectileEntity.class, EntityDataSerializers.DIRECTION);

    private boolean anchored = false;
    private BlockPos anchorPos = null;
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

    private ScriptedProjectileSpec getSpec() {
        return ScriptedEntitySpecAccess.getScriptedProjectileSpec(this.getType());
    }

    private static String normalizeHook(String hookName) {
        return hookName == null ? "" : hookName;
    }

    private Item resolveDefaultItem() {
        ScriptedProjectileSpec spec = getSpec();
        if (spec == null || spec.getDefaultItemId() == null || spec.getDefaultItemId().isEmpty()) {
            return Items.AIR;
        }
        ResourceLocation itemId;
        try {
            itemId = ResourceLocations.parse(spec.getDefaultItemId());
        } catch (Exception ignored) {
            return Items.AIR;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? Items.AIR : item;
    }

    private void dropConfiguredItemAndDiscard() {
        if (!this.level().isClientSide) {
            ScriptedProjectileSpec spec = getSpec();
            boolean dropItem = spec == null || spec.isDropItemOnDiscard();
            if (dropItem) {
                Item item = resolveDefaultItem();
                if (item != Items.AIR) {
                    ItemEntity dropped = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), new ItemStack(item));
                    this.level().addFreshEntity(dropped);
                }
            }
        }
        this.discard();
    }

    private void anchorAt(HitResult result) {
        this.anchored = true;
        this.entityData.set(DATA_ANCHORED, true);
        BlockHitResult bhr = (BlockHitResult) result;
        // Upstream stores res.getBlockPos() — the ACTUAL hit block. The raw
        // hit location lies exactly ON the block face, and
        // BlockPos.containing() of a face-plane point floors to the block on
        // the NEAR side of the face — the air in front of the wall — so the
        // anchored-tick isAir check then immediately dropped the hook as an
        // item on every side hit (the hook only ever stayed anchored on
        // top-face hits, where floor() lands on the block itself).
        this.anchorPos = bhr.getBlockPos();
        // Upstream EntityMagHook.preRender: the hooked hook sits HALF A BLOCK
        // OFF the hit face, not at the raw hit point — anchored at the hit
        // point its 1x1 box embeds into the block (invisible when it bites a
        // tree trunk), and it rotates to the hit face so the open cross
        // spreads against the wall instead of keeping the throw angle (which
        // skewed the cross and hid one arm).
        Direction face = bhr.getDirection();
        this.anchorFace = face;
        this.entityData.set(DATA_ANCHOR_FACE, face);
        this.setPos(Vec3.atCenterOf(this.anchorPos)
                .add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.51D)));
        applyAnchoredState();
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
        return this.anchored || this.entityData.get(DATA_ANCHORED);
    }

    /**
     * The old EntityMagHook synchronised isHit and performed its still/open
     * transition on both sides.  Keeping this local state in sync is required
     * in 1.20 too: otherwise clients continue simulating a thrown item after
     * the server has attached it to a block.
     */
    private void applyAnchoredState() {
        this.anchored = true;
        // The synced face (server value) — a client that did not run its own
        // hit detection would otherwise keep the default UP face and flatten
        // the open cross into a bar (upstream syncs HIT_SIDE the same way).
        this.anchorFace = this.entityData.get(DATA_ANCHOR_FACE);
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.refreshDimensions();
        applyAnchorRotation(this.anchorFace);
    }

    @Override
    protected void updateRotation() {
        if (isAnchored()) {
            // Zero delta makes ThrowableProjectile lerp the rotation toward
            // atan2(0, 0) = 0 every tick, fighting applyAnchorRotation's face
            // orientation (reset each tick) — the anchored cross jittered.
            return;
        }
        super.updateRotation();
    }

    /**
     * Face orientation matching upstream EntityMagHook.preRender's switch:
     * DOWN/UP turn the hook vertical, horizontal faces turn it flat against
     * the wall, so the open cross always faces away from the anchored block.
     */
    private void applyAnchorRotation(Direction face) {
        switch (face) {
            case DOWN -> this.setXRot(-90.0F);
            case UP -> this.setXRot(90.0F);
            case NORTH -> { this.setYRot(0.0F); this.setXRot(0.0F); }
            case SOUTH -> { this.setYRot(180.0F); this.setXRot(0.0F); }
            case WEST -> { this.setYRot(-90.0F); this.setXRot(0.0F); }
            case EAST -> { this.setYRot(90.0F); this.setXRot(0.0F); }
            default -> { }
        }
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return isAnchored() ? EntityDimensions.scalable(1.0F, 1.0F) : super.getDimensions(pose);
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
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.noPhysics = true;

        if (this.level().isClientSide) {
            return;
        }
        ScriptedProjectileSpec spec = getSpec();
        if (!HOOK_DROP_WHEN_INVALID.equals(normalizeHook(spec == null ? null : spec.getOnAnchoredTickHook()))) {
            return;
        }
        if (this.anchorPos == null) {
            dropConfiguredItemAndDiscard();
            return;
        }
        BlockState state = this.level().getBlockState(this.anchorPos);
        if (state.isAir()) {
            dropConfiguredItemAndDiscard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        ScriptedProjectileSpec spec = getSpec();
        if (result.getType() == HitResult.Type.BLOCK
                && !isAnchored()
                && HOOK_ANCHOR.equals(normalizeHook(spec == null ? null : spec.getOnHitBlockHook()))) {
            // ThrowableItemProjectile's implementation discards itself after
            // a hit.  AcademyCraft's EntityMagHook instead stays alive,
            // enlarges, and becomes a magnetic target, so anchor before (and
            // instead of) the disposable projectile path.
            anchorAt(result);
            return;
        }
        super.onHit(result);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        ScriptedProjectileSpec spec = getSpec();
        if (!HOOK_DAMAGE_AND_DISCARD.equals(normalizeHook(spec == null ? null : spec.getOnHitEntityHook()))) {
            return;
        }
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        if (!this.level().isClientSide && target != owner) {
            if (!(target instanceof ScriptedProjectileEntity)) {
                float damage = (float) (spec == null ? 0.0D : Math.max(0.0D, spec.getDamage()));
                if (damage > 0.0F) {
                    DamageSource source = owner instanceof Player player
                        ? this.damageSources().playerAttack(player)
                        : this.damageSources().thrown(this, owner == null ? this : owner);
                    target.hurt(source, damage);
                }
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
        if (this.anchored) {
            return 0.0D;
        }
        ScriptedProjectileSpec spec = getSpec();
        return spec == null ? 0.05D : Math.max(0.0D, spec.getGravity());
    }

    @Override
    public boolean isPickable() {
        return isAnchored();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        ScriptedProjectileSpec spec = getSpec();
        if (isAnchored()
                && source.getEntity() != null
                && HOOK_DISCARD_WHEN_HURT.equals(normalizeHook(spec == null ? null : spec.getOnAnchoredHurtHook()))) {
            dropConfiguredItemAndDiscard();
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Anchored", anchored);
        if (anchorPos != null) {
            tag.putLong("AnchorPos", anchorPos.asLong());
            tag.putString("AnchorFace", anchorFace.getName());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        anchored = tag.getBoolean("Anchored");
        this.entityData.set(DATA_ANCHORED, anchored);
        if (tag.contains("AnchorPos")) {
            anchorPos = BlockPos.of(tag.getLong("AnchorPos"));
        }
        if (tag.contains("AnchorFace")) {
            anchorFace = Direction.byName(tag.getString("AnchorFace"));
            if (anchorFace == null) {
                anchorFace = Direction.UP;
            }
            this.entityData.set(DATA_ANCHOR_FACE, anchorFace);
        }
        if (anchored) {
            applyAnchoredState();
        }
    }
}
