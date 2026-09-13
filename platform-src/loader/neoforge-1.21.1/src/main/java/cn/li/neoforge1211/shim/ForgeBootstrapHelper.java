package cn.li.neoforge1211.shim;

import cn.li.neoforge1211.block.entity.ScriptedBlockEntity;
import cn.li.mcbase.block.ScriptedLiquidBlock;
import cn.li.mc1211.block.SharedBootstrapBlockHelper;
import cn.li.mc1211.block.SharedDynamicStateBlock;
import cn.li.mc1211.block.SharedScriptedBlock;
import cn.li.mc1211.block.ScriptedRenderShapes;
import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mc1211.runtime.BlockRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.lang.reflect.Constructor;
import java.util.function.Function;
import java.util.List;
import java.util.function.Supplier;

public final class ForgeBootstrapHelper {
    private ForgeBootstrapHelper() {
    }

    private static RenderShape resolveRenderShape(String blockId) {
        return ScriptedRenderShapes.resolveDefault(blockId);
    }

    public static DeferredRegister<Block> createBlocksRegister(String modId) {
        return DeferredRegister.create(Registries.BLOCK, modId);
    }

    public static DeferredRegister<Item> createItemsRegister(String modId) {
        return DeferredRegister.create(Registries.ITEM, modId);
    }

    public static DeferredRegister<BlockEntityType<?>> createBlockEntityTypesRegister(String modId) {
        return DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, modId);
    }

    public static DeferredRegister<CreativeModeTab> createCreativeTabsRegister(String modId) {
        return DeferredRegister.create(Registries.CREATIVE_MODE_TAB, modId);
    }

    public static DeferredRegister<MenuType<?>> createMenusRegister(String modId) {
        return DeferredRegister.create(Registries.MENU, modId);
    }

    public static DeferredRegister<FluidType> createFluidTypesRegister(String modId) {
        return DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, modId);
    }

    public static DeferredRegister<Fluid> createFluidsRegister(String modId) {
        return DeferredRegister.create(Registries.FLUID, modId);
    }

    public static DeferredRegister<SoundEvent> createSoundsRegister(String modId) {
        return DeferredRegister.create(Registries.SOUND_EVENT, modId);
    }

    public static DeferredRegister<MobEffect> createEffectsRegister(String modId) {
        return DeferredRegister.create(Registries.MOB_EFFECT, modId);
    }

    public static DeferredRegister<ParticleType<?>> createParticleTypesRegister(String modId) {
        return DeferredRegister.create(Registries.PARTICLE_TYPE, modId);
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

    public static Block createCarrierScriptedDynamicBlock(String blockId, String tileId, List<Property<?>> properties, BlockBehaviour.Properties blockProperties) {
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

    public static Block createDynamicStateBlock(String blockId, List<Property<?>> properties, BlockBehaviour.Properties blockProperties) {
        return SharedBootstrapBlockHelper.createDynamicStateBlock(blockId, properties, blockProperties);
    }

    public static Block createCarrierScriptedBlock(String blockId, String tileId, BlockBehaviour.Properties blockProperties) {
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

    public static FluidType createFluidType(int luminosity,
                                            int density,
                                            int viscosity,
                                            int temperature,
                                            boolean canHydrate,
                                            boolean supportsBoating,
                                            String stillTexture,
                                            String flowingTexture,
                                            String overlayTexture,
                                            int tintColor) {
        FluidType.Properties props = FluidType.Properties.create()
            .lightLevel(luminosity)
            .density(density)
            .viscosity(viscosity)
            .temperature(temperature)
            .canHydrate(canHydrate)
            .supportsBoating(supportsBoating);
        return new ForgeFlowingFluidType(
            props,
            ResourceLocation.parse(stillTexture),
            ResourceLocation.parse(flowingTexture),
            overlayTexture == null ? null : ResourceLocation.parse(overlayTexture),
            tintColor
        );
    }

    public static BaseFlowingFluid.Properties createFlowingFluidProperties(
        Supplier<FluidType> fluidTypeSupplier,
        Supplier<? extends FlowingFluid> sourceSupplier,
        Supplier<? extends FlowingFluid> flowingSupplier,
        Supplier<? extends Item> bucketSupplier,
        Supplier<? extends LiquidBlock> blockSupplier,
        int slopeFindDistance,
        int levelDecreasePerBlock,
        int tickRate,
        float explosionResistance,
        boolean canConvertToSource
    ) {
        BaseFlowingFluid.Properties properties =
            new BaseFlowingFluid.Properties(fluidTypeSupplier, sourceSupplier, flowingSupplier)
                .slopeFindDistance(slopeFindDistance)
                .levelDecreasePerBlock(levelDecreasePerBlock)
                .tickRate(tickRate)
                .explosionResistance(explosionResistance);
        if (bucketSupplier != null) {
            properties = properties.bucket(bucketSupplier);
        }
        if (blockSupplier != null) {
            properties = properties.block(blockSupplier);
        }
        // NeoForge 1.21.1 API does not expose a configurable canConvertToSource setter.
        // Keep parameter for cross-platform metadata parity; behavior follows default.
        return properties;
    }

    public static Fluid createSourceFluid(BaseFlowingFluid.Properties properties) {
        return new BaseFlowingFluid.Source(properties);
    }

    public static Fluid createFlowingFluid(BaseFlowingFluid.Properties properties) {
        return new BaseFlowingFluid.Flowing(properties);
    }

    public static Block createLiquidBlock(Supplier<? extends FlowingFluid> fluidSupplier, int lightLevel) {
        return new LiquidBlock(fluidSupplier.get(), SharedBootstrapBlockHelper.withLightLevel(
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
            fluidSupplier.get(),
            new Item.Properties()
                .stacksTo(1)
                .craftRemainder(Items.BUCKET)
        );
    }

    @SuppressWarnings("unchecked")
    public static EntityType<?> createEntityType(String fullId,
                                                 Class<?> entityClass,
                                                 String category,
                                                 float width,
                                                 float height,
                                                 int clientTrackingRange,
                                                 int updateInterval,
                                                 boolean fireImmune) {
        Class<? extends Entity> typedClass = (Class<? extends Entity>) entityClass;
        Constructor<? extends Entity> ctor;
        try {
            ctor = typedClass.getConstructor(EntityType.class, net.minecraft.world.level.Level.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Entity class must have constructor (EntityType, Level): " + typedClass.getName(), e);
        }
        EntityType.EntityFactory<Entity> factory = (type, level) -> {
            try {
                return ctor.newInstance(type, level);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to instantiate entity: " + typedClass.getName(), e);
            }
        };
        MobCategory mobCategory = switch (String.valueOf(category)) {
            case "monster" -> MobCategory.MONSTER;
            case "creature" -> MobCategory.CREATURE;
            case "ambient" -> MobCategory.AMBIENT;
            case "water_creature" -> MobCategory.WATER_CREATURE;
            case "water_ambient" -> MobCategory.WATER_AMBIENT;
            case "misc" -> MobCategory.MISC;
            default -> MobCategory.MISC;
        };
        EntityType.Builder<Entity> builder = EntityType.Builder.of(factory, mobCategory)
            .sized(width, height)
            .clientTrackingRange(clientTrackingRange)
            .updateInterval(updateInterval);
        if (fireImmune) {
            builder = builder.fireImmune();
        }
        return builder.build(fullId);
    }

    public static EntityType<?> createEntityTypeByKind(String fullId,
                                                       String entityKind,
                                                       String category,
                                                       float width,
                                                       float height,
                                                       int clientTrackingRange,
                                                       int updateInterval,
                                                       boolean fireImmune) {
        Class<?> entityClass = ScriptedEntitySpecAccess.resolveEntityClassByKind(entityKind);
        if (entityClass == null) {
            throw new IllegalStateException("Unknown entity kind: " + entityKind);
        }
        return createEntityType(
            fullId,
            entityClass,
            category,
            width,
            height,
            clientTrackingRange,
            updateInterval,
            fireImmune
        );
    }

    @SuppressWarnings("unchecked")
    public static BlockEntityType<?> createScriptedBlockEntityType(String tileId,
                                                                   List<Block> blocks,
                                                                   Function<Block, String> blockIdResolver) {
        return SharedBootstrapBlockHelper.createScriptedBlockEntityType(
            tileId,
            blocks,
            blockIdResolver,
            (type, pos, state, resolvedTileId, blockId) -> new cn.li.neoforge1211.block.entity.ScriptedBlockEntity(type, pos, state, resolvedTileId, blockId),
            cn.li.neoforge1211.block.entity.ScriptedBlockEntity::registerType
        );
    }

    public static boolean isAirBlock(Block block, Block airBlock) {
        return BlockRegistry.isAirBlock(block, airBlock);
    }

    public static Block getAirBlock() {
        return BlockRegistry.getAirBlock();
    }

    public static Block findBlock(String namespace, String path) {
        return BlockRegistry.findBlock(namespace, path);
    }
}
