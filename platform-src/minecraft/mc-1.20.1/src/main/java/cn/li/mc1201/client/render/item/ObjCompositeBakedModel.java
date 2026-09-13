package cn.li.mc1201.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * GUI-vs-world composite item model (upstream ItemDeveloper / BakedModelForTEISR).
 * <ul>
 *   <li>GUI — flat generated model (energy empty/half/full overrides)</li>
 *   <li>Hand / ground / item-frame — 3D world mesh</li>
 * </ul>
 *
 * {@link #getOverrides()} delegates to the GUI model by default. Energy-tier items pass an
 * override list that re-wraps whatever the predicate resolves to, because {@link ItemOverrides}
 * captures its targets at bake time and cannot be reached by editing the baking-result registry.
 * That subclass lives in the loader modules — Fabric keeps the empty ctor private.
 */
public class ObjCompositeBakedModel implements BakedModel {

    private final BakedModel guiModel;
    private final BakedModel worldModel;
    private final ItemOverrides overrides;

    public ObjCompositeBakedModel(BakedModel guiModel, BakedModel worldModel) {
        this(guiModel, worldModel, guiModel.getOverrides());
    }

    public ObjCompositeBakedModel(BakedModel guiModel, BakedModel worldModel, ItemOverrides overrides) {
        this.guiModel = guiModel;
        this.worldModel = worldModel;
        this.overrides = overrides;
    }

    /**
     * Public because Fabric has no {@code applyTransform}: its ItemRenderer mixin swaps the
     * rendered model itself.
     */
    public BakedModel selectModel(@Nullable ItemDisplayContext displayContext) {
        if (displayContext == ItemDisplayContext.GUI) {
            return guiModel;
        }
        return worldModel;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                              @NotNull RandomSource rand) {
        return guiModel.getQuads(state, side, rand);
    }

    /**
     * Same contract as Forge {@code IForgeBakedModel#applyTransform}: apply the
     * selected sub-model's item transform and return that model for rendering.
     */
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext transformType,
                                              @NotNull PoseStack poseStack,
                                              boolean applyLeftHandTransform) {
        BakedModel selected = selectModel(transformType);
        selected.getTransforms().getTransform(transformType).apply(applyLeftHandTransform, poseStack);
        return selected;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return worldModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return guiModel.getParticleIcon();
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return guiModel.getTransforms();
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return overrides;
    }
}
