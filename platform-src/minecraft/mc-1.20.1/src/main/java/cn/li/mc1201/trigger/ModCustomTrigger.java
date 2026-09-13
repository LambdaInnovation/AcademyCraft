package cn.li.mc1201.trigger;

import cn.li.mcver.ResourceLocations;

import cn.li.mcmod.ModId;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

/**
 * Generic custom advancement trigger.
 *
 * <p>This trigger intentionally contains no game-specific concepts. AC-side logic
 * maps game events to a plain string criterion id, then platform layers only
 * relay that id into Minecraft's advancement engine.</p>
 */
public class ModCustomTrigger extends SimpleCriterionTrigger<ModCustomTrigger.TriggerInstance> {

    public static final ResourceLocation ID = ResourceLocations.of(ModId.ID, "custom");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(
            JsonObject json,
            ContextAwarePredicate player,
            DeserializationContext context) {
        String criterionId = GsonHelper.getAsString(json, "criterion_id", "");
        return new TriggerInstance(player, criterionId);
    }

    public void trigger(ServerPlayer player, String criterionId) {
        this.trigger(player, instance -> instance.matches(criterionId));
    }

    public static final class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final String criterionId;

        public TriggerInstance(ContextAwarePredicate player, String criterionId) {
            super(ID, player);
            this.criterionId = criterionId;
        }

        public boolean matches(String incomingId) {
            return criterionId.equals(incomingId);
        }
    }
}