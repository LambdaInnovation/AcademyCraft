package cn.li.fabric1201.shim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.function.Supplier;

/**
 * Fabric equivalent of Forge's ForgeFlowingFluid — custom source/flowing pair
 * wired from fluid DSL metadata (no FluidType registry on Fabric).
 */
public abstract class FabricFlowingFluid extends FlowingFluid {
    private final Properties properties;

    protected FabricFlowingFluid(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Fluid getFlowing() {
        return properties.flowing.get();
    }

    @Override
    public Fluid getSource() {
        return properties.source.get();
    }

    @Override
    protected boolean canConvertToSource(Level level) {
        return properties.canConvertToSource;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockEntity be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, be);
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return properties.slopeFindDistance;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return properties.levelDecreasePerBlock;
    }

    @Override
    public Item getBucket() {
        return properties.bucket != null ? properties.bucket.get() : Items.AIR;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !isSame(fluid);
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return properties.tickRate;
    }

    @Override
    protected float getExplosionResistance() {
        return properties.explosionResistance;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        if (properties.block != null) {
            return properties.block.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == getSource() || fluid == getFlowing();
    }

    public static final class Properties {
        private final Supplier<? extends Fluid> source;
        private final Supplier<? extends Fluid> flowing;
        private Supplier<? extends Item> bucket;
        private Supplier<? extends LiquidBlock> block;
        private int slopeFindDistance = 4;
        private int levelDecreasePerBlock = 1;
        private int tickRate = 5;
        private float explosionResistance = 100.0f;
        private boolean canConvertToSource = true;

        public Properties(Supplier<? extends Fluid> source, Supplier<? extends Fluid> flowing) {
            this.source = source;
            this.flowing = flowing;
        }

        public Properties bucket(Supplier<? extends Item> bucket) {
            this.bucket = bucket;
            return this;
        }

        public Properties block(Supplier<? extends LiquidBlock> block) {
            this.block = block;
            return this;
        }

        public Properties slopeFindDistance(int slopeFindDistance) {
            this.slopeFindDistance = slopeFindDistance;
            return this;
        }

        public Properties levelDecreasePerBlock(int levelDecreasePerBlock) {
            this.levelDecreasePerBlock = levelDecreasePerBlock;
            return this;
        }

        public Properties tickRate(int tickRate) {
            this.tickRate = tickRate;
            return this;
        }

        public Properties explosionResistance(float explosionResistance) {
            this.explosionResistance = explosionResistance;
            return this;
        }

        public Properties canConvertToSource(boolean canConvertToSource) {
            this.canConvertToSource = canConvertToSource;
            return this;
        }
    }

    public static class Source extends FabricFlowingFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends FabricFlowingFluid {
        public Flowing(Properties properties) {
            super(properties);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
