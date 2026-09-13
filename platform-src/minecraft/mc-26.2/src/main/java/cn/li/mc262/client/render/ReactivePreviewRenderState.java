package cn.li.mc262.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;

/**
 * Extracted GUI state for a reactive 3D preview.
 *
 * <p>The state contains only render-ready geometry and scalar camera
 * parameters. The GUI extraction pass submits it; the PIP renderer owns the
 * offscreen color/depth targets and the perspective camera.</p>
 *
 * <p>Block views resolve the real {@link BlockState} model via
 * {@link BlockModelResolver} (matching the 1.20.1/1.21.1 previews, which
 * rendered the block state directly); the block item's own flat inventory
 * model is never used, so the preview shows the actual cube.</p>
 *
 * <p>Submitting the state uses loader-specific GUI APIs: NeoForge patches
 * {@code GuiGraphicsExtractor.submitPictureInPictureRenderState} and
 * {@code peekScissorStack}, which the NeoForge loader installs via
 * {@link #installSubmitter}; loaders without those APIs (Fabric) never
 * install one and {@link #submit} falls back to false, leaving the caller's
 * compatibility path active.</p>
 */
public record ReactivePreviewRenderState(
        TrackingItemStackRenderState itemRenderState,
        BlockModelRenderState blockRenderState,
        int x0,
        int y0,
        int x1,
        int y1,
        float modelScale,
        float yawDegrees,
        float yOffset,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
) implements PictureInPictureRenderState {

    @FunctionalInterface
    public interface SubmitFunction {
        boolean submit(
                GuiGraphicsExtractor graphics,
                TrackingItemStackRenderState itemRenderState,
                BlockModelRenderState blockRenderState,
                int x0,
                int y0,
                int x1,
                int y1,
                float modelScale,
                float yawDegrees,
                float yOffset);
    }

    private static volatile SubmitFunction submitter;

    /**
     * Install the loader's PIP submission function (NeoForge's
     * {@code GuiGraphicsExtractor.submitPictureInPictureRenderState}).
     * Called from the loader's RegisterPictureInPictureRenderersEvent handler.
     */
    public static void installSubmitter(SubmitFunction function) {
        submitter = function;
    }

    /**
     * Extract and submit a PIP state. Returns false when the loader did not
     * install a submitter, allowing the caller to use its compatibility path.
     */
    public static boolean submit(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            double x,
            double y,
            double width,
            double height,
            double modelScale,
            double yawDegrees,
            double yOffset
    ) {
        SubmitFunction fn = submitter;
        if (fn == null || graphics == null || stack == null || stack.isEmpty()
                || width <= 0.0 || height <= 0.0) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return false;
        }

        // Same extraction as GuiGraphicsExtractor.item(): build the item's
        // render state so the PIP renderer can submit its mesh into the
        // offscreen PIP targets.
        TrackingItemStackRenderState itemRenderState = new TrackingItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(
                itemRenderState, stack, ItemDisplayContext.GUI, mc.level, null, 0);

        return fn.submit(
                graphics, itemRenderState, null,
                (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(x + width), (int) Math.round(y + height),
                (float) modelScale, (float) yawDegrees, (float) yOffset);
    }

    /**
     * Extract and submit a PIP state for a real {@link BlockState} (used by
     * the tutorial's {@code :block-3d} previews). Returns false when the
     * loader did not install a submitter.
     */
    public static boolean submitBlock(
            GuiGraphicsExtractor graphics,
            BlockState blockState,
            double x,
            double y,
            double width,
            double height,
            double modelScale,
            double yawDegrees,
            double yOffset
    ) {
        SubmitFunction fn = submitter;
        if (fn == null || graphics == null || blockState == null
                || width <= 0.0 || height <= 0.0) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return false;
        }

        BlockModelRenderState blockRenderState = new BlockModelRenderState();
        mc.getBlockModelResolver().update(
                blockRenderState, blockState, BlockDisplayContext.create());

        return fn.submit(
                graphics, null, blockRenderState,
                (int) Math.round(x), (int) Math.round(y),
                (int) Math.round(x + width), (int) Math.round(y + height),
                (float) modelScale, (float) yawDegrees, (float) yOffset);
    }

    @Override
    public float scale() {
        // Camera/model scaling is applied in renderToTexture after resetting
        // the base PIP renderer's orthographic pixel transform.
        return 1.0F;
    }
}
