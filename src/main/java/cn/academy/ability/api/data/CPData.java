/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.data;

import cn.academy.ability.api.event.*;
import cn.academy.core.AcademyCraft;
import cn.academy.core.config.ConfigEnv;
import cn.academy.core.config.PlayerConfigEnv;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.annoreg.mc.RegEventHandler.Bus;
import cn.lambdalib.s11n.SerializeIncluded;
import cn.lambdalib.s11n.nbt.NBTS11n;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import cn.lambdalib.util.generic.MathUtils;
import com.google.common.base.Preconditions;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * CP but more than CP. CPData stores rather dynamic part of player ability data, 
 *     for example, whether the player is using ability, current CP and overload, etc.
 * @author WeAthFolD
 */
@Registrant
@RegDataPart(EntityPlayer.class)
public class CPData extends DataPart<EntityPlayer> {

    private ConfigEnv env;

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
    private float currentCP;

    @SerializeIncluded
    private float maxCP = 100.0f;
    @SerializeIncluded
    private float addMaxCP = 0.0f; // The CP added out of ability usage.

    @SerializeIncluded
    private float overload;

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
       env = PlayerConfigEnv.get(getEntity());
    }

    @Override
    public void tick() {
        AbilityData aData = AbilityData.get(getEntity());

        boolean remote = isClient();

        if(aData.hasCategory()) {
            if(untilRecover == 0) {
                float recover = getCPRecoverSpeed();
                currentCP += recover;
                if(currentCP > getMaxCP())
                    currentCP = getMaxCP();
            } else {
                untilRecover--;
            }
            
            if(untilOverloadRecover == 0) {
                float recover = getOverloadRecoverSpeed();
                
                overload -= recover;
                if(overload <= 0) {
                    overloadFine = true;
                    overload = 0;
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
            
            // Do the sync. Only sync when player activated ability to avoid waste
            if(!remote && activated) {
                ++tickSync;
                if(tickSync >= (dataDirty ? 4 : 10)) {
                    dataDirty = false;
                    tickSync = 0;
                    sync();
                }
            }
        }
    }
    
    public boolean isActivated() {
        return activated;
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
        currentCP = MathUtils.clampf(0, maxCP, cp);

        markDirty();
    }

    public void setOverload(float newOverload) {
        overload = MathUtils.clampf(0, maxOverload, newOverload);

        markDirty();
    }

    private void markDirty() {
        if(!isClient()) {
            dataDirty = true;
        }
    }
    
    public float getCP() {
        return currentCP;
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
    
    public float getOverload() {
        return overload;
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
        if(getEntity().capabilities.isCreativeMode) {
            addMaxCP(cpToAdd);
            addMaxOverload(overloadToAdd);
            return true;
        }
        if(currentCP - cpToAdd < 0)
            return false;
        
        addOverload(overloadToAdd);
        consumeCP(cpToAdd);
        
        if(overload > getMaxOverload()) {
            overloadFine = false;
        }
        
        return true;
    }
    
    /**
     * Consume the CP and does the overload without any validation. This should be used WITH CAUTION.
     */
    public void performWithForce(float overload, float cp) {
        if(getEntity().capabilities.isCreativeMode)
            return;
        overload = estimateOverload(overload);
        cp = estimateConsumption(cp);
        
        this.overload += overload;
        this.currentCP -= cp;
        
        if(currentCP < 0) currentCP = 0;
        if(overload > getMaxOverload() * 2) overload = getMaxOverload() * 2;
        
        if(overload > getMaxOverload()) overloadFine = false;
        
        untilRecover = getInt("cp_recover_cooldown");
        untilOverloadRecover = getInt("overload_recover_cooldown");
        
        addMaxCP(cp);
        addMaxOverload(overload);
        
        if(!isClient())
            dataDirty = true;
    }
    
    /***
     * A pre test to judge whether the skill can be performed.
     * @return Whether the player can perform the ability with the given consumption currently,
     *     takes account of creative mode.
     */
    public boolean canPerform(float cp) {
        return getEntity().capabilities.isCreativeMode || this.getCP() >= cp;
    }
    
    private void addMaxCP(float consumedCP) {
        consumedCP = estimateConsumption(consumedCP);

        AbilityData aData = AbilityData.get(getEntity());
        float max = getMaxAddCP(aData.getLevel());
        addMaxCP += consumedCP * getFloat("maxcp_incr_rate");
        if(addMaxCP > max)
            addMaxCP = max;
    }
    
    private void addMaxOverload(float overload) {
        overload = estimateOverload(overload);

        AbilityData aData = AbilityData.get(getEntity());
        float max = getMaxAddOverload(aData.getLevel());
        float add = MathUtils.clampf(0, 10, overload * getFloat("maxo_incr_rate"));
        addMaxOverload += add;
        if(addMaxOverload > max)
            addMaxOverload = max;
    }

    private float getCPRecoverSpeed() {
        return getFloat("cp_recover_speed") *
                0.0001f * maxCP *
                MathUtils.lerpf(1, 2, currentCP / maxCP);
    }

    private float getOverloadRecoverSpeed() {
        return getFloat("overload_recover_speed") *
                Math.max(0.002f * maxOverload,
                        0.007f * maxOverload * MathUtils.lerpf(1, 0.5f, overload / maxOverload / 2));
    }
    
    public boolean canLevelUp() {
        return AbilityData.get(getEntity()).getLevel() < 5 && getLevelProgress() == 1;
    }
    
    public float getLevelProgress() {
        return addMaxCP / getMaxAddCP(AbilityData.get(getEntity()).getLevel());
    }
    
    /**
     * Can be called in both sides. Consumes the CP and return whether the action is successful.
     * Will just make a simulation in client side.
     */
    public boolean consumeCP(float amt) {

        
        if(currentCP < amt)
            return false;
        currentCP -= amt;
        untilRecover = getInt("cp_recover_cooldown");
        
        addMaxCP(amt);
        
        if(!isClient())
            dataDirty = true;
        
        return true;
    }
    
    /**
     * Add a specific amount of overload. Note that the action will ALWAYS be
     * successful, even if you try to overload over 2*maxOverload. (The value will
     * stay at 2*maxo)
     */
    public void addOverload(float amt) {
        if(getEntity().capabilities.isCreativeMode)
            return;
        
        overload += amt;
        if(overload > 2 * getMaxOverload())
            overload = 2 * getMaxOverload();
        
        untilOverloadRecover = getInt("overload_recover_cooldown");
        
        addMaxOverload(amt);
        
        if(!isClient())
            dataDirty = true;
    }

    public float estimateOverload(float amt) {
        if(isOverloaded()) {
            amt *= getFloat("overload_o_mul");
        }
        return amt;
    }

    public float estimateConsumption(float amt) {
        if(isOverloaded()) {
            amt *= getFloat("overload_cp_mul");
        }
        return amt;
    }
    
    public boolean isOverloaded() {
        return overload > getMaxOverload();
    }
    
    /**
     * SERVER ONLY. <br/>
     * Should be called when player upgrades level. 
     * Recalc the max overload and max cp based on 
     * currently learned buff skills and level.
     */
    public void recalcMaxValue() {
        AbilityData data = AbilityData.get(getEntity());
        
        this.maxCP = env.pipeFloat(PipedValues.MAXCP, getInitCP(data.getLevel()));
        this.maxOverload = env.pipeFloat(PipedValues.MAXOVERLOAD, getInitOverload(data.getLevel()));
        
        currentCP = getMaxCP();
        overload = 0;
        
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
        return env.getInt(path(name));
    }

    private float getFloat(String name) {
        return env.getFloat(path(name));
    }

    public float getInitCP(int level) {
        return env.getFloatArray(path("init_cp"))[level];
    }

    public float getInitOverload(int level) {
        return env.getFloatArray(path("init_overload"))[level];
    }

    public float getMaxAddCP(int level) {
        return env.getFloatArray(path("add_cp"))[level];
    }

    public float getMaxAddOverload(int level) {
        return env.getFloatArray(path("add_overload"))[level];
    }

    private String path(String name) {
        return "ac.ability.data." + name;
    }

    /**
     * Effective in SERVER. Recover all the cp and overload.
     */
    public void recoverAll() {
        if(!isClient()) {
            currentCP = getMaxCP();
            overload = 0;
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
    
    @RegEventHandler(Bus.Forge)
    public static class Events {
        
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
            if(!event.wakeImmediatly && !event.updateWorld && event.setSpawn)
                CPData.get(event.entityPlayer).recoverAll();
        }
        
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void playerDeath(LivingDeathEvent event) {
            if(event.entityLiving instanceof EntityPlayer) {
                CPData.get((EntityPlayer) event.entityLiving).recoverAll();
            }
        }
        
    }

}
