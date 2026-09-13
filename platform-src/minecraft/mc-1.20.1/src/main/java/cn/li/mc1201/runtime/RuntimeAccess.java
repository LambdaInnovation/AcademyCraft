package cn.li.mc1201.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RuntimeAccess {
    private RuntimeAccess() {
    }

    public static String getEntityRegistryId(Entity entity) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null ? key.toString() : null;
    }

    public static Class<?> getItemStackClass() {
        return ItemStack.class;
    }

    public static Class<?> getItemClass() {
        return Item.class;
    }

    public static Class<?> getEntityClass() {
        return Entity.class;
    }

    public static Class<?> getPlayerClass() {
        return Player.class;
    }

    public static Class<?> getInventoryClass() {
        return Inventory.class;
    }

    public static Class<?> getServerPlayerClass() {
        return ServerPlayer.class;
    }

    public static Class<?> getAbstractContainerMenuClass() {
        return AbstractContainerMenu.class;
    }

    public static Class<?> getBlockStateClass() {
        return BlockState.class;
    }

    public static Class<?> getLevelClass() {
        return Level.class;
    }

    public static Object getWorldServerSessionId(Object levelObj) {
        if (!(levelObj instanceof Level level)) {
            return null;
        }
        MinecraftServer server = level.getServer();
        return server != null ? System.identityHashCode(server) : null;
    }

    public static boolean blockStateIsAir(Object state) {
        return ((BlockState) state).isAir();
    }

    public static Object blockStateGetBlock(Object state) {
        return ((BlockState) state).getBlock();
    }

    public static Object blockStateGetStateDefinition(Object state) {
        return ((BlockState) state).getBlock().getStateDefinition();
    }

    public static Object blockStateGetProperty(Object stateDef, Object propName) {
        String name = propName instanceof String s ? s : String.valueOf(propName);
        return ((StateDefinition<?, ?>) stateDef).getProperty(name);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object blockStateSetProperty(Object state, Object prop, Object value) {
        return ((BlockState) state).setValue((Property) prop, (Comparable) value);
    }

    public static Object itemStackOf(Object nbt) {
        return ItemStack.of((CompoundTag) nbt);
    }

    public static Object getEntityLevel(Object entity) {
        return ((Entity) entity).level();
    }

    public static Object getPlayerContainerMenu(Object player) {
        return ((Player) player).containerMenu;
    }

    public static Object getInventoryPlayer(Object inventory) {
        return ((Inventory) inventory).player;
    }

    public static int getMenuContainerId(Object menu) {
        return ((AbstractContainerMenu) menu).containerId;
    }

    public static Object playerRaytraceBlock(Object playerObj, double reach, boolean sourceOnly) {
        if (!(playerObj instanceof Player player)) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(reach));
        // `sourceOnly` is really a "detect fluid" switch: upstream
        // ItemMatterUnit rayTrace(useLiquids=true) hits ANY fluid state, not
        // just sources — a non-source imag-phase pool would never be
        // collectable under SOURCE_ONLY.
        ClipContext.Fluid fluid = sourceOnly ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
        HitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, fluid, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return (BlockHitResult) hit;
    }

    public static MobEffect getMobEffect(String fieldName) {
        return switch (fieldName) {
            case "MOVEMENT_SPEED" -> MobEffects.MOVEMENT_SPEED;
            case "MOVEMENT_SLOWDOWN" -> MobEffects.MOVEMENT_SLOWDOWN;
            case "JUMP" -> MobEffects.JUMP;
            case "REGENERATION" -> MobEffects.REGENERATION;
            case "DAMAGE_BOOST" -> MobEffects.DAMAGE_BOOST;
            case "DAMAGE_RESISTANCE" -> MobEffects.DAMAGE_RESISTANCE;
            case "HUNGER" -> MobEffects.HUNGER;
            case "BLINDNESS" -> MobEffects.BLINDNESS;
            case "DIG_SPEED" -> MobEffects.DIG_SPEED;
            case "DIG_SLOWDOWN" -> MobEffects.DIG_SLOWDOWN;
            case "CONFUSION" -> MobEffects.CONFUSION;
            case "INVISIBILITY" -> MobEffects.INVISIBILITY;
            case "NIGHT_VISION" -> MobEffects.NIGHT_VISION;
            case "WEAKNESS" -> MobEffects.WEAKNESS;
            case "POISON" -> MobEffects.POISON;
            case "WITHER" -> MobEffects.WITHER;
            case "HEALTH_BOOST" -> MobEffects.HEALTH_BOOST;
            case "ABSORPTION" -> MobEffects.ABSORPTION;
            case "SATURATION" -> MobEffects.SATURATION;
            case "GLOWING" -> MobEffects.GLOWING;
            case "LEVITATION" -> MobEffects.LEVITATION;
            case "LUCK" -> MobEffects.LUCK;
            case "UNLUCK" -> MobEffects.UNLUCK;
            case "SLOW_FALLING" -> MobEffects.SLOW_FALLING;
            case "CONDUIT_POWER" -> MobEffects.CONDUIT_POWER;
            case "DOLPHINS_GRACE" -> MobEffects.DOLPHINS_GRACE;
            case "BAD_OMEN" -> MobEffects.BAD_OMEN;
            case "HERO_OF_THE_VILLAGE" -> MobEffects.HERO_OF_THE_VILLAGE;
            case "DARKNESS" -> MobEffects.DARKNESS;
            default -> null;
        };
    }
}
