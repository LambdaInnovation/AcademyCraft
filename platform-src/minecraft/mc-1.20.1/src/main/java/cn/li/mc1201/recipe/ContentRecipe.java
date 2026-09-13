package cn.li.mc1201.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.function.Supplier;

/** Loader-agnostic content machine recipe (process / mode variants). */
public final class ContentRecipe implements Recipe<SimpleContainer> {
    private static volatile Supplier<RecipeType<?>> processType = () -> null;
    private static volatile Supplier<RecipeType<?>> modeType = () -> null;
    private static volatile Supplier<RecipeSerializer<?>> processSerializer = () -> null;
    private static volatile Supplier<RecipeSerializer<?>> modeSerializer = () -> null;

    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack output;
    private final int consumeLiquid;
    private final int craftTime;
    private final String mode;
    private final String kind;

    public ContentRecipe(ResourceLocation id, Ingredient input, ItemStack output,
                         int consumeLiquid, int craftTime, String mode, String kind) {
        this.id = id;
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
    public boolean matches(SimpleContainer container, Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess access) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
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

    @Override
    public ResourceLocation getId() {
        return id;
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

        public Serializer(String kind) {
            this.kind = kind;
        }

        @Override
        public ContentRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "input"));
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "output"));
            return new ContentRecipe(
                id,
                input,
                output,
                GsonHelper.getAsInt(json, "consume_liquid", 0),
                GsonHelper.getAsInt(json, "craft_time", 200),
                GsonHelper.getAsString(json, "mode", ""),
                kind);
        }

        @Override
        public ContentRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            ItemStack output = buf.readItem();
            int liquid = buf.readVarInt();
            int time = buf.readVarInt();
            String mode = "mode".equals(kind) ? buf.readUtf() : "";
            return new ContentRecipe(id, input, output, liquid, time, mode, kind);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ContentRecipe recipe) {
            recipe.input.toNetwork(buf);
            buf.writeItem(recipe.output);
            buf.writeVarInt(recipe.consumeLiquid);
            buf.writeVarInt(recipe.craftTime);
            if ("mode".equals(kind)) {
                buf.writeUtf(recipe.mode);
            }
        }
    }
}
