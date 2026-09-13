package cn.li.mc1201.client.font.msdf;

import cn.li.mcbase.client.font.msdf.MsdfTextFx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public final class MsdfRenderTypes extends RenderType {

    public static final VertexFormat MSDF_TEXT_FORMAT = DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;

    private static volatile ShaderInstance msdfShader;

    private static final ShaderStateShard MSDF_SHADER_STATE =
            new ShaderStateShard(MsdfRenderTypes::bindMsdfShader);

    private static final Function<ResourceLocation, RenderType> MSDF_TEXT_NORMAL =
            Util.memoize(texture -> createMsdfText(texture, "normal", NO_DEPTH_TEST, NO_LAYERING));

    private static final Function<ResourceLocation, RenderType> MSDF_TEXT_SEE_THROUGH =
            Util.memoize(texture -> createMsdfText(texture, "see_through", GREATER_DEPTH_TEST, NO_LAYERING));

    private static final Function<ResourceLocation, RenderType> MSDF_TEXT_POLYGON_OFFSET =
            Util.memoize(texture -> createMsdfText(texture, "polygon_offset", LEQUAL_DEPTH_TEST, POLYGON_OFFSET_LAYERING));

    private MsdfRenderTypes(final String name, final VertexFormat format, final VertexFormat.Mode mode,
                            final int bufferSize, final boolean affectsCrumbling, final boolean sortOnUpload,
                            final Runnable setupState, final Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        throw new IllegalStateException("MsdfRenderTypes is utility-only");
    }

    private static RenderType createMsdfText(
            final ResourceLocation texture,
            final String suffix,
            final DepthTestStateShard depthTest,
            final LayeringStateShard layering) {
        return create(
                "academy_msdf_text/" + suffix + "/" + texture,
                MSDF_TEXT_FORMAT,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                CompositeState.builder()
                        .setShaderState(MSDF_SHADER_STATE)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setLightmapState(NO_LIGHTMAP)
                        .setDepthTestState(depthTest)
                        .setLayeringState(layering)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .setTextureState(new TextureStateShard(texture, true, false))
                        .createCompositeState(false)
        );
    }

    public static void setMsdfShader(final ShaderInstance shader) {
        msdfShader = shader;
    }

    public static ShaderInstance getMsdfShader() {
        return msdfShader;
    }

    public static RenderType msdfText(final ResourceLocation texture) {
        return MSDF_TEXT_NORMAL.apply(texture);
    }

    public static GlyphRenderTypes glyphRenderTypes(final ResourceLocation texture) {
        return new GlyphRenderTypes(
                MSDF_TEXT_NORMAL.apply(texture),
                MSDF_TEXT_SEE_THROUGH.apply(texture),
                MSDF_TEXT_POLYGON_OFFSET.apply(texture));
    }

    private static ShaderInstance bindMsdfShader() {
        final ShaderInstance shader = msdfShader;
        if (shader != null) {
            applyUniforms(shader);
        }
        return shader;
    }

    private static void applyUniforms(final ShaderInstance shader) {
        var uThickness = shader.getUniform("u_ThicknessOffset");
        if (uThickness != null) {
            uThickness.set(MsdfTextFx.getThicknessOffset());
        }
        var uBoldThickness = shader.getUniform("u_BoldThickness");
        if (uBoldThickness != null) {
            uBoldThickness.set(MsdfTextFx.BOLD_THICKNESS);
        }
        var uOutlineColor = shader.getUniform("u_OutlineColor");
        if (uOutlineColor != null) {
            uOutlineColor.set(
                    MsdfTextFx.getOutlineR(),
                    MsdfTextFx.getOutlineG(),
                    MsdfTextFx.getOutlineB(),
                    MsdfTextFx.getOutlineA());
        }
        var uOutlineWidth = shader.getUniform("u_OutlineWidth");
        if (uOutlineWidth != null) {
            uOutlineWidth.set(MsdfTextFx.getOutlineWidth());
        }
        var uGlowColor = shader.getUniform("u_GlowColor");
        if (uGlowColor != null) {
            uGlowColor.set(
                    MsdfTextFx.getGlowR(),
                    MsdfTextFx.getGlowG(),
                    MsdfTextFx.getGlowB(),
                    MsdfTextFx.getGlowA());
        }
        var uGlowRadius = shader.getUniform("u_GlowRadius");
        if (uGlowRadius != null) {
            uGlowRadius.set(MsdfTextFx.getGlowRadius());
        }
        var uShadowOffset = shader.getUniform("u_ShadowOffset");
        if (uShadowOffset != null) {
            uShadowOffset.set(MsdfTextFx.getShadowOffsetX(), MsdfTextFx.getShadowOffsetY());
        }
    }
}
