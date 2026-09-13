package cn.li.fabric1211.mixin;

import cn.li.mcbase.runtime.DamageInterception;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Fabric registration shim; all damage rewriting lives in common runtime. */
@Mixin(LivingEntity.class)
abstract class LivingEntityDamageMixin {
    @ModifyArgs(
            method = "actuallyHurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private void myMod$rewriteDamage(Args args) {
        DamageInterception.rewriteArmorInput((LivingEntity) (Object) this, args);
    }
}
