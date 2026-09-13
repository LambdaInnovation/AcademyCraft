package cn.li.mc1211.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public final class ModRenderTypes extends RenderType {
    public static final VertexFormat PLASMA_BODY_FORMAT = DefaultVertexFormat.POSITION;

    private static volatile ShaderInstance plasmaBodyShader;
    private static volatile ShaderInstance skillProgbarShader;
    private static volatile ShaderInstance monoShader;
    private static volatile ShaderInstance cpbarOverloadShader;
    private static volatile ShaderInstance alphaDiscardShader;
    private static volatile ShaderInstance noFogShader;

    public static void setNoFogShader(ShaderInstance shader) {
        noFogShader = shader;
    }

    private static final ShaderStateShard PLASMA_BODY_SHADER_STATE =
            new ShaderStateShard(ModRenderTypes::getPlasmaBodyShader);

    private static final CompositeState PLASMA_BODY_STATE = CompositeState.builder()
            .setShaderState(PLASMA_BODY_SHADER_STATE)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false);

    private static final Function<String, RenderType> PLASMA_BODY_BY_KEY =
            Util.memoize(key -> create(
                    key,
                    PLASMA_BODY_FORMAT,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    false,
                    PLASMA_BODY_STATE
            ));

    /**
     * Translucent textured-QUADS render type for the tp_mark humanoid — the
     * vanilla textSeeThrough pattern (RENDERTYPE_TEXT_SHADER samples the
     * texture times vertex colour, no lightmap modulation — the same
     * "texture true colours" semantics as upstream's ShaderSimple) with
     * depth test disabled and cull disabled. Upstream MarkRender draws the
     * mark with GL11.glDisable(GL_DEPTH_TEST) + glDisable(GL_CULL_FACE), so
     * the humanoid stays visible through walls when the destination is still
     * inside one (penetrate_teleport's unavailable case) and its back faces
     * never cull away. COLOR_WRITE skips depth writes like the vanilla
     * see-through types.
     */
    private static final Function<ResourceLocation, RenderType> ACADEMY_QUADS_TRANSLUCENT_BY_TEXTURE =
            Util.memoize(texture -> create(
                    "academy_quads_translucent",
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_TEXT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));

    public static RenderType academyQuadsTranslucent(ResourceLocation texture) {
        return ACADEMY_QUADS_TRANSLUCENT_BY_TEXTURE.apply(texture);
    }

    /**
     * Additive textured-QUADS render type for ray glow boards. The glow is
     * light, not smoke: it ADDS to whatever is behind it (SRC_ALPHA / ONE —
     * vanilla's lightning blend) instead of alpha-blending over it. Depth is
     * tested but never written: a halo must not occlude anything, and the
     * tubes drawn after it always land on top.
     */
    private static final Function<ResourceLocation, RenderType> ACADEMY_QUADS_ADDITIVE_BY_TEXTURE =
            Util.memoize(texture -> create(
                    "academy_quads_additive",
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    false,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_TEXT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setDepthTestState(LEQUAL_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));

    public static RenderType academyQuadsAdditive(ResourceLocation texture) {
        return ACADEMY_QUADS_ADDITIVE_BY_TEXTURE.apply(texture);
    }

    /**
     * Fog-free translucent textured-QUADS render type for the MineDetect ore
     * highlights. Upstream HandlerRender disables GL_FOG for the whole
     * mineview pass, so the boxes stay visible through the blindness fog the
     * skill itself applies; every vanilla shader includes fog, hence the
     * custom rendertype_academy_no_fog program. Same state as
     * academyQuadsTranslucent otherwise (no depth test, no cull, colour
     * write only).
     */
    private static final Function<ResourceLocation, RenderType> ACADEMY_QUADS_NO_FOG_BY_TEXTURE =
            Util.memoize(texture -> create(
                    "academy_quads_no_fog",
                    DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    true,
                    CompositeState.builder()
                            .setShaderState(new ShaderStateShard(() -> noFogShader))
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));

    public static RenderType academyQuadsNoFog(ResourceLocation texture) {
        return ACADEMY_QUADS_NO_FOG_BY_TEXTURE.apply(texture);
    }

    private ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                           boolean affectsCrumbling, boolean sortOnUpload,
                           Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        throw new IllegalStateException("ModRenderTypes is utility-only");
    }

    public static void setPlasmaBodyShader(ShaderInstance shader) {
        plasmaBodyShader = shader;
    }

    public static ShaderInstance getPlasmaBodyShader() {
        return plasmaBodyShader;
    }

    public static void setSkillProgbarShader(ShaderInstance shader) {
        skillProgbarShader = shader;
    }

    public static ShaderInstance getSkillProgbarShader() {
        return skillProgbarShader;
    }

    public static void setMonoShader(ShaderInstance shader) {
        monoShader = shader;
    }

    public static ShaderInstance getMonoShader() {
        return monoShader;
    }

    public static void setCpbarOverloadShader(ShaderInstance shader) {
        cpbarOverloadShader = shader;
    }

    public static ShaderInstance getCpbarOverloadShader() {
        return cpbarOverloadShader;
    }

    public static void setAlphaDiscardShader(ShaderInstance shader) {
        alphaDiscardShader = shader;
    }

    public static ShaderInstance getAlphaDiscardShader() {
        return alphaDiscardShader;
    }

    public static RenderType plasmaBody() {
        return PLASMA_BODY_BY_KEY.apply("academy_plasma_body");
    }
}