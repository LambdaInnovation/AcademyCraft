package cn.li.mc262.trigger;

import cn.li.mcmod.ModId;
import cn.li.mcver.ResourceLocations;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Generic custom advancement trigger (codec-based, Minecraft 26.2).
 */
public class ModCustomTrigger extends SimpleCriterionTrigger<ModCustomTrigger.TriggerInstance> {

    public static final Identifier ID = ResourceLocations.of(ModId.ID, "custom");

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String criterionId) {
        this.trigger(player, instance -> instance.matches(criterionId));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, String criterionId)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        Codec.STRING.optionalFieldOf("criterion_id", "").forGetter(TriggerInstance::criterionId)
                ).apply(instance, TriggerInstance::new));

        public boolean matches(String incomingId) {
            return criterionId.equals(incomingId);
        }
    }
}
