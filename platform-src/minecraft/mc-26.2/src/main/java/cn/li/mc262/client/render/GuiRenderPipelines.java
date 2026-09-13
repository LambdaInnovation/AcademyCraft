package cn.li.mc262.client.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Extraction-safe GUI pipelines used by the reactive UI on Minecraft 26.2.
 *
 * <p>Gui extraction cannot mutate a live shader or GL depth state. Dynamic
 * scalar parameters are therefore carried in the per-quad vertex colour:
 * progress/scroll in red, highlight in green, and alpha in alpha. The depth
 * mask additionally carries its alpha threshold in red and selected skill-tree
 * layer in green.</p>
 */
public final class GuiRenderPipelines {
    private static final ColorTargetState TRANSLUCENT = new ColorTargetState(
            Optional.of(BlendFunction.TRANSLUCENT),
            GpuFormat.RGBA8_UNORM,
            ColorTargetState.WRITE_COLOR);

    private static final ColorTargetState DEPTH_ONLY = new ColorTargetState(
            Optional.empty(),
            GpuFormat.RGBA8_UNORM,
            ColorTargetState.WRITE_NONE);

    private static final Identifier GUI_VERTEX =
            Identifier.parse("academy:core/gui_textured_26");
    private static final Identifier PLATE_VERTEX =
            Identifier.parse("academy:core/gui_plate_depth_26");
    private static final Identifier RING_VERTEX =
            Identifier.parse("academy:core/gui_ring_depth_26");
    private static final Identifier RING_COLOR_VERTEX =
            Identifier.parse("academy:core/gui_ring_color_depth_26");
    private static final Identifier MASK_VERTEX =
            Identifier.parse("academy:core/gui_mask_depth_26");

    // Creation order == GUI sort-key order (RenderPipeline.nextPipelineSortKey):
    // the 26.2 GUI mesh sorts every element by (scissor, pipeline sort key,
    // texture setup), so the skill tree's depth-stamp-then-test chain only
    // survives if the depth stamps sort BEFORE the icons and lines: masks
    // (ALPHA_DISCARD) < icons (MONO / DEPTH_EQUAL_TEXTURED) < lines
    // (DEPTH_NOTEQUAL_COLOR). The sort is stable, so same-key runs keep tape
    // order and vanilla blits (smaller keys) still paint before the stamps.
    private static final RenderPipeline ALPHA_DISCARD = texturedPipeline(
            "alpha_discard", MASK_VERTEX,
            Identifier.parse("academy:core/alpha_discard_26"),
            false, new DepthStencilState(CompareOp.ALWAYS_PASS, true), DEPTH_ONLY);

    private static final RenderPipeline MONO = texturedPipeline(
            "mono", PLATE_VERTEX,
            Identifier.parse("academy:core/mono_26"),
            false, new DepthStencilState(CompareOp.EQUAL, false), TRANSLUCENT);

    private static final RenderPipeline DEPTH_EQUAL_TEXTURED = texturedPipeline(
            "depth_equal_textured", PLATE_VERTEX,
            Identifier.parse("academy:core/gui_textured_26"),
            false, new DepthStencilState(CompareOp.EQUAL, false), TRANSLUCENT);

    private static final RenderPipeline DEPTH_NOTEQUAL_COLOR = RenderPipeline.builder()
            .withLocation(Identifier.parse("academy:pipeline/gui_depth_notequal_color"))
            .withVertexShader(RING_COLOR_VERTEX)
            .withFragmentShader(Identifier.parse("academy:core/gui_color_26"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withColorTargetState(TRANSLUCENT)
            .withDepthStencilState(new DepthStencilState(CompareOp.NOT_EQUAL, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();

    private static final RenderPipeline SKILL_PROGBAR = texturedPipeline(
            "skill_progbar", GUI_VERTEX,
            Identifier.parse("academy:core/skill_progbar_26"),
            true, null, TRANSLUCENT);

    private static final RenderPipeline CPBAR_OVERLOAD = texturedPipeline(
            "cpbar_overload", GUI_VERTEX,
            Identifier.parse("academy:core/cpbar_overload_26"),
            true, null, TRANSLUCENT);

    // Dedicated line pipeline: created last so its sort key is larger than the
    // vanilla blit pipelines (backgrounds, plates) — the GUI mesh sorts by
    // pipeline sort key, and a line drawn through vanilla GUI_TEXTURED could
    // sort before the background that covers it.
    private static final RenderPipeline LINE_TEXTURED = texturedPipeline(
            "line_textured", GUI_VERTEX,
            Identifier.parse("academy:core/gui_textured_26"),
            false, null, TRANSLUCENT);

    private static final List<RenderPipeline> ALL = List.of(
            ALPHA_DISCARD,
            MONO,
            DEPTH_EQUAL_TEXTURED,
            DEPTH_NOTEQUAL_COLOR,
            SKILL_PROGBAR,
            CPBAR_OVERLOAD,
            LINE_TEXTURED);

    private GuiRenderPipelines() {
    }

    private static RenderPipeline texturedPipeline(
            String name,
            Identifier vertexShader,
            Identifier fragmentShader,
            boolean twoTextures,
            DepthStencilState depthStencilState,
            ColorTargetState colorTargetState) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Identifier.parse("academy:pipeline/gui_" + name))
                .withVertexShader(vertexShader)
                .withFragmentShader(fragmentShader)
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withBindGroupLayout(twoTextures
                        ? BindGroupLayouts.SAMPLER0_SAMPLER1
                        : BindGroupLayouts.SAMPLER0)
                .withColorTargetState(colorTargetState)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withCull(false);
        if (depthStencilState != null) {
            builder.withDepthStencilState(depthStencilState);
        }
        return builder.build();
    }

    public static List<RenderPipeline> all() {
        return ALL;
    }

    public static RenderPipeline skillProgbar() {
        return SKILL_PROGBAR;
    }

    public static RenderPipeline mono() {
        return MONO;
    }

    public static RenderPipeline cpbarOverload() {
        return CPBAR_OVERLOAD;
    }

    public static RenderPipeline alphaDiscard() {
        return ALPHA_DISCARD;
    }

    public static RenderPipeline depthEqualTextured() {
        return DEPTH_EQUAL_TEXTURED;
    }

    public static RenderPipeline depthNotEqualColor() {
        return DEPTH_NOTEQUAL_COLOR;
    }

    public static RenderPipeline lineTextured() {
        return LINE_TEXTURED;
    }
}
