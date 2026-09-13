package cn.li.forge1201.client.render.item;

import cn.li.mc1201.client.render.item.ObjCompositeBakedModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps predicate-selected item models (energy empty/half/full) on the 3D world mesh.
 * <p>
 * {@link ItemOverrides} captures its target {@link BakedModel}s while baking, so replacing
 * the tier entries in the baking-result registry afterwards cannot reach them —
 * {@code ItemRenderer#getModel} would hand the raw flat tier model to the renderer and the
 * charged developer would go back to a flat icon in hand. Resolving through the flat base
 * and re-wrapping the result fixes that without depending on which instance the registry holds.
 * <p>
 * Lives in the loader module because Forge widens the no-arg {@code ItemOverrides} constructor
 * to {@code protected}; on Fabric — which shares {@code mc-1.20.1} — it is still private.
 */
public class ObjCompositeOverrides extends ItemOverrides {

    private final BakedModel flatBase;
    private final BakedModel worldModel;
    /** BakedModel has no equals/hashCode, so this keys on identity. */
    private final Map<BakedModel, BakedModel> composites = new ConcurrentHashMap<>();

    public ObjCompositeOverrides(BakedModel flatBase, BakedModel worldModel) {
        this.flatBase = flatBase;
        this.worldModel = worldModel;
    }

    @Override
    public BakedModel resolve(@NotNull BakedModel model, @NotNull ItemStack stack, @Nullable ClientLevel level,
                              @Nullable LivingEntity entity, int seed) {
        BakedModel flat = flatBase.getOverrides().resolve(flatBase, stack, level, entity, seed);
        if (flat == null || flat == flatBase) {
            // No predicate matched: `model` is already the base composite.
            return model;
        }
        return composites.computeIfAbsent(flat, tier -> new ObjCompositeBakedModel(tier, worldModel));
    }
}
