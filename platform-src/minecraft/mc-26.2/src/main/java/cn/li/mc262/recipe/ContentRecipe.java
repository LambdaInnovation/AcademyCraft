package cn.li.mc262.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Content machine recipe (process / mode). 26.2: RecipeSerializer is a record;
 * assemble no longer takes HolderLookup; PlacementInfo / RecipeBookCategory
 * required. The output is an {@link ItemStackTemplate} like every vanilla 26.2
 * recipe: {@link ItemStack#CODEC} validates that the referenced item's holder
 * has components bound, which only happens after the datapack reload completes
 * (ReloadableServerResources.updateComponentsAndStaticRegistryTags), so a
 * strict ItemStack codec makes every recipe file fail to parse at load time.
 */
public final class ContentRecipe implements Recipe<SingleRecipeInput> {
    private static volatile Supplier<RecipeType<?>> processType = () -> null;
    private static volatile Supplier<RecipeType<?>> modeType = () -> null;
    private static volatile Supplier<RecipeSerializer<?>> processSerializer = () -> null;
    private static volatile Supplier<RecipeSerializer<?>> modeSerializer = () -> null;

    private final Ingredient input;
    private final ItemStackTemplate output;
    private final int consumeLiquid;
    private final int craftTime;
    private final String mode;
    private final String kind;

    public ContentRecipe(Ingredient input, ItemStackTemplate output,
                         int consumeLiquid, int craftTime, String mode, String kind) {
        this.input = input;
        this.output = output;
        this.consumeLiquid = consumeLiquid;
        this.craftTime = craftTime;
        this.mode = mode == null ? "" : mode;
        this.kind = kind;
    }

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

    /** Build a 26.2 record-shaped serializer for the given kind. */
    public static RecipeSerializer<ContentRecipe> createSerializer(String kind) {
        MapCodec<ContentRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("input").forGetter(ContentRecipe::getInput),
                ItemStackTemplate.CODEC.fieldOf("output").forGetter(ContentRecipe::getOutput),
                Codec.INT.optionalFieldOf("consume_liquid", 0).forGetter(ContentRecipe::getConsumeLiquid),
                Codec.INT.optionalFieldOf("craft_time", 200).forGetter(ContentRecipe::getCraftTime),
                Codec.STRING.optionalFieldOf("mode", "").forGetter(ContentRecipe::getMode)
        ).apply(instance, (in, out, liquid, time, mode) ->
                new ContentRecipe(in, out, liquid, time, mode, kind)));

        StreamCodec<RegistryFriendlyByteBuf, ContentRecipe> streamCodec = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, ContentRecipe::getInput,
                ItemStackTemplate.STREAM_CODEC, ContentRecipe::getOutput,
                ByteBufCodecs.VAR_INT, ContentRecipe::getConsumeLiquid,
                ByteBufCodecs.VAR_INT, ContentRecipe::getCraftTime,
                ByteBufCodecs.STRING_UTF8, ContentRecipe::getMode,
                (in, out, liquid, time, mode) -> new ContentRecipe(in, out, liquid, time, mode, kind)
        );
        return new RecipeSerializer<>(codec, streamCodec);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.input.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return output.create();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return (RecipeSerializer) ("mode".equals(kind) ? modeSerializer.get() : processSerializer.get());
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return (RecipeType) ("mode".equals(kind) ? modeType.get() : processType.get());
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(input);
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    public Ingredient getInput() {
        return input;
    }

    public ItemStackTemplate getOutput() {
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
}
