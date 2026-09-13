package cn.li.mc1211.runtime;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Raycast {
    private Raycast() {
    }

    public static Map<String, Object> raycastBlocks(
            Level level,
            double startX,
            double startY,
            double startZ,
            double dirX,
            double dirY,
            double dirZ,
            double maxDistance) {
        if (level == null) {
            return null;
        }

        Vec3 start = new Vec3(startX, startY, startZ);
        Vec3 end = new Vec3(startX + dirX * maxDistance, startY + dirY * maxDistance, startZ + dirZ * maxDistance);
        ClipContext clipContext = new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty());
        BlockHitResult result = level.clip(clipContext);

        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        var pos = result.getBlockPos();
        BlockState blockState = level.getBlockState(pos);
        Map<String, Object> hit = new LinkedHashMap<>();
        hit.put("x", pos.getX());
        hit.put("y", pos.getY());
        hit.put("z", pos.getZ());
        hit.put("hit-x", result.getLocation().x);
        hit.put("hit-y", result.getLocation().y);
        hit.put("hit-z", result.getLocation().z);
        hit.put("hit-type", "block");
        hit.put("block-id", blockState.getBlock().getDescriptionId());
        hit.put("face", result.getDirection().getSerializedName());
        hit.put("distance", start.distanceTo(result.getLocation()));
        return hit;
    }

    /**
     * Trace the same block set used by AcademyCraft ArcGen: ordinary blocks
     * with a collision shape, plus water (including flowing water). Other
     * non-collidable blocks and fluids are transparent to the trace.
     */
    public static Map<String, Object> raycastCollidableBlocksOrWater(
            Level level,
            double startX,
            double startY,
            double startZ,
            double dirX,
            double dirY,
            double dirZ,
            double maxDistance) {
        if (level == null) {
            return null;
        }

        Vec3 start = new Vec3(startX, startY, startZ);
        Vec3 end = new Vec3(startX + dirX * maxDistance, startY + dirY * maxDistance, startZ + dirZ * maxDistance);
        BlockHitResult collidableResult = level.clip(
                new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                        net.minecraft.world.phys.shapes.CollisionContext.empty()));
        Map<String, Object> collidableHit =
                collidableResult == null || collidableResult.getType() != HitResult.Type.BLOCK
                        ? null
                        : buildBlockHit(level, start, collidableResult);
        Map<String, Object> waterHit = raycastBlocksMatching(
                level,
                startX, startY, startZ,
                dirX, dirY, dirZ,
                maxDistance,
                List.of("minecraft:water"));

        if (collidableHit == null) {
            return waterHit;
        }
        if (waterHit == null) {
            return collidableHit;
        }
        double collidableDistance = ((Number) collidableHit.get("distance")).doubleValue();
        double waterDistance = ((Number) waterHit.get("distance")).doubleValue();
        return collidableDistance <= waterDistance ? collidableHit : waterHit;
    }

    private static Map<String, Object> buildBlockHit(
            Level level,
            Vec3 start,
            BlockHitResult result) {
        BlockPos pos = result.getBlockPos();
        BlockState blockState = level.getBlockState(pos);
        Map<String, Object> hit = new LinkedHashMap<>();
        hit.put("x", pos.getX());
        hit.put("y", pos.getY());
        hit.put("z", pos.getZ());
        hit.put("hit-x", result.getLocation().x);
        hit.put("hit-y", result.getLocation().y);
        hit.put("hit-z", result.getLocation().z);
        hit.put("hit-type", "block");
        hit.put("block-id", BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString());
        hit.put("face", result.getDirection().getSerializedName());
        hit.put("distance", start.distanceTo(result.getLocation()));
        return hit;
    }

    /**
     * Trace only blocks whose registry ids are accepted, allowing the ray to
     * pass through every other block. This matches LambdaLib2's filtered block
     * selector used by AcademyCraft's MagManip acquisition trace.
     */
    public static Map<String, Object> raycastBlocksMatching(
            Level level,
            double startX,
            double startY,
            double startZ,
            double dirX,
            double dirY,
            double dirZ,
            double maxDistance,
            Collection<?> acceptedBlockIds) {
        if (level == null || acceptedBlockIds == null || acceptedBlockIds.isEmpty() || maxDistance < 0.0D) {
            return null;
        }
        Set<String> accepted = new HashSet<>();
        for (Object id : acceptedBlockIds) {
            if (id != null) {
                accepted.add(String.valueOf(id).toLowerCase(java.util.Locale.ROOT));
            }
        }
        Vec3 rawDirection = new Vec3(dirX, dirY, dirZ);
        if (rawDirection.lengthSqr() < 1.0E-12D) {
            return null;
        }
        Vec3 start = new Vec3(startX, startY, startZ);
        Vec3 end = start.add(rawDirection.normalize().scale(maxDistance));
        int steps = Math.max(1, (int) Math.ceil(maxDistance * 32.0D));
        BlockPos previous = null;
        for (int index = 0; index <= steps; index++) {
            double fraction = (double) index / (double) steps;
            Vec3 sample = start.lerp(end, fraction);
            BlockPos pos = BlockPos.containing(sample);
            if (pos.equals(previous)) {
                continue;
            }
            previous = pos;
            BlockState state = level.getBlockState(pos);
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (!accepted.contains(blockId.toLowerCase(java.util.Locale.ROOT))) {
                continue;
            }
            BlockHitResult result = state.getShape(level, pos).clip(start, end, pos);
            if (result == null && !state.getFluidState().isEmpty()) {
                result = state.getFluidState().getShape(level, pos).clip(start, end, pos);
            }
            if (result == null || result.getType() != HitResult.Type.BLOCK) {
                continue;
            }
            Map<String, Object> hit = new LinkedHashMap<>();
            hit.put("x", pos.getX());
            hit.put("y", pos.getY());
            hit.put("z", pos.getZ());
            hit.put("hit-x", result.getLocation().x);
            hit.put("hit-y", result.getLocation().y);
            hit.put("hit-z", result.getLocation().z);
            hit.put("hit-type", "block");
            hit.put("block-id", blockId);
            hit.put("face", result.getDirection().getSerializedName());
            hit.put("distance", start.distanceTo(result.getLocation()));
            return hit;
        }
        return null;
    }

    public static Map<String, Object> raycastEntities(
            Level level,
            double startX,
            double startY,
            double startZ,
            double dirX,
            double dirY,
            double dirZ,
            double maxDistance) {
        if (level == null) {
            return null;
        }

        Vec3 start = new Vec3(startX, startY, startZ);
        Vec3 end = new Vec3(startX + dirX * maxDistance, startY + dirY * maxDistance, startZ + dirZ * maxDistance);
        AABB searchBox = createSearchBox(startX, startY, startZ, dirX, dirY, dirZ, maxDistance).inflate(2.0D);

        Map<String, Object> nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        // Original LambdaLib2 Raytrace.rayTraceEntities selects every entity
        // whose canBeCollidedWith() is true (1.20: isPickable()) — the
        // silbarn (a non-LivingEntity scripted block body) must be hittable
        // by ray-barrage. LivingEntity stays as a baseline so the trace
        // never regresses for mobs/players regardless of their pickability.
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, searchBox,
                e -> e.isPickable() || e instanceof LivingEntity);

        for (Entity entity : entities) {
            Optional<Vec3> optionalHit = entity.getBoundingBox().inflate(0.3D).clip(start, end);

            if (optionalHit.isEmpty()) {
                continue;
            }

            Vec3 hitVec = optionalHit.get();
            double distance = start.distanceTo(hitVec);

            if (distance >= nearestDistance) {
                continue;
            }

            nearestDistance = distance;
            nearest = new LinkedHashMap<>();
            nearest.put("hit-type", "entity");
            nearest.put("uuid", entity.getUUID().toString());
            nearest.put("x", entity.position().x);
            nearest.put("y", entity.position().y);
            nearest.put("z", entity.position().z);
            nearest.put("hit-x", hitVec.x);
            nearest.put("hit-y", hitVec.y);
            nearest.put("hit-z", hitVec.z);
            nearest.put("eye-height", entity.getEyeHeight());
            nearest.put("width", entity.getBbWidth());
            nearest.put("height", entity.getBbHeight());
            nearest.put("type", entity.getType().getDescriptionId());
            nearest.put("distance", distance);
        }

        return nearest;
    }

    public static Map<String, Object> raycastCombined(
            Level level,
            double startX,
            double startY,
            double startZ,
            double dirX,
            double dirY,
            double dirZ,
            double maxDistance) {
        Map<String, Object> blockHit = raycastBlocks(level, startX, startY, startZ, dirX, dirY, dirZ, maxDistance);
        Map<String, Object> entityHit = raycastEntities(level, startX, startY, startZ, dirX, dirY, dirZ, maxDistance);

        if (blockHit == null) {
            if (entityHit != null) {
                entityHit.put("hit-type", "entity");
            }
            return entityHit;
        }

        if (entityHit == null) {
            blockHit.put("hit-type", "block");
            return blockHit;
        }

        double blockDistance = ((Number) blockHit.get("distance")).doubleValue();
        double entityDistance = ((Number) entityHit.get("distance")).doubleValue();

        if (blockDistance <= entityDistance) {
            blockHit.put("hit-type", "block");
            return blockHit;
        }

        entityHit.put("hit-type", "entity");
        return entityHit;
    }

    public static Map<String, Object> raycastCombinedExcluding(
            Level level,
            double startX,
            double startY,
            double startZ,
            double dirX,
            double dirY,
            double dirZ,
            double maxDistance,
            String excludedEntityUuid) {
        Map<String, Object> blockHit = raycastBlocks(
                level, startX, startY, startZ,
                dirX, dirY, dirZ, maxDistance);
        Map<String, Object> entityHit = raycastEntitiesExcluding(
                level, startX, startY, startZ,
                dirX, dirY, dirZ, maxDistance,
                excludedEntityUuid);
        if (blockHit == null) {
            if (entityHit != null) {
                entityHit.put("hit-type", "entity");
            }
            return entityHit;
        }
        if (entityHit == null) {
            blockHit.put("hit-type", "block");
            return blockHit;
        }
        double blockDistance = ((Number) blockHit.get("distance")).doubleValue();
        double entityDistance = ((Number) entityHit.get("distance")).doubleValue();
        if (blockDistance <= entityDistance) {
            blockHit.put("hit-type", "block");
            return blockHit;
        }
        entityHit.put("hit-type", "entity");
        return entityHit;
    }

    private static Map<String, Object> raycastEntitiesExcluding(
            Level level,
            double startX,
            double startY,
            double startZ,
            double dirX,
            double dirY,
            double dirZ,
            double maxDistance,
            String excludedEntityUuid) {
        if (level == null) {
            return null;
        }
        Vec3 start = new Vec3(startX, startY, startZ);
        Vec3 end = new Vec3(
                startX + dirX * maxDistance,
                startY + dirY * maxDistance,
                startZ + dirZ * maxDistance);
        AABB searchBox = createSearchBox(
                startX, startY, startZ,
                dirX, dirY, dirZ,
                maxDistance).inflate(2.0D);
        Map<String, Object> nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (excludedEntityUuid != null
                    && excludedEntityUuid.equalsIgnoreCase(entity.getUUID().toString())) {
                continue;
            }
            Optional<Vec3> optionalHit = entity.getBoundingBox().inflate(0.3D).clip(start, end);
            if (optionalHit.isEmpty()) {
                continue;
            }
            Vec3 hitVec = optionalHit.get();
            double distance = start.distanceTo(hitVec);
            if (distance >= nearestDistance) {
                continue;
            }
            nearestDistance = distance;
            nearest = new LinkedHashMap<>();
            nearest.put("hit-type", "entity");
            nearest.put("uuid", entity.getUUID().toString());
            nearest.put("x", entity.position().x);
            nearest.put("y", entity.position().y);
            nearest.put("z", entity.position().z);
            nearest.put("hit-x", hitVec.x);
            nearest.put("hit-y", hitVec.y);
            nearest.put("hit-z", hitVec.z);
            nearest.put("eye-height", entity.getEyeHeight());
            nearest.put("width", entity.getBbWidth());
            nearest.put("height", entity.getBbHeight());
            nearest.put("type", entity.getType().getDescriptionId());
            nearest.put("distance", distance);
        }
        return nearest;
    }

    public static Map<String, Object> raycastAllEntities(
            Level level,
            double startX,
            double startY,
            double startZ,
            double dirX,
            double dirY,
            double dirZ,
            double maxDistance) {
        if (level == null) {
            return null;
        }

        Vec3 start = new Vec3(startX, startY, startZ);
        Vec3 end = new Vec3(
                startX + dirX * maxDistance,
                startY + dirY * maxDistance,
                startZ + dirZ * maxDistance);
        AABB searchBox = createSearchBox(
                startX, startY, startZ,
                dirX, dirY, dirZ,
                maxDistance).inflate(2.0D);

        Map<String, Object> nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, searchBox);

        for (Entity entity : entities) {
            if (!entity.isPickable()) {
                continue;
            }
            Optional<Vec3> optionalHit = entity.getBoundingBox().inflate(0.3D).clip(start, end);
            if (optionalHit.isEmpty()) {
                continue;
            }
            Vec3 hitVec = optionalHit.get();
            double distance = start.distanceTo(hitVec);
            if (distance >= nearestDistance) {
                continue;
            }
            nearestDistance = distance;
            nearest = new LinkedHashMap<>();
            nearest.put("hit-type", "entity");
            nearest.put("uuid", entity.getUUID().toString());
            nearest.put("x", entity.position().x);
            nearest.put("y", entity.position().y);
            nearest.put("z", entity.position().z);
            nearest.put("hit-x", hitVec.x);
            nearest.put("hit-y", hitVec.y);
            nearest.put("hit-z", hitVec.z);
            nearest.put("eye-height", entity.getEyeHeight());
            nearest.put("width", entity.getBbWidth());
            nearest.put("height", entity.getBbHeight());
            nearest.put("type", entity.getType().getDescriptionId());
            nearest.put("distance", distance);
        }
        return nearest;
    }

    public static Map<String, Object> raycastCombinedAll(
            Level level,
            double startX,
            double startY,
            double startZ,
            double dirX,
            double dirY,
            double dirZ,
            double maxDistance) {
        Map<String, Object> blockHit = raycastBlocks(
                level, startX, startY, startZ,
                dirX, dirY, dirZ, maxDistance);
        Map<String, Object> entityHit = raycastAllEntities(
                level, startX, startY, startZ,
                dirX, dirY, dirZ, maxDistance);

        if (blockHit == null) {
            if (entityHit != null) {
                entityHit.put("hit-type", "entity");
            }
            return entityHit;
        }
        if (entityHit == null) {
            blockHit.put("hit-type", "block");
            return blockHit;
        }
        double blockDistance = ((Number) blockHit.get("distance")).doubleValue();
        double entityDistance = ((Number) entityHit.get("distance")).doubleValue();
        if (blockDistance <= entityDistance) {
            blockHit.put("hit-type", "block");
            return blockHit;
        }
        entityHit.put("hit-type", "entity");
        return entityHit;
    }

    public static Map<String, Object> raycastCombinedFromPlayer(
            Player player,
            double maxDistance,
            boolean livingOnly) {
        if (player == null) {
            return null;
        }
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Map<String, Object> blockHit = raycastBlocks(
                player.level(),
                eyePos.x, eyePos.y, eyePos.z,
                lookVec.x, lookVec.y, lookVec.z,
                maxDistance);
        Map<String, Object> entityHit = raycastFromPlayer(player, maxDistance, livingOnly);
        if (blockHit == null) {
            if (entityHit != null) {
                entityHit.put("hit-type", "entity");
            }
            return entityHit;
        }
        if (entityHit == null) {
            blockHit.put("hit-type", "block");
            return blockHit;
        }
        double blockDistance = ((Number) blockHit.get("distance")).doubleValue();
        double entityDistance = ((Number) entityHit.get("distance")).doubleValue();
        if (blockDistance <= entityDistance) {
            blockHit.put("hit-type", "block");
            return blockHit;
        }
        entityHit.put("hit-type", "entity");
        return entityHit;
    }

    public static Map<String, Object> getPlayerLookVector(Player player) {
        if (player == null) {
            return null;
        }

        Vec3 lookVec = player.getLookAngle();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("x", lookVec.x);
        result.put("y", lookVec.y);
        result.put("z", lookVec.z);
        return result;
    }

    public static Map<String, Object> getPlayerPosition(Player player) {
        if (player == null) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("x", player.getX());
        result.put("y", player.getY());
        result.put("z", player.getZ());
        result.put("eye-y", player.getEyeY());
        result.put("world-id", player.level().dimension().location().toString());
        return result;
    }

    public static Map<String, Object> raycastFromPlayer(Player player, double maxDistance, boolean livingOnly) {
        if (player == null) {
            return null;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 end = eyePos.add(lookVec.scale(maxDistance));
        AABB searchBox = createSearchBox(eyePos.x, eyePos.y, eyePos.z, lookVec.x, lookVec.y, lookVec.z, maxDistance).inflate(2.0D);

        Map<String, Object> nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        if (livingOnly) {
            for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, searchBox)) {
                double candidateDistance = updateNearestPlayerHit(player, eyePos, end, entity, nearestDistance);
                if (candidateDistance < nearestDistance) {
                    nearestDistance = candidateDistance;
                    nearest = buildEntityHitMap(entity, eyePos, end, nearestDistance);
                }
            }
        } else {
            for (Entity entity : player.level().getEntitiesOfClass(Entity.class, searchBox)) {
                double candidateDistance = updateNearestPlayerHit(player, eyePos, end, entity, nearestDistance);
                if (candidateDistance < nearestDistance) {
                    nearestDistance = candidateDistance;
                    nearest = buildEntityHitMap(entity, eyePos, end, nearestDistance);
                }
            }
        }

        return nearest;
    }

    private static AABB createSearchBox(
            double startX,
            double startY,
            double startZ,
            double dirX,
            double dirY,
            double dirZ,
            double maxDistance) {
        double endX = startX + dirX * maxDistance;
        double endY = startY + dirY * maxDistance;
        double endZ = startZ + dirZ * maxDistance;
        return new AABB(
                Math.min(startX, endX),
                Math.min(startY, endY),
                Math.min(startZ, endZ),
                Math.max(startX, endX),
                Math.max(startY, endY),
                Math.max(startZ, endZ));
    }

    private static double updateNearestPlayerHit(Player player, Vec3 start, Vec3 end, Entity entity, double currentNearest) {
        if (entity == player || !entity.isPickable()) {
            return currentNearest;
        }

        Optional<Vec3> optionalHit = entity.getBoundingBox().inflate(0.3D).clip(start, end);

        if (optionalHit.isEmpty()) {
            return currentNearest;
        }

        double distance = start.distanceTo(optionalHit.get());
        return Math.min(currentNearest, distance);
    }

    private static Map<String, Object> buildEntityHitMap(Entity entity, Vec3 start, Vec3 end, double nearestDistance) {
        Optional<Vec3> optionalHit = entity.getBoundingBox().inflate(0.3D).clip(start, end);

        if (optionalHit.isEmpty()) {
            return null;
        }

        double distance = start.distanceTo(optionalHit.get());

        if (distance != nearestDistance) {
            return null;
        }

        Map<String, Object> hit = new LinkedHashMap<>();
        hit.put("hit-type", "entity");
        hit.put("entity-id", entity.getUUID().toString());
        hit.put("x", entity.position().x);
        hit.put("y", entity.position().y);
        hit.put("z", entity.position().z);
        Vec3 hitVec = optionalHit.get();
        hit.put("hit-x", hitVec.x);
        hit.put("hit-y", hitVec.y);
        hit.put("hit-z", hitVec.z);
        hit.put("eye-height", entity.getEyeHeight());
        hit.put("width", entity.getBbWidth());
        hit.put("height", entity.getBbHeight());
        // The original RayTraceResult retains entityHit directly. Preserve
        // its type here so MagMovement can identify an anchored mag hook as
        // a configured metallic entity during acquisition.
        hit.put("type", entity.getType().getDescriptionId());
        hit.put("distance", distance);
        return hit;
    }
}
