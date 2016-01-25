/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.api.data;

import cn.academy.ability.api.event.AbilityActivateEvent;
import cn.academy.ability.api.event.AbilityDeactivateEvent;
import cn.academy.ability.api.event.CategoryChangeEvent;
import cn.academy.ability.api.event.LevelChangeEvent;
import cn.academy.ability.api.event.SkillLearnEvent;
import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.annoreg.mc.RegEventHandler.Bus;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption;
import cn.lambdalib.ripple.Path;
import cn.lambdalib.ripple.ScriptFunction;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import cn.lambdalib.util.generic.MathUtils;
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
@RegDataPart("CP")
public class CPData extends DataPart<EntityPlayer> {

    // Names are very short for serialization efficiency
    // And random characters because.. IDK 233
    static final String
        TAG_CURCP = "C",
        TAG_MAXCP = "M",
        TAG_UNTILRECOVER = "I",
        TAG_OVERLOAD = "D",
        TAG_MAX_OVERLOAD = "N",
        TAG_UNTIL_OVERLOAD_RECOVER = "J",
        TAG_OVERLOAD_FINE = "B",
        TAG_ADD_MAXCP = "1",
        TAG_ADD_MAX_OVERLOAD = "2",
        TAG_INTERFERING = "3",
        TAG_ACTIVATED = "A";

    public interface IInterfSource {
        /**
         * @return Whether the inteference should still be applied. If not the interferer will be removed.
         */
        boolean interfering();
    }

    public static int 
        RECOVER_COOLDOWN,
        OVERLOAD_COOLDOWN;
    public static float 
        OVERLOAD_O_MUL,
        OVERLOAD_CP_MUL;

    @RegInitCallback
    public static void init() {
        RECOVER_COOLDOWN = getIntParam("recover_cooldown");
        OVERLOAD_COOLDOWN = getIntParam("overload_cooldown");
        OVERLOAD_O_MUL = getFloatParam("overload_o_mul");
        OVERLOAD_CP_MUL = getFloatParam("overload_cp_mul");
    }
    
    private AbilityData aData;

    private Map<String, IInterfSource> interfSources = new HashMap<>();
    
    private boolean activated = false;
    
    private float currentCP;
    private float maxCP = 100.0f;
    private float addMaxCP = 0.0f; // The CP added out of ability usage.
    
    private float overload;
    private float maxOverload = 100.0f;
    private float addMaxOverload = 0.0f; // The Overload added out of ability usage.

    private boolean overloadFine = true;
    private boolean interfering = false; // Cached value
    
    /**
     * Tick counter for cp recover.
     */
    private int untilRecover;
    /**
     * Tick conter for overload recover.
     */
    private int untilOverloadRecover;
    
    private boolean dataDirty = false;
    
    private int tickSync;

    public CPData() {
        setTick();
    }
    
    public static CPData get(EntityPlayer player) {
        return EntityData.get(player).getPart(CPData.class);
    }

    @Override
    public void tick() {
        if(aData == null)
            aData = AbilityData.get(getEntity());

        boolean remote = isRemote();

        if(aData.isLearned()) {
            if(untilRecover == 0) {
                float recover = getFunc("recover_speed")
                        .callFloat(currentCP, getMaxCP());
                currentCP += recover;
                if(currentCP > getMaxCP())
                    currentCP = getMaxCP();
            } else {
                untilRecover--;
            }
            
            if(untilOverloadRecover == 0) {
                float recover = getFunc("overload_recover_speed")
                        .callFloat(overload, getMaxOverload());
                
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
    
    public void activate() {
        if(isRemote()) {
            activateAtServer();
            return;
        }
        
        if(AbilityData.get(getEntity()).isLearned() && !activated) {
            activated = true;
            MinecraftForge.EVENT_BUS.post(new AbilityActivateEvent(getEntity()));
            sync();
        } else {
            AcademyCraft.log.warn("Trying to activate ability when player doesn't have one");
        }
    }
    
    public void deactivate() {
        if(isRemote()) {
            deactivateAtServer();
            return;
        }
        
        if(activated) {
            activated = false;
            MinecraftForge.EVENT_BUS.post(new AbilityDeactivateEvent(getEntity()));
            sync();
        }
    }
    
    public void setCP(float cp) {
        currentCP = cp;
        if(currentCP < 0) currentCP = 0;
        if(currentCP > getMaxCP()) currentCP = getMaxCP();
        if(!isRemote())
            dataDirty = true;
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
        
        this.overload += overload;
        this.currentCP -= cp;
        
        if(currentCP < 0) currentCP = 0;
        if(overload > getMaxOverload() * 2) overload = getMaxOverload() * 2;
        
        if(overload > getMaxOverload()) overloadFine = false;
        
        untilRecover = RECOVER_COOLDOWN;
        untilOverloadRecover = OVERLOAD_COOLDOWN;
        
        addMaxCP(cp);
        addMaxOverload(overload);
        
        if(!isRemote())
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
        AbilityData aData = AbilityData.get(getEntity());
        float max = getFunc("add_cp").callFloat(aData.getLevel());
        addMaxCP += getFunc("maxcp_rate").callFloat(consumedCP);
        if(addMaxCP > max)
            addMaxCP = max;
    }
    
    private void addMaxOverload(float overload) {
        AbilityData aData = AbilityData.get(getEntity());
        float max = getFunc("add_overload").callFloat(aData.getLevel());
        float add = MathUtils.clampf(0, 10, getFunc("maxo_rate").callFloat(overload));
        addMaxOverload += add;
        if(addMaxOverload > max)
            addMaxOverload = max;
    }
    
    public boolean canLevelUp() {
        return AbilityData.get(getEntity()).getLevel() < 5 && getLevelProgress() == 1;
    }
    
    public float getLevelProgress() {
        return addMaxCP / getFunc("add_cp").callFloat(AbilityData.get(getEntity()).getLevel());
    }
    
    /**
     * Can be called in both sides. Consumes the CP and return whether the action is successful.
     * Will just make a simulation in client side.
     */
    public boolean consumeCP(float amt) {
        if(isOverloaded()) {
            amt *= OVERLOAD_CP_MUL;
        }
        
        if(currentCP < amt)
            return false;
        currentCP -= amt;
        untilRecover = RECOVER_COOLDOWN;
        
        addMaxCP(amt);
        
        if(!isRemote())
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
        
        untilOverloadRecover = OVERLOAD_COOLDOWN;
        
        addMaxOverload(amt);
        
        if(!isRemote())
            dataDirty = true;
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
        
        this.maxCP = AcademyCraft.pipeline.pipeFloat
            ("ability.maxcp", getInitCP(data.getLevel()), getEntity());
        
        this.maxOverload = AcademyCraft.pipeline.pipeFloat(
            "ability.maxo", getInitOverload(data.getLevel()), getEntity());
        
        currentCP = getMaxCP();
        overload = 0;
        
        if(!isRemote())
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
        assertSide(Side.SERVER);

        interfSources.put(id, interferer);
    }

    /**
     * Removes all inteference source. SERVER only.
     */
    public void removeInterf() {
        assertSide(Side.SERVER);

        interfSources.clear();
    }

    /**
     * Removes the given interference source, if any. SERVER only.
     * @param name The name of given interference
     */
    public void removeInterf(String name) {
        assertSide(Side.SERVER);

        interfSources.remove(name);
    }

    // Inteference API end
    
    public static float getInitCP(int level) {
        return getFunc("init_cp").callFloat(level);
    }
    
    public static float getInitOverload(int level) {
        return getFunc("init_overload").callFloat(level);
    }
    
    /**
     * Effective in SERVER. Recover all the cp and overload.
     */
    public void recoverAll() {
        if(!isRemote()) {
            currentCP = getMaxCP();
            overload = 0;
            overloadFine = false;
            sync();
        }
    }
    
    @Override
    public NBTTagCompound toNBTSync() {
        NBTTagCompound tag = toNBT();
        tag.setBoolean(TAG_ACTIVATED, activated);
        return tag;
    }
    
    @Override
    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        
        tag.setFloat(TAG_CURCP,                    currentCP);
        tag.setFloat(TAG_MAXCP,                    maxCP);
        tag.setInteger(TAG_UNTILRECOVER,           untilRecover);
        
        tag.setFloat(TAG_OVERLOAD,                 overload);
        tag.setFloat(TAG_MAX_OVERLOAD,             maxOverload);
        tag.setInteger(TAG_UNTIL_OVERLOAD_RECOVER, untilOverloadRecover);
        
        tag.setBoolean(TAG_OVERLOAD_FINE,          overloadFine);
        
        tag.setFloat(TAG_ADD_MAXCP,                addMaxCP);
        tag.setFloat(TAG_ADD_MAX_OVERLOAD,            addMaxOverload);

        tag.setBoolean(TAG_INTERFERING,            interfering);
        
        return tag;
    }
    
    @Override
    public void fromNBTSync(NBTTagCompound tag) {
        fromNBT(tag);
        
        boolean lastActivated = activated;
        activated = tag.getBoolean(TAG_ACTIVATED);
        
        if(isRemote()) {
            if(lastActivated ^ activated) {
                MinecraftForge.EVENT_BUS.post(activated ? 
                    new AbilityActivateEvent(getEntity()) :
                    new AbilityDeactivateEvent(getEntity()));
            }
        }
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {
        currentCP = tag.getFloat(TAG_CURCP);
        maxCP = tag.getFloat(TAG_MAXCP);
        untilRecover = tag.getInteger(TAG_UNTILRECOVER);

        overload = tag.getFloat(TAG_OVERLOAD);
        maxOverload = tag.getFloat(TAG_MAX_OVERLOAD);
        untilOverloadRecover = tag.getInteger(TAG_UNTIL_OVERLOAD_RECOVER);

        overloadFine = tag.getBoolean(TAG_OVERLOAD_FINE);

        addMaxCP = tag.getFloat(TAG_ADD_MAXCP);
        addMaxOverload = tag.getFloat(TAG_ADD_MAX_OVERLOAD);

        interfering = tag.getBoolean(TAG_INTERFERING);
    }
    
    private static double getDoubleParam(String name) {
        return AcademyCraft.getScript().root.getDouble(path(name));
    }
    
    private static int getIntParam(String name) {
        return AcademyCraft.getScript().root.getInteger(path(name));
    }
    
    private static float getFloatParam(String name) {
        return AcademyCraft.getScript().root.getFloat(path(name));
    }
    
    private static ScriptFunction getFunc(String name) {
        return AcademyCraft.getScript().root.getFunction(path(name));
    }
    
    private static Path path(String name) {
        return new Path("ac.ability.cp." + name);
    }
    
    @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
    private void activateAtServer() {
        activate();
    }
    
    @RegNetworkCall(side = Side.SERVER, thisStorage = StorageOption.Option.INSTANCE)
    private void deactivateAtServer() {
        deactivate();
    }
    
    @RegEventHandler(Bus.Forge)
    public static class Events {
        
        @SubscribeEvent
        public void changedCategory(CategoryChangeEvent event) {
            CPData cpData = CPData.get(event.player);
            
            if(!AbilityData.get(event.player).isLearned()) {
                cpData.deactivate();
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
            if(!event.isCanceled() && event.entityLiving instanceof EntityPlayer) {
                CPData.get((EntityPlayer) event.entityLiving).recoverAll();
            }
        }
        
    }

}
