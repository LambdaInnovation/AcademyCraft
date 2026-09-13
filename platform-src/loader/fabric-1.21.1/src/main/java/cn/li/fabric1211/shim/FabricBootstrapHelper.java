package cn.li.fabric1211.shim;

import cn.li.mcver.ResourceLocations;

import cn.li.fabric1211.block.entity.ScriptedBlockEntity;
import cn.li.mcbase.block.ScriptedLiquidBlock;
import cn.li.mc1211.block.SharedBootstrapBlockHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricBootstrapHelper {

    private FabricBootstrapHelper() {
    }

    public static BlockBehaviour.Properties createStoneProperties() {
        return SharedBootstrapBlockHelper.createStoneProperties();
    }

    public static BlockBehaviour.Properties createBlockProperties(String material, float hardness, float resistance, boolean requiresCorrectTool) {
        return SharedBootstrapBlockHelper.createBlockProperties(material, hardness, resistance, requiresCorrectTool);
    }

    public static BlockBehaviour.Properties carrierBlockProperties(BlockBehaviour.Properties base) {
        return SharedBootstrapBlockHelper.carrierBlockProperties(base);
    }

    public static Block createCarrierScriptedDynamicBlock(String blockId,
                                                           String tileId,
                                                           List<Property<?>> properties,
                                                           BlockBehaviour.Properties blockProperties) {
        return SharedBootstrapBlockHelper.createCarrierScriptedDynamicBlock(
            blockId,
            tileId,
            properties,
            blockProperties,
            (resolvedTileId, resolvedBlockId, pos, state) -> {
                BlockEntityType<ScriptedBlockEntity> type = ScriptedBlockEntity.getType(resolvedTileId);
                return type != null ? new ScriptedBlockEntity(type, pos, state, resolvedTileId, resolvedBlockId) : null;
            },
            (level, pos, state, blockEntity) -> {
                if (blockEntity instanceof ScriptedBlockEntity scripted) {
                    ScriptedBlockEntity.serverTick(level, pos, state, scripted);
                }
            }
        );
    }

    public static Block createDynamicStateBlock(String blockId,
                                                List<Property<?>> properties,
                                                BlockBehaviour.Properties blockProperties) {
        return SharedBootstrapBlockHelper.createDynamicStateBlock(blockId, properties, blockProperties);
    }

    public static Block createCarrierScriptedBlock(String blockId,
                                                   String tileId,
                                                   BlockBehaviour.Properties blockProperties) {
        return SharedBootstrapBlockHelper.createCarrierScriptedBlock(
            blockId,
            tileId,
            blockProperties,
            (resolvedTileId, resolvedBlockId, pos, state) -> {
                BlockEntityType<ScriptedBlockEntity> type = ScriptedBlockEntity.getType(resolvedTileId);
                return type != null ? new ScriptedBlockEntity(type, pos, state, resolvedTileId, resolvedBlockId) : null;
            },
            (level, pos, state, blockEntity) -> {
                if (blockEntity instanceof ScriptedBlockEntity scripted) {
                    ScriptedBlockEntity.serverTick(level, pos, state, scripted);
                }
            }
        );
    }

    public static Block createPlainBlock(BlockBehaviour.Properties blockProperties) {
        return SharedBootstrapBlockHelper.createPlainBlock(blockProperties);
    }

    public static Block createLiquidBlock(Supplier<? extends FlowingFluid> fluidSupplier, int lightLevel) {
        return new LiquidBlock(Objects.requireNonNull(fluidSupplier.get(), "fluid"),
            SharedBootstrapBlockHelper.withLightLevel(
                BlockBehaviour.Properties.ofFullCopy(Blocks.WATER), lightLevel));
    }

    public static Block createScriptedLiquidBlock(Supplier<? extends FlowingFluid> fluidSupplier,
                                                   String blockId,
                                                   String tileId,
                                                   int lightLevel) {
        return new ScriptedLiquidBlock(
            fluidSupplier,
            blockId,
            tileId,
            SharedBootstrapBlockHelper.withLightLevel(
                BlockBehaviour.Properties.ofFullCopy(Blocks.WATER), lightLevel),
            (resolvedTileId, resolvedBlockId, pos, state) -> {
                BlockEntityType<ScriptedBlockEntity> type = ScriptedBlockEntity.getType(resolvedTileId);
                return type != null ? new ScriptedBlockEntity(type, pos, state, resolvedTileId, resolvedBlockId) : null;
            },
            (level, pos, state, blockEntity) -> {
                if (blockEntity instanceof ScriptedBlockEntity scripted) {
                    ScriptedBlockEntity.serverTick(level, pos, state, scripted);
                }
            });
    }

    public static Item createFluidBucket(Supplier<? extends Fluid> fluidSupplier) {
        return new net.minecraft.world.item.BucketItem(
            Objects.requireNonNull(fluidSupplier.get(), "fluid"),
            new Item.Properties()
                .stacksTo(1)
                .craftRemainder(Items.BUCKET)
        );
    }

    public static FabricFlowingFluid.Properties createFlowingFluidProperties(
            Supplier<? extends Fluid> sourceSupplier,
            Supplier<? extends Fluid> flowingSupplier,
            Supplier<? extends Item> bucketSupplier,
            Supplier<? extends LiquidBlock> blockSupplier,
            int slopeFindDistance,
            int levelDecreasePerBlock,
            int tickRate,
            float explosionResistance,
            boolean canConvertToSource) {
        FabricFlowingFluid.Properties properties =
            new FabricFlowingFluid.Properties(sourceSupplier, flowingSupplier)
                .slopeFindDistance(slopeFindDistance)
                .levelDecreasePerBlock(levelDecreasePerBlock)
                .tickRate(tickRate)
                .explosionResistance(explosionResistance)
                .canConvertToSource(canConvertToSource);
        if (bucketSupplier != null) {
            properties = properties.bucket(bucketSupplier);
        }
        if (blockSupplier != null) {
            properties = properties.block(blockSupplier);
        }
        return properties;
    }

    public static Fluid createSourceFluid(FabricFlowingFluid.Properties properties) {
        return new FabricFlowingFluid.Source(properties);
    }

    public static Fluid createFlowingFluid(FabricFlowingFluid.Properties properties) {
        return new FabricFlowingFluid.Flowing(properties);
    }

    public static Fluid registerFluid(String modId, String id, Fluid fluid) {
        return Registry.register(BuiltInRegistries.FLUID, ResourceLocations.of(modId, id), fluid);
    }

    public static Item createBlockItem(Block block) {
        return SharedBootstrapBlockHelper.createBlockItem(block);
    }

    @SuppressWarnings("unchecked")
    public static BlockEntityType<?> createScriptedBlockEntityType(String tileId,
                                                                    List<Block> blocks,
                                                                    Function<Block, String> blockIdResolver) {
        return SharedBootstrapBlockHelper.createScriptedBlockEntityType(
            tileId,
            blocks,
            blockIdResolver,
            (type, pos, state, resolvedTileId, blockId) -> new ScriptedBlockEntity(type, pos, state, resolvedTileId, blockId),
            ScriptedBlockEntity::registerType
        );
    }

    public static Block registerBlock(String modId, String id, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, ResourceLocations.of(modId, id), block);
    }

    public static Item registerItem(String modId, String id, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocations.of(modId, id), item);
    }

    public static BlockEntityType<?> registerBlockEntityType(String modId, String id, BlockEntityType<?> type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ResourceLocations.of(modId, id), type);
    }
}
