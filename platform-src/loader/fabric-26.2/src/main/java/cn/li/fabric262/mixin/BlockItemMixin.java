package cn.li.fabric262.mixin;

import cn.li.mcbase.clj.ClojureInterop;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void academy$beforePlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        try {
            if (context == null) {
                return;
            }
            Level level = context.getLevel();
            if (level == null || level.isClientSide()) {
                return;
            }

            Player player = context.getPlayer();
            BlockPos pos = context.getClickedPos();
            Block block = ((BlockItem) (Object) this).getBlock();

            Object shouldCancel = ClojureInterop.invoke(
                "cn.li.fabric262.integration.events",
                "handle-block-place-mixin",
                player,
                level,
                pos,
                block
            );

            if (Boolean.TRUE.equals(shouldCancel)) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
        } catch (Throwable ignored) {
            // Keep placement stable even if bridge logic fails.
        }
    }
}
