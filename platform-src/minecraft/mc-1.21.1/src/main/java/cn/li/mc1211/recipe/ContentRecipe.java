package cn.li.mc1211.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.function.Supplier;

/** Loader-agnostic content machine recipe (process / mode variants). */
public final class ContentRecipe implements Recipe<SingleRecipeInput> {
    private static volatile Supplier<RecipeType<?>> processType = () -> null;
    private static volatile Supplier<RecipeType<?>> modeType = () -> null;
    private static volatile Supplier<RecipeSerializer<?>> processSerializer = () -> null;
    private static volatile Supplier<RecipeSerializer<?>> modeSerializer = () -> null;

    private final Ingredient input;
    private final ItemStack output;
    private final int consumeLiquid;
    private final int craftTime;
    private final String mode;
    private final String kind;

    public ContentRecipe(Ingredient input, ItemStack output,
                         int consumeLiquid, int craftTime, String mode, String kind) {
        this.input = input;
        this.output = output;
        this.consumeLiquid = consumeLiquid;
        this.craftTime = craftTime;
        this.mode = mode;
        this.kind = kind;
    }

    /** Install registered type/serializer suppliers from the loader DeferredRegister. */
    public static void install(Supplier<RecipeType<?>> processTypeSupplier,
                               Supplier<RecipeType<?>> modeTypeSupplier,
                               Supplier<RecipeSerializer<?>> processSerializerSupplier,
                               Supplier<RecipeSerializer<?>> modeSerializerSupplier) {
        processType = Objects.requireNonNull(processTypeSupplier);
        modeType = Objects.requireNonNull(modeTypeSupplier);
        processSerializer = Objects.requireNonNull(processSerializerSupplier);
        modeSerializer = Objects.requireNonNull(modeSerializerSupplier);
    }

    public static RecipeType<?> contentProcessType() {
        return processType.get();
    }

    public static RecipeType<?> contentModeType() {
        return modeType.get();
    }

    public static RecipeSerializer<?> contentProcessSerializer() {
        return processSerializer.get();
    }

    public static RecipeSerializer<?> contentModeSerializer() {
        return modeSerializer.get();
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output.copy();
    }

    public Ingredient getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output;
    }

    public int getConsumeLiquid() {
        return consumeLiquid;
    }

    public int getCraftTime() {
        return craftTime;
    }

    public String getMode() {
        return mode;
    }

    public String getKind() {
        return kind;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return "mode".equals(kind) ? modeSerializer.get() : processSerializer.get();
    }

    @Override
    public RecipeType<?> getType() {
        return "mode".equals(kind) ? modeType.get() : processType.get();
    }

    public static final class Serializer implements RecipeSerializer<ContentRecipe> {
        private final String kind;
        private final MapCodec<ContentRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ContentRecipe> streamCodec;

        public Serializer(String kind) {
            this.kind = kind;
            this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(ContentRecipe::getInput),
                ItemStack.STRICT_CODEC.fieldOf("output").forGetter(ContentRecipe::getOutput),
                Codec.INT.optionalFieldOf("consume_liquid", 0).forGetter(ContentRecipe::getConsumeLiquid),
                Codec.INT.optionalFieldOf("craft_time", 200).forGetter(ContentRecipe::getCraftTime),
                Codec.STRING.optionalFieldOf("mode", "").forGetter(ContentRecipe::getMode)
            ).apply(instance, (input, output, liquid, time, mode) ->
                new ContentRecipe(input, output, liquid, time, mode, kind)));
            this.streamCodec = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, ContentRecipe::getInput,
                ItemStack.STREAM_CODEC, ContentRecipe::getOutput,
                ByteBufCodecs.VAR_INT, ContentRecipe::getConsumeLiquid,
                ByteBufCodecs.VAR_INT, ContentRecipe::getCraftTime,
                ByteBufCodecs.STRING_UTF8, ContentRecipe::getMode,
                (input, output, liquid, time, mode) -> new ContentRecipe(input, output, liquid, time, mode, kind)
            );
        }

        @Override
        public MapCodec<ContentRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ContentRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
