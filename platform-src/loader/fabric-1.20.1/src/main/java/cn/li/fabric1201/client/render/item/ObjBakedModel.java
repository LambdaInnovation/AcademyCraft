package cn.li.fabric1201.client.render.item;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holds a pre-built OBJ mesh, reusing the display transforms and particle sprite of the vanilla
 * model the {@code _3d} JSON baked into.
 *
 * <p>Every quad is unculled: the mesh is a free-standing item, not a block face, and upstream's
 * TEISR drew all of its triangles.
 */
public class ObjBakedModel implements BakedModel {

    private final List<BakedQuad> quads;
    private final ItemTransforms transforms;
    private final TextureAtlasSprite particle;

    public ObjBakedModel(List<BakedQuad> quads, ItemTransforms transforms, TextureAtlasSprite particle) {
        this.quads = quads;
        this.transforms = transforms;
        this.particle = particle;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource random) {
        return side == null ? quads : List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
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
        return particle;
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
