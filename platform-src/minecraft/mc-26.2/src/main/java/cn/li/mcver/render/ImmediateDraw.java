package cn.li.mcver.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * Consumer-bound version seam for immediate-style vertex emission.
 *
 * <p>Minecraft 26.2 owns upload, topology, and graphics state through a
 * RenderType submitted to a SubmitNodeCollector. Callers bind the consumer
 * supplied by that collector, then use the legacy begin/vertex fluent API.
 * Vertices are written immediately; {@link #draw()} only closes the logical
 * batch and intentionally performs no upload.
 */
public final class ImmediateDraw {
    private ImmediateDraw() {
    }

    public enum Mode {
        QUADS
    }

    public enum Format {
        POSITION_TEX,
        POSITION_COLOR
    }

    private static final ThreadLocal<Binding> BINDING = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> OPEN = ThreadLocal.withInitial(() -> false);

    public static void bind(VertexConsumer consumer, PoseStack.Pose pose) {
        if (consumer == null || pose == null) {
            throw new NullPointerException("ImmediateDraw binding requires a consumer and pose");
        }
        if (BINDING.get() != null) {
            throw new IllegalStateException("ImmediateDraw is already bound on this thread");
        }
        BINDING.set(new Binding(consumer, pose));
    }

    public static void bind(VertexConsumer consumer, PoseStack poseStack) {
        if (poseStack == null) {
            throw new NullPointerException("ImmediateDraw binding requires a pose stack");
        }
        bind(consumer, poseStack.last());
    }

    public static void unbind() {
        BINDING.remove();
        OPEN.remove();
    }

    public static void begin(Mode mode, Format format) {
        requireBinding();
        if (mode == null || format == null) {
            throw new NullPointerException("ImmediateDraw.begin requires mode and format");
        }
        if (Boolean.TRUE.equals(OPEN.get())) {
            throw new IllegalStateException("ImmediateDraw.begin called while a batch is already open");
        }
        OPEN.set(true);
    }

    public static Vertex vertex(Matrix4f pose, float x, float y, float z) {
        Binding binding = requireOpen();
        return new Vertex(binding.consumer.addVertex(pose, x, y, z));
    }

    public static Vertex vertex(float x, float y, float z) {
        Binding binding = requireOpen();
        return new Vertex(binding.consumer.addVertex(binding.pose, x, y, z));
    }

    public static void draw() {
        requireOpen();
        OPEN.set(false);
    }

    /**
     * One POSITION_TEX axis-aligned quad emitted into the bound consumer.
     * The collector-selected RenderType owns shader, texture, blend, and depth state.
     */
    public static void texturedQuad(
            Matrix4f pose,
            float x1, float y1, float x2, float y2, float z,
            float u0, float u1, float v0, float v1) {
        begin(Mode.QUADS, Format.POSITION_TEX);
        vertex(pose, x1, y1, z).uv(u0, v1).endVertex();
        vertex(pose, x2, y1, z).uv(u1, v1).endVertex();
        vertex(pose, x2, y2, z).uv(u1, v0).endVertex();
        vertex(pose, x1, y2, z).uv(u0, v0).endVertex();
        draw();
    }

    private static Binding requireBinding() {
        Binding binding = BINDING.get();
        if (binding == null) {
            throw new IllegalStateException(
                    "ImmediateDraw has no bound VertexConsumer; call bind() in custom geometry");
        }
        return binding;
    }

    private static Binding requireOpen() {
        Binding binding = requireBinding();
        if (!Boolean.TRUE.equals(OPEN.get())) {
            throw new IllegalStateException("ImmediateDraw has no open batch; call begin() first");
        }
        return binding;
    }

    private record Binding(VertexConsumer consumer, PoseStack.Pose pose) {
    }

    public static final class Vertex {
        private final VertexConsumer consumer;

        private Vertex(VertexConsumer consumer) {
            this.consumer = consumer;
        }

        public Vertex uv(float u, float v) {
            consumer.setUv(u, v);
            return this;
        }

        public Vertex color(float r, float g, float b, float a) {
            consumer.setColor(r, g, b, a);
            return this;
        }

        public Vertex color(int argb) {
            consumer.setColor(argb);
            return this;
        }

        public void endVertex() {
            // VertexConsumer finalizes vertices as attributes for the next
            // vertex are written; retained for seam compatibility.
        }
    }
}
