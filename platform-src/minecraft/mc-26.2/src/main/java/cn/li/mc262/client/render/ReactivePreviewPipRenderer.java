package cn.li.mc262.client.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Perspective, depth-backed PIP renderer for reactive {@code :preview-3d}.
 */
public final class ReactivePreviewPipRenderer
        extends PictureInPictureRenderer<ReactivePreviewRenderState> {

    private static final float FOV_DEGREES = 50.0F;
    private static final float Z_NEAR = 0.1F;
    private static final float Z_FAR = 100.0F;
    private static final float CAMERA_DISTANCE = 3.2F;
    private static final float TARGET_MODEL_SIZE = 1.8F;

    private final Projection perspective = new Projection();
    private final ProjectionMatrixBuffer perspectiveBuffer =
            new ProjectionMatrixBuffer("reactive preview PIP projection");

    @Override
    public Class<ReactivePreviewRenderState> getRenderStateClass() {
        return ReactivePreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(
            ReactivePreviewRenderState state,
            PoseStack pose,
            SubmitNodeCollector submitNodes
    ) {
        int width = Math.max(1, state.x1() - state.x0());
        int height = Math.max(1, state.y1() - state.y0());
        // Projection uses reversed-Z argument order internally in 26.2.
        perspective.setupPerspective(Z_FAR, Z_NEAR, FOV_DEGREES, width, height);
        RenderSystem.setProjectionMatrix(
                perspectiveBuffer.getBuffer(perspective),
                ProjectionType.PERSPECTIVE);

        if (state.blockRenderState() != null) {
            // Real block state (tutorial :block-3d): the model is a unit cube
            // in block space, so bounds are implicit.
            AABB bounds = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
            Vec3 center = bounds.getCenter();
            pose.setIdentity();
            pose.translate(0.0F, -state.yOffset(), -CAMERA_DISTANCE);
            pose.mulPose(new Quaternionf().rotateAxis(
                    (float) Math.toRadians(-20.0),
                    1.0F,
                    0.0F,
                    0.1F));
            pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(state.yawDegrees())));
            float modelScale = (float) TARGET_MODEL_SIZE
                    * Math.max(0.001F, state.modelScale());
            pose.scale(modelScale, modelScale, modelScale);
            pose.translate(-center.x, -center.y, -center.z);
            Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
            state.blockRenderState().submit(
                    pose,
                    submitNodes,
                    15728880,
                    OverlayTexture.NO_OVERLAY,
                    0);
            return;
        }

        AABB bounds = state.itemRenderState().getModelBoundingBox();
        Vec3 center = bounds.getCenter();
        double largestExtent = Math.max(
                bounds.getXsize(),
                Math.max(bounds.getYsize(), bounds.getZsize()));
        float fitScale = (float) (TARGET_MODEL_SIZE / Math.max(0.001, largestExtent));

        // Discard PictureInPictureRenderer's orthographic pixel transform and
        // build a conventional eye-space modelview for this node's target.
        pose.setIdentity();
        pose.translate(0.0F, -state.yOffset(), -CAMERA_DISTANCE);
        pose.mulPose(new Quaternionf().rotateAxis(
                (float) Math.toRadians(-20.0),
                1.0F,
                0.0F,
                0.1F));
        pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(state.yawDegrees())));
        float modelScale = fitScale * Math.max(0.001F, state.modelScale());
        pose.scale(modelScale, modelScale, modelScale);
        pose.translate(-center.x, -center.y, -center.z);

        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
        state.itemRenderState().submit(
                pose,
                submitNodes,
                15728880,
                OverlayTexture.NO_OVERLAY,
                0);
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "reactive_preview_3d";
    }

    @Override
    public void close() {
        perspectiveBuffer.close();
        super.close();
    }
}
