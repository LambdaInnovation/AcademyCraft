package cn.li.mcver.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/**
 * Version seam for immediate-mode mesh upload on 1.21.1.
 * Uses {@link Tesselator#begin}, {@link BufferBuilder#addVertex},
 * {@link BufferBuilder#buildOrThrow} → {@link MeshData}, then
 * {@link BufferUploader#drawWithShader}.
 *
 * <p>Does not own shader/texture/blend/depth state — callers set those first.
 * {@link Vertex#endVertex()} is a no-op (1.21 ends vertices automatically).
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

    private static BufferBuilder active;

    public static void begin(Mode mode, Format format) {
        if (active != null) {
            throw new IllegalStateException("ImmediateDraw.begin called while a mesh is already open");
        }
        VertexFormat.Mode glMode = mode == Mode.QUADS ? VertexFormat.Mode.QUADS : VertexFormat.Mode.QUADS;
        VertexFormat vf = format == Format.POSITION_COLOR
            ? DefaultVertexFormat.POSITION_COLOR
            : DefaultVertexFormat.POSITION_TEX;
        active = Tesselator.getInstance().begin(glMode, vf);
    }

    public static Vertex vertex(Matrix4f pose, float x, float y, float z) {
        requireActive();
        return new Vertex(active.addVertex(pose, x, y, z));
    }

    public static Vertex vertex(float x, float y, float z) {
        requireActive();
        return new Vertex(active.addVertex(x, y, z));
    }

    public static void draw() {
        requireActive();
        BufferBuilder bb = active;
        active = null;
        MeshData mesh = bb.buildOrThrow();
        BufferUploader.drawWithShader(mesh);
    }

    /**
     * One POSITION_TEX axis-aligned quad. Caller must already set shader + texture.
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

    private static void requireActive() {
        if (active == null) {
            throw new IllegalStateException("ImmediateDraw has no open mesh; call begin() first");
        }
    }

    public static final class Vertex {
        private final com.mojang.blaze3d.vertex.VertexConsumer consumer;

        private Vertex(com.mojang.blaze3d.vertex.VertexConsumer consumer) {
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

        /** No-op on 1.21 — vertices end automatically after attribute writes. */
        public void endVertex() {
        }
    }
}
