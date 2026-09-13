package cn.li.mc262.client.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

/**
 * Minecraft 26.2 see-through translucent render type.
 *
 * <p>1.20.1 and 1.21.1 build the same capability out of {@code RenderType}
 * composite states; 26.2 replaced that with {@link RenderPipeline}, so the
 * equivalent has to be assembled here. No custom shader is involved — this is
 * vanilla's {@code core/entity} program with one state changed.</p>
 */
public final class ModRenderTypes {
    private static final Identifier PIPELINE_ID =
            Identifier.parse("academy:pipeline/entity_translucent_see_through");

    /**
     * {@code RenderPipelines.ENTITY_TRANSLUCENT} rebuilt with the depth state
     * legacy TESRs set by hand: {@code glDisable(GL_DEPTH_TEST)} plus
     * {@code glDepthMask(false)}, i.e. {@link CompareOp#ALWAYS_PASS} and no
     * depth write. Everything else is copied from vanilla so the only visible
     * change is depth behaviour.
     *
     * <p>Vanilla composes this pipeline from private snippets
     * (GLOBALS -> MATRICES_FOG -> MATRICES_FOG_LIGHT_DIR -> ENTITY), so the
     * chain is expanded inline below. <b>Bind group layouts are indexed by
     * declaration order</b> — the sequence here must stay byte-for-byte in step
     * with {@code RenderPipelines}' snippet chain, or the shader reads the
     * wrong bind groups.</p>
     */
    private static final RenderPipeline ENTITY_TRANSLUCENT_SEE_THROUGH = RenderPipeline.builder()
            .withLocation(PIPELINE_ID)
            // GLOBALS_SNIPPET
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            // MATRICES_FOG_SNIPPET
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            // MATRICES_FOG_LIGHT_DIR_SNIPPET
            .withBindGroupLayout(BindGroupLayouts.LIGHTING)
            // ENTITY_SNIPPET
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            // ENTITY_TRANSLUCENT itself
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withShaderDefine("PER_FACE_LIGHTING")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            // The one deliberate deviation from ENTITY_TRANSLUCENT, which
            // inherits DepthStencilState.DEFAULT (GEQUAL, write depth).
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();

    private static final Function<Identifier, RenderType> ACADEMY_QUADS_TRANSLUCENT_BY_TEXTURE =
            Util.memoize(texture -> RenderType.create(
                    "academy_quads_translucent",
                    RenderSetup.builder(ENTITY_TRANSLUCENT_SEE_THROUGH)
                            .withTexture("Sampler0", texture)
                            .useLightmap()
                            .useOverlay()
                            .sortOnUpload()
                            // An overlay effect must not pick up block-break
                            // crumbling or contribute to entity outlines.
                            .setOutline(RenderSetup.OutlineProperty.NONE)
                            .createRenderSetup()));

    private ModRenderTypes() {
    }

    /**
     * Exposed so loaders can precompile the pipeline during
     * RegisterRenderPipelinesEvent. Registration is a warm-up only — GlDevice
     * compiles on first use via {@code getOrCompilePipeline} — so a loader
     * without that event still renders correctly.
     */
    public static RenderPipeline academyQuadsTranslucentPipeline() {
        return ENTITY_TRANSLUCENT_SEE_THROUGH;
    }

    /**
     * Translucent QUADS with no depth test, no depth write and no cull. Uses
     * DefaultVertexFormat.ENTITY (unlike the 1.20.1/1.21.1 types, which are
     * POSITION_COLOR_TEX_LIGHTMAP), so vertices carry overlay and normal —
     * {@code RenderInterop.submitVertexNoOverlay} owns that difference.
     */
    public static RenderType academyQuadsTranslucent(Identifier texture) {
        return ACADEMY_QUADS_TRANSLUCENT_BY_TEXTURE.apply(texture);
    }
}
