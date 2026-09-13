package cn.li.fabric1211.mixin;

import cn.li.fabric1211.access.PlayerPersistentDataAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerPersistentDataMixin implements PlayerPersistentDataAccess {
    private static final String PERSISTENT_KEY = "academy_persistent";

    @Unique
    private CompoundTag academy$persistentData;

    @Override
    public CompoundTag academy$getPersistentData() {
        if (academy$persistentData == null) {
            academy$persistentData = new CompoundTag();
        }
        return academy$persistentData;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void academy$readPersistentData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(PERSISTENT_KEY)) {
            academy$persistentData = tag.getCompound(PERSISTENT_KEY);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void academy$writePersistentData(CompoundTag tag, CallbackInfo ci) {
        if (academy$persistentData != null && !academy$persistentData.isEmpty()) {
            tag.put(PERSISTENT_KEY, academy$persistentData);
        }
    }
}
