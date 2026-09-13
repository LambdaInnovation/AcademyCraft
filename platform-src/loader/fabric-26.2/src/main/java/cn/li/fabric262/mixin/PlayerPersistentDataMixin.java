package cn.li.fabric262.mixin;

import cn.li.fabric262.access.PlayerPersistentDataAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    private void academy$readPersistentData(ValueInput input, CallbackInfo ci) {
        academy$persistentData = input.read(PERSISTENT_KEY, CompoundTag.CODEC).orElse(null);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void academy$writePersistentData(ValueOutput output, CallbackInfo ci) {
        if (academy$persistentData != null && !academy$persistentData.isEmpty()) {
            output.store(PERSISTENT_KEY, CompoundTag.CODEC, academy$persistentData);
        }
    }
}
