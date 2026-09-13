package cn.li.mc1211.trigger;

import cn.li.mcmod.ModId;
import cn.li.mcver.ResourceLocations;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Generic custom advancement trigger (codec-based, Minecraft 1.21.1).
 *
 * <p>AC-side logic maps game events to a plain string criterion id; platform
 * layers only relay that id into Minecraft's advancement engine.</p>
 */
public class ModCustomTrigger extends SimpleCriterionTrigger<ModCustomTrigger.TriggerInstance> {

    public static final ResourceLocation ID = ResourceLocations.of(ModId.ID, "custom");

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
