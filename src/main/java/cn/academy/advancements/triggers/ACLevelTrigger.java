package cn.academy.advancements.triggers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ACLevelTrigger implements ICriterionTrigger<ACLevelTrigger.Instance>
{
    private final int level;
    public final ResourceLocation ID;
    private final Map<PlayerAdvancements, Listeners> listeners = Maps.newHashMap();

    public ACLevelTrigger(String domain, String name, int level){
        this(new ResourceLocation(domain, name), level);
    }
    public ACLevelTrigger(String name, int level){
        this(new ResourceLocation("academy", name), level);
    }
    public ACLevelTrigger(ResourceLocation rl, int level){
        ID = rl;
        this.level = level;
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public void addListener(PlayerAdvancements playerAdvancementsIn, Listener<ACLevelTrigger.Instance> listener)
    {
        ACLevelTrigger.Listeners consumeitemtrigger$listeners = this.listeners.get(playerAdvancementsIn);

        if (consumeitemtrigger$listeners == null)
        {
            consumeitemtrigger$listeners = new ACLevelTrigger.Listeners(playerAdvancementsIn);
            this.listeners.put(playerAdvancementsIn, consumeitemtrigger$listeners);
        }

        consumeitemtrigger$listeners.add(listener);
    }

    @Override
    public void removeListener(PlayerAdvancements playerAdvancementsIn, Listener<ACLevelTrigger.Instance> listener)
    {
        ACLevelTrigger.Listeners consumeitemtrigger$listeners = this.listeners.get(playerAdvancementsIn);

        if (consumeitemtrigger$listeners != null)
        {
            consumeitemtrigger$listeners.remove(listener);

            if (consumeitemtrigger$listeners.isEmpty())
            {
                this.listeners.remove(playerAdvancementsIn);
            }
        }
    }

    @Override
    public void removeAllListeners(PlayerAdvancements playerAdvancementsIn)
    {
        this.listeners.remove(playerAdvancementsIn);
    }

    @Override
    public ACLevelTrigger.Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        return new ACLevelTrigger.Instance(ID, level);
    }

    public void trigger(EntityPlayerMP player, int level) {
        ACLevelTrigger.Listeners enterblocktrigger$listeners = this.listeners.get(player.getAdvancements());
        if (enterblocktrigger$listeners != null)
        {
            enterblocktrigger$listeners.trigger(level);
        }

    }

    public void trigger(EntityPlayerMP player)
    {
    }

    public static class Instance extends AbstractCriterionInstance
    {
        private final int level;
        public Instance(ResourceLocation id, int level)
        {
            super(id);
            this.level = level;
        }

        public boolean test(int level)
        {
            return level>=this.level;
        }
    }

    static class Listeners
    {
        private final PlayerAdvancements playerAdvancements;
        private final Set<Listener<Instance>> listeners = Sets.newHashSet();

        public Listeners(PlayerAdvancements playerAdvancementsIn)
        {
            this.playerAdvancements = playerAdvancementsIn;
        }

        public boolean isEmpty()
        {
            return this.listeners.isEmpty();
        }

        public void add(Listener<ACLevelTrigger.Instance> listener)
        {
            this.listeners.add(listener);
        }

        public void remove(Listener<ACLevelTrigger.Instance> listener)
        {
            this.listeners.remove(listener);
        }

        public void trigger(int level)
        {
            List<Listener<Instance>> list = null;

            for (Listener<ACLevelTrigger.Instance> listener : this.listeners)
            {
                if (listener.getCriterionInstance().test(level))
                {
                    if (list == null)
                    {
                        list = Lists.newArrayList();
                    }

                    list.add(listener);
                }
            }

            if (list != null)
            {
                for (Listener<ACLevelTrigger.Instance> listener1 : list)
                {
                    listener1.grantCriterion(this.playerAdvancements);
                }
            }
        }
    }
}
