package cn.academy.client.render.item;

import cn.lambdalib2.render.TransformUtils;
import cn.lambdalib2.util.Debug;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.List;

/**
 * A BakedModel that renders nothing, to facilitate rendering of {@link net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer}.
 * See: https://mcforge.readthedocs.io/en/latest/rendering/teisr/
 */
public class BakedModelForTEISR implements IBakedModel {
    private ResourceLocation _location;
    private Matrix4f[] _transformMapping = new Matrix4f[TransformType.values().length];
    private IBakedModel[] _modelMapping = new IBakedModel[TransformType.values().length];

    private Matrix4f _identityMatrix = new Matrix4f();

    public Runnable fiddleRuntime;

    public BakedModelForTEISR(ResourceLocation location) {
        _location = location;
        _identityMatrix.setIdentity();
    }

    public void mapModel(TransformType type, IBakedModel mdl) {
        _modelMapping[type.ordinal()] = mdl;
    }

    public void mapTransform(TransformType type, org.lwjgl.util.vector.Matrix4f mat) {
        mapTransform(type, TransformUtils.toJavax(mat));
    }

    public void mapTransform(TransformType type, Matrix4f mat) {
        _transformMapping[type.ordinal()] = mat;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return Collections.emptyList();
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType type) {
        if (fiddleRuntime != null)
            fiddleRuntime.run();

        final int ordinal = type.ordinal();
        Matrix4f mat = _identityMatrix;
        IBakedModel model = this;
        if (_transformMapping[ordinal] != null)
            mat = _transformMapping[ordinal];
        if (_modelMapping[ordinal] != null)
            model = _modelMapping[ordinal];
        return Pair.of(model, mat);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return ModelLoader.defaultTextureGetter().apply(_location);
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }
}
