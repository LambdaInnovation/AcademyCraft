package cn.li.mc262.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapts the deferred 26.2 submit-node pipeline to the buffer-shaped contract
 * used by the platform-neutral scripted TESRs.
 *
 * <p>Each requested render type becomes a custom-geometry submit node. Vertex
 * data is recorded while the Clojure renderer runs and replayed when Minecraft
 * executes that node. Coordinates and normals are stored relative to the pose
 * captured for that node, so renderers may continue to mutate their PoseStack
 * after requesting a buffer.</p>
 */
public final class SubmitNodeRenderBufferAdapter {
    private final SubmitNodeCollector collector;
    private final PoseStack poseStack;
    private final List<DeferredVertexConsumer> consumers = new ArrayList<>();
    private boolean finished;

    public SubmitNodeRenderBufferAdapter(SubmitNodeCollector collector, PoseStack poseStack) {
        this.collector = collector;
        this.poseStack = poseStack;
    }

    public VertexConsumer getBuffer(RenderType renderType) {
        if (finished) {
            throw new IllegalStateException("Scripted TESR requested a render buffer after submission finished");
        }
        PoseStack.Pose basePose = poseStack.last().copy();
        DeferredVertexConsumer deferred = new DeferredVertexConsumer(basePose);
        consumers.add(deferred);
        collector.submitCustomGeometry(poseStack, renderType, deferred::replay);
        return deferred;
    }

    public void finish() {
        if (finished) {
            return;
        }
        finished = true;
        for (DeferredVertexConsumer consumer : consumers) {
            consumer.finish();
        }
    }

    public static SubmitNodeRenderBufferAdapter require(Object source) {
        if (source instanceof SubmitNodeRenderBufferAdapter adapter) {
            return adapter;
        }
        throw new IllegalArgumentException(
                "Minecraft 26.2 scripted TESR requires SubmitNodeRenderBufferAdapter, got "
                        + (source == null ? "nil" : source.getClass().getName()));
    }

    public static final class DeferredVertexConsumer implements VertexConsumer {
        private final Matrix4f inverseBasePose;
        private final Matrix3f inverseBaseNormal;
        private final List<Vertex> vertices = new ArrayList<>();
        private Vertex current;
        private boolean finished;

        private DeferredVertexConsumer(PoseStack.Pose basePose) {
            inverseBasePose = new Matrix4f(basePose.pose()).invert();
            inverseBaseNormal = new Matrix3f(basePose.normal()).invert();
        }

        public void submitVertex(
                PoseStack poseStack,
                float x, float y, float z,
                float r, float g, float b, float a,
                float u, float v,
                int overlay, int light,
                float nx, float ny, float nz) {
            ensureOpen();
            PoseStack.Pose currentPose = poseStack.last();
            Matrix4f relativePose = new Matrix4f(inverseBasePose).mul(currentPose.pose());
            Matrix3f relativeNormal = new Matrix3f(inverseBaseNormal).mul(currentPose.normal());
            Vector3f position = relativePose.transformPosition(x, y, z, new Vector3f());
            Vector3f normal = relativeNormal.transform(nx, ny, nz, new Vector3f()).normalize();

            addVertex(position.x(), position.y(), position.z())
                    .setColor(r, g, b, a)
                    .setUv(u, v)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(normal.x(), normal.y(), normal.z());
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            ensureOpen();
            current = new Vertex(x, y, z);
            vertices.add(current);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            requireCurrent().setRgba(r, g, b, a);
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            requireCurrent().setPackedColor(color);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            Vertex vertex = requireCurrent();
            vertex.u = u;
            vertex.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            Vertex vertex = requireCurrent();
            vertex.overlayU = u;
            vertex.overlayV = v;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            Vertex vertex = requireCurrent();
            vertex.lightU = u;
            vertex.lightV = v;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            Vertex vertex = requireCurrent();
            vertex.nx = x;
            vertex.ny = y;
            vertex.nz = z;
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            requireCurrent().lineWidth = width;
            return this;
        }

        private void finish() {
            finished = true;
            current = null;
        }

        private void replay(PoseStack.Pose pose, VertexConsumer target) {
            if (!finished) {
                throw new IllegalStateException("Scripted TESR geometry replayed before recording finished");
            }
            for (Vertex vertex : vertices) {
                VertexConsumer out = target.addVertex(pose, vertex.x, vertex.y, vertex.z);
                if (vertex.packedColor != null) {
                    out.setColor(vertex.packedColor);
                } else {
                    out.setColor(vertex.r, vertex.g, vertex.b, vertex.a);
                }
                out.setUv(vertex.u, vertex.v)
                        .setUv1(vertex.overlayU, vertex.overlayV)
                        .setUv2(vertex.lightU, vertex.lightV)
                        .setNormal(pose, vertex.nx, vertex.ny, vertex.nz)
                        .setLineWidth(vertex.lineWidth);
            }
        }

        private Vertex requireCurrent() {
            ensureOpen();
            if (current == null) {
                throw new IllegalStateException("Vertex attribute submitted before addVertex");
            }
            return current;
        }

        private void ensureOpen() {
            if (finished) {
                throw new IllegalStateException("Vertex submitted after scripted TESR recording finished");
            }
        }
    }

    private static final class Vertex {
        private final float x;
        private final float y;
        private final float z;
        private int r = 255;
        private int g = 255;
        private int b = 255;
        private int a = 255;
        private Integer packedColor;
        private float u;
        private float v;
        private int overlayU;
        private int overlayV;
        private int lightU;
        private int lightV;
        private float nx;
        private float ny = 1.0F;
        private float nz;
        private float lineWidth = 1.0F;

        private Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void setRgba(int r, int g, int b, int a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.packedColor = null;
        }

        private void setPackedColor(int packedColor) {
            this.packedColor = packedColor;
        }
    }
}
