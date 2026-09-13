package cn.li.mc262.client.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.GpuFormat;
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

import java.util.Optional;

/**
 * Minecraft 26.2 render state for plasma level geometry.
 *
 * <p>The 26.2 collector only prepares the standard global and transform bind
 * groups for a {@link RenderType}; it has no public equivalent of the old
 * per-draw {@code ShaderInstance#getUniform}. Plasma parameters are therefore
 * carried by ENTITY vertices: UV integer bands identify the depth sample
 * and two quantized neighbour radii, colour RGB stores the nearest-neighbour
 * offset, and the otherwise-unused normal stores a second offset. The fragment
 * shader adds all three metaball terms on six depth samples, so small clusters
 * visibly fuse.</p>
 *
 * <p>This remains deliberately below the 1.21.1 path: it sees two neighbours
 * per ball with quantized radii and six discrete samples, rather than all 16
 * balls in a 20-step ray march. Keeping that limitation explicit avoids
 * implying that SubmitNodeCollector supplies a custom per-draw UBO.</p>
 */
public final class PlasmaRenderTypes {
    private static final Identifier PIPELINE_ID =
            Identifier.parse("academy:pipeline/plasma_body_26");
    private static final Identifier SHADER_ID =
            Identifier.parse("academy:core/plasma_body_26");

    private static final RenderPipeline PLASMA_BODY_PIPELINE = RenderPipeline.builder()
            .withLocation(PIPELINE_ID)
            .withVertexShader(SHADER_ID)
            .withFragmentShader(SHADER_ID)
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withColorTargetState(new ColorTargetState(
                    Optional.of(BlendFunction.TRANSLUCENT),
                    GpuFormat.RGBA8_UNORM,
                    ColorTargetState.WRITE_COLOR))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();

    private static final RenderType PLASMA_BODY = RenderType.create(
            "academy_plasma_body_26",
            RenderSetup.builder(PLASMA_BODY_PIPELINE)
                    .sortOnUpload()
                    .createRenderSetup());

    private PlasmaRenderTypes() {
    }

    public static RenderPipeline plasmaBodyPipeline() {
        return PLASMA_BODY_PIPELINE;
    }

    public static RenderType plasmaBody() {
        return PLASMA_BODY;
    }
}
