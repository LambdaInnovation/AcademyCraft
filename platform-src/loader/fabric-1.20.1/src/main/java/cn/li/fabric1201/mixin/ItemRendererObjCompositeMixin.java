package cn.li.fabric1201.mixin;

import cn.li.mc1201.client.render.item.ObjCompositeBakedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Picks the GUI or the world half of an {@link ObjCompositeBakedModel} before it is rendered.
 *
 * <p>Forge and NeoForge expose {@code BakedModel#applyTransform}, which lets a model hand back a
 * different model per display context; vanilla just reads transforms and quads off whatever it was
 * given. Swapping the argument at HEAD covers both — the display transforms and the quads then come
 * from the same sub-model, exactly as {@code applyTransform} arranges on the other loaders.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererObjCompositeMixin {

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private BakedModel academy$selectObjCompositeModel(BakedModel model, ItemStack stack,
                                                       ItemDisplayContext displayContext, boolean leftHand,
                                                       PoseStack poseStack, MultiBufferSource bufferSource,
                                                       int combinedLight, int combinedOverlay) {
        return model instanceof ObjCompositeBakedModel composite ? composite.selectModel(displayContext) : model;
    }
}
