package cn.academy.datapart;

import cn.academy.event.ability.*;
import cn.academy.event.ability.CalcEvent.CPRecoverSpeed;
import cn.academy.event.ability.CalcEvent.OverloadRecoverSpeed;
import cn.academy.ACConfig;
import cn.lambdalib2.registry.mc.RegEventHandler;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.nbt.NBTS11n;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.util.MathUtils;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * CP but more than CP. CPData stores rather dynamic part of player ability data,
 *     for example, whether the player is using ability, current CP and overload, etc.
 * @author WeAthFolD
 */
@RegDataPart(EntityPlayer.class)
public class CPData extends DataPart<EntityPlayer> {

    private Config config;
    private AbilityData abilityData;

    private static final String
        MSG_POST_EVENT = "post_event",
        MSG_ACTIVATE_SVR = "actv_svr";

    public interface IInterfSource {
        /**
         * @return Whether the inteference should still be applied. If not the interferer will be removed.
         */
        boolean interfering();
    }

    private Map<String, IInterfSource> interfSources = new HashMap<>();

    @SerializeIncluded
    private boolean activated = false;

    @SerializeIncluded
    private float curCP;

    @SerializeIncluded
    private float maxCP = 100.0f;
    @SerializeIncluded
    private float addMaxCP = 0.0f; // The CP added out of ability usage.

    @SerializeIncluded
    private float curOverload;

    @SerializeIncluded
    private float maxOverload = 100.0f;
    @SerializeIncluded
    private float addMaxOverload = 0.0f; // The Overload added out of ability usage.

    @SerializeIncluded
    private boolean overloadFine = true;
    @SerializeIncluded
    private boolean interfering = false; // Cached value

    /**
     * Tick counter for cp recover.
     */
    @SerializeIncluded
    private int untilRecover;
    /**
     * Tick conter for overload recover.
     */
    @SerializeIncluded
    private int untilOverloadRecover;

    private boolean dataDirty = false;

    private int tickSync;

    public CPData() {
        setTick(true);
        setClientNeedSync();
        setNBTStorage();
    }

    public static CPData get(EntityPlayer player) {
        return EntityData.get(player).getPart(CPData.class);
    }

    @Override
    public void wake() {
        config = ACConfig.instance().getConfig("ac.ability.data");
        abilityData = AbilityData.get(getEntity());
    }

    @Override
    public void tick() {
        AbilityData aData = AbilityData.get(getEntity());

        boolean remote = isClient();

        if(aData.hasCategory()) {
            if(untilRecover == 0) {
                float recover = getCPRecoverSpeed();
                curCP += recover;
                if(curCP > getMaxCP())
                    curCP = getMaxCP();
            } else {
                untilRecover--;
            }

            if(untilOverloadRecover == 0) {
                float recover = getOverloadRecoverSpeed();

                curOverload -= recover;
                if(curOverload <= 0) {
                    overloadFine = true;
                    curOverload = 0;
                }
            } else {
                untilOverloadRecover--;
            }

            // Update interefering
            if (!remote) {
                Iterator<Entry<String, IInterfSource>> iter = interfSources.entrySet().iterator();
                while (iter.hasNext()) {
                    Entry<String, IInterfSource> entry = iter.next();
                    if (!entry.getValue().interfering()) {
                        iter.remove();
                    }
                }

                boolean newInterf = !interfSources.isEmpty();
                if (newInterf != interfering) {
                    dataDirty = true;
                }

                interfering = newInterf;
            }

            // Do the sync.
            if(!remote) {
                int interval = (activated ? 1 : 3) * (dataDirty ? 4 : 10);

                ++tickSync;
                if(tickSync >= interval) {
                    dataDirty = false;
                    tickSync = 0;
                    sync();
                }
            }
        }
    }

    public boolean isActivated() {
        // At initialization stage, it's possible that activated become true, but AbilityData isn't
        //  yet synchronized, so additional check is required.
        return abilityData.hasCategory() && activated;
    }

    /**
     * @return Whether the player can use ability currently.
     *     e.g. whether the skills can be executed by pressing ability keys.
     */
    public boolean canUseAbility() {
        return activated && overloadFine && !interfering;
    }

    /**
     * Sets the ability active state. Available in both client and server. In client syncs to server internally.
     */
    public void setActivateState(boolean state) {
        if (isClient()) {
            activated = state; // Set client state in advance to prevent display lag
            NetworkMessage.sendToServer(this, MSG_ACTIVATE_SVR, state);
        } else {
            Preconditions.checkState(!state || AbilityData.get(getEntity()).hasCategory(),
                    "Trying to activate ability when player doesn't have one");

            if (activated != state) {
                activated = state;
                NetworkMessage.sendToSelf(this, MSG_POST_EVENT, activated);
                NetworkMessage.sendTo(getEntity(), this, MSG_POST_EVENT, activated);
            }

            markDirty();
        }
    }

    public void setCP(float cp) {
        curCP = MathUtils.clampf(0, maxCP, cp);

        markDirty();
    }

    public void setOverload(float newOverload) {
        curOverload = MathUtils.clampf(0, getMaxOverload(), newOverload);

        markDirty();
    }

    private void markDirty() {
        if(!isClient()) {
            dataDirty = true;
        }
    }

    public float getCP() {
        return curCP;
    }

    public float getMaxCP() {
        return maxCP + addMaxCP;
    }

    public float getRawMaxCP() {
        return maxCP;
    }

    public float getAddMaxCP() {
        return addMaxCP;
    }

    public void setAddMaxCP(float value) {
        AbilityData aData = AbilityData.get(getEntity());
        float max = getMaxAddCP(aData.getLevel());
        addMaxCP = Math.min(max, value);

        markDirty();
    }

    public float getOverload() {
        return curOverload;
    }

    public float getMaxOverload() {
        return maxOverload + addMaxOverload;
    }

    public float getRawMaxOverload() {
        return maxOverload;
    }

    public float getAddMaxOverload() {
        return addMaxOverload;
    }

    /**
     * Performs a generic ability action.
     * Will fail when either can't overload anymore or can't consume cp.
     * @param overloadToAdd Amount of overload
     * @param cpToAdd Amount of CP
     */
    public boolean perform(float overloadToAdd, float cpToAdd) {
        return performInternal(overloadToAdd, cpToAdd, false);
    }

    /**
     * Consume the CP and does the overload without any validation. This should be used WITH CAUTION.
     */
    public void performWithForce(float overload, float cp) {
        performInternal(overload, cp, true);
    }

    private boolean performInternal(float overload, float cp, boolean force) {
        Pair<Float, Float> res = performData(overload, cp);
        overload = res.getLeft();
        cp = res.getRight();

        boolean result;

        // consume CP/Overload
        if (!getEntity().capabilities.isCreativeMode) {
            result = consumeCP(cp, force);
            if (result) {
                addOverload(overload);
            }
        } else {
            result = true;
        }

        // Add maxcp/maxoverload
        if (result) {
            addMaxCP(cp);
            addMaxOverload(overload);
        }

        return result;
    }

    private Pair<Float, Float> performData(float overload, float cp) {
        CalcEvent.SkillPerform evt = new CalcEvent.SkillPerform(getEntity(), overload, cp);
        MinecraftForge.EVENT_BUS.post(evt);

        return Pair.of(evt.overload, evt.cp);
    }

    /***
     * A pre test to judge whether the skill can be performed.
     * @return Whether the player can perform the ability with the given consumption currently,
     *     takes account of creative mode.
     */
    public boolean canPerform(float cp) {
        return getEntity().capabilities.isCreativeMode || this.getCP() >= cp;
    }

    public boolean isOverloadRecovering() {
        return !overloadFine;
    }

    private void addMaxCP(float consumedCP) {
        setAddMaxCP(addMaxCP + consumedCP * getFloat("maxcp_incr_rate"));
    }

    private void addMaxOverload(float overload) {
        AbilityData aData = AbilityData.get(getEntity());
        float max = getMaxAddOverload(aData.getLevel());
        float add = MathUtils.clampf(0, 10, overload * getFloat("maxo_incr_rate"));
        addMaxOverload += add;
        if(addMaxOverload > max)
            addMaxOverload = max;
    }

    private float getCPRecoverSpeed() {
        float raw = getFloat("cp_recover_speed") *
                0.0003f * maxCP *
                MathUtils.lerpf(1, 2, curCP / maxCP);

        return CalcEvent.calc(new CPRecoverSpeed(getEntity(), 1)) * raw;
    }

    private float getOverloadRecoverSpeed() {
        float raw = getFloat("overload_recover_speed") *
                Math.max(0.002f * maxOverload,
                        0.007f * maxOverload * MathUtils.lerpf(1, 0.5f, curOverload / maxOverload / 2));

        return CalcEvent.calc(new OverloadRecoverSpeed(getEntity(), 1)) * raw;
    }

    /**
     * Can be called in both sides. Consumes the CP and return whether the action is successful.
     * Will just make a simulation in client side.
     */
    private boolean consumeCP(float amt, boolean force) {
        if(!force && curCP < amt)
            return false;

        curCP = Math.max(0, curCP - amt);
        untilRecover = getInt("cp_recover_cooldown");

        if(!isClient())
            dataDirty = true;

        return true;
    }

    /**
     * Add a specific amount of overload.
     */
    private void addOverload(float amt) {
        if(getEntity().capabilities.isCreativeMode)
            return;

        curOverload = Math.min(getMaxOverload(), curOverload + amt);

        untilOverloadRecover = getInt("overload_recover_cooldown");

        if(curOverload == getMaxOverload()) {
            MinecraftForge.EVENT_BUS.post(new OverloadEvent(getEntity()));
            overloadFine = false;
        }

        if(!isClient())
            dataDirty = true;
    }

    public boolean isOverloaded() {
        return !overloadFine && untilOverloadRecover > 0;
    }

    /**
     * SERVER ONLY. <br/>
     * Should be called when player upgrades level.
     * Recalc the max overload and max cp based on
     * currently learned buff skills and level.
     */
    public void recalcMaxValue() {
        AbilityData data = AbilityData.get(getEntity());

        this.maxCP = getInitCP(data.getLevel());
        this.maxOverload = getInitOverload(data.getLevel());

        curCP = getMaxCP();
        curOverload = 0;

        if(!isClient())
            sync();
    }

    // Inteference API

    // Observers

    /**
     * @return If the ability is being intefered.
     */
    public boolean isInterfering() {
        return interfering;
    }

    /**
     * @return Whether the interference source with given name is present.
     */
    public boolean hasInterfSource(String name) {
        return interfSources.containsKey(name);
    }

    // Modifiers

    /**
     * Adds a interference source. SERVER only.
     * @param id The unique id of the source. If the source with same id previously exists, it will be discarded.
     * @param interferer The source
     */
    public void addInterf(String id, IInterfSource interferer) {
        checkSide(Side.SERVER);

        interfSources.put(id, interferer);
    }

    /**
     * Removes all inteference source. SERVER only.
     */
    public void removeInterf() {
        checkSide(Side.SERVER);

        interfSources.clear();
    }

    /**
     * Removes the given interference source, if any. SERVER only.
     * @param name The name of given interference
     */
    public void removeInterf(String name) {
        checkSide(Side.SERVER);

        interfSources.remove(name);
    }

    // Inteference API end

    private int getInt(String name) {
        return config.getInt(name);
    }

    private float getFloat(String name) {
        return (float) config.getDouble(name);
    }

    public float getInitCP(int level) {
        float rawValue = config.getDoubleList("init_cp").get(level).floatValue();

        return CalcEvent.calc(new CalcEvent.MaxCP(getEntity(), rawValue));
    }

    public float getInitOverload(int level) {
        float rawValue = config.getDoubleList("init_overload").get(level).floatValue();

        return CalcEvent.calc(new CalcEvent.MaxOverload(getEntity(), rawValue));
    }

    public float getMaxAddCP(int level) {
        return config.getDoubleList("add_cp").get(level).floatValue();
    }

    public float getMaxAddOverload(int level) {
        return config.getDoubleList("add_overload").get(level).floatValue();
    }

    /**
     * Effective in SERVER. Recover all the cp and overload.
     */
    public void recoverAll() {
        if(!isClient()) {
            curCP = getMaxCP();
            curOverload = 0;
            overloadFine = false;
            sync();
        }
    }

    @Override
    public void toNBT(NBTTagCompound tag) {
        NBTS11n.write(tag, this);
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {
        NBTS11n.read(tag, this);
    }

    @Listener(channel=MSG_ACTIVATE_SVR, side=Side.SERVER)
    private void activateAtServer(boolean state) {
        setActivateState(state);
    }

    @Listener(channel=MSG_POST_EVENT, side={Side.CLIENT,Side.SERVER})
    private void postEvent(boolean state) {
        MinecraftForge.EVENT_BUS.post(state ?
                new AbilityActivateEvent(getEntity()) :
                new AbilityDeactivateEvent(getEntity()));
    }

    public enum Events {
        @RegEventHandler()
        instance;

        @SubscribeEvent
        public void changedCategory(CategoryChangeEvent event) {
            CPData cpData = CPData.get(event.player);

            if(!AbilityData.get(event.player).hasCategory()) {
                cpData.setActivateState(false);
            }
            cpData.recalcMaxValue();
        }

        @SubscribeEvent
        public void learnedSkill(SkillLearnEvent event) {
            CPData.get(event.player).recalcMaxValue();
        }

        @SubscribeEvent
        public void changedLevel(LevelChangeEvent event) {
            CPData cpData = CPData.get(event.player);
            cpData.addMaxCP = cpData.addMaxOverload = 0;
            cpData.recalcMaxValue();
        }

        @SubscribeEvent
        public void playerWakeup(PlayerWakeUpEvent event) {
            if(!event.wakeImmediately() && !event.updateWorld() && event.shouldSetSpawn())
                CPData.get(event.getEntityPlayer()).recoverAll();
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void playerDeath(LivingDeathEvent event) {
            if(event.getEntityLiving() instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) event.getEntityLiving();
                CPData cpData = CPData.get(player);

                cpData.recoverAll();
                cpData.setActivateState(false);
            }
        }

    }

}