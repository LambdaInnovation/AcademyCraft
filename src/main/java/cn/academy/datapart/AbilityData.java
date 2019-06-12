package cn.academy.datapart;

import cn.academy.ACConfig;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.analytic.events.AnalyticLevelUpEvent;
import cn.academy.event.ability.*;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.nbt.NBTS11n;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import com.google.common.base.Preconditions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This DataPart handles player category, player learned skills and respective skill exps.
 */
@RegDataPart(EntityPlayer.class)
public class AbilityData extends DataPart<EntityPlayer> {


    public static AbilityData get(EntityPlayer player) {
        return EntityData.get(player).getPart(AbilityData.class);
    }

    private static final String MSG_CAT_CHANGE = "cat_change", MSG_SKILL_LEARNED = "skill_learned";

    @SerializeIncluded
    private int catID = -1;
    @SerializeIncluded
    private BitSet learnedSkills;
    @SerializeIncluded
    private float[] skillExps;
    @SerializeIncluded
    private int level;
    @SerializeIncluded
    private float expAddedThisLevel;
    
    private int updateTicker = 0;

    public AbilityData() {
        learnedSkills = new BitSet(32);
        skillExps = new float[32];

        setTick(true);
        setNBTStorage();
        setClientNeedSync();
    }
    
    /**
     * Server only. Changes player's category.
     * @param c The category. If null, sets player to no category.
     */
    public void setCategory(Category c) {
        checkSide(Side.SERVER);

        int id = c == null ? -1 : c.getCategoryID();
        if(id != catID) {
            catID = id;

            // Resets the level
            if(catID != -1 && level == 0) {
                level = 1;
            }
            if(catID == -1 && level != 0) {
                level = 0;
            }

            for(int i = 0; i < skillExps.length; ++i) {
                skillExps[i] = 0.0f;
            }
            learnedSkills.set(0, learnedSkills.size(), false);

            sync();

            informCategoryChange();
            sendMessage(MSG_CAT_CHANGE);
        }
    }

    /**
     * @return The player's current category. Never null.
     * @throws IllegalStateException if player currently doesn't have category
     */
    public Category getCategory() {
        Preconditions.checkState(catID != -1);
        return CategoryManager.INSTANCE.getCategory(catID);
    }

    public Category getCategoryNullable() {
        return hasCategory() ? getCategory() : null;
    }

    /**
     * @return Whether the player has ability category (learned skill)
     */
    public boolean hasCategory() {
        return catID >= 0;
    }

    /**
     * @return Player's current level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Server only. Sets player's current level.
     * @param lv The new level, currently must be in [1, 5]
     * @throws IllegalStateException if in client
     */
    public void setLevel(int lv) {
        checkSide(Side.SERVER);
        checkLearned();

        if(level != lv) {
            if(level<lv){
                MinecraftForge.EVENT_BUS.post(new AnalyticLevelUpEvent(getEntity()));
            }
            level = lv;
            expAddedThisLevel = 0;
            MinecraftForge.EVENT_BUS.post(new LevelChangeEvent(getEntity()));
            sync();
        }
    }

    /**
     * For DEBUG/Command only: max out current level.
     */
    public void maxOutLevelProgress() {
        expAddedThisLevel = 100;
        sync();
    }
    
    /**
     * Get all the learned skills.
     */
    public List<Skill> getLearnedSkillList() {
        return getSkillListFiltered(this::isSkillLearned);
    }
    
    /**
     * Get all the learned and controllable skills.
     */
    public List<Skill> getControllableSkillList() {
        return getSkillListFiltered(s -> (s.canControl() && isSkillLearned(s)));
    }
    
    /**
     * Server only. Learn the specified skill.
     */
    public void learnSkill(Skill s) {
        checkSide(Side.SERVER);
        checkSkill(s);

        setSkillLearnState(s, true);
    }

    /**
     * Server only. Set a skill's learning state.
     */
    public void setSkillLearnState(Skill s, boolean value) {
        checkSide(Side.SERVER);
        checkSkill(s);

        int id = s.getID();
        boolean prevState = learnedSkills.get(id);

        learnedSkills.set(id, value);

        if (!prevState && value) {
            fireSkillLearn(s);
            sendToLocal(MSG_SKILL_LEARNED, s);
        }

        sync();
    }

    @Listener(channel=MSG_SKILL_LEARNED, side=Side.CLIENT)
    private void fireSkillLearn(Skill s) {
        MinecraftForge.EVENT_BUS.post(new SkillLearnEvent(getEntity(), s));
    }

    /**
     * Gets exp of a skill. If skill isn't in player's category return 0.
     */
    public float getSkillExp(Skill skill) {
        if (!checkSkillSoft(skill)) {
            return 0.0f;
        } else {
            return skill.expCustomized ? skill.getSkillExp(this) : this.skillExps[skill.getID()];
        }
    }

    /**
     * Adds exp to specified skill. Muted in client.
     */
    public void addSkillExp(Skill skill, float amt) {
        if (checkSideSoft(Side.SERVER)) {
            checkSide(Side.SERVER);
            checkSkill(skill);

            learnSkill(skill);

            int id = skill.getID();
            float added = Math.min(1.0f - skillExps[id], amt);
            skillExps[skill.getID()] += added;
            addLevelProgress(amt);

            MinecraftForge.EVENT_BUS.post(new SkillExpChangedEvent(getEntity(), skill));
            MinecraftForge.EVENT_BUS.post(new SkillExpAddedEvent(getEntity(), skill, amt));
            scheduleUpdate(25);
        }
    }

    public float getLevelProgress() {
        float threshold = getLevelTotalExp() * (level == 4 ? 1.333f : 0.666f);
        return threshold == 0 ? 1 : Math.min(1, expAddedThisLevel / threshold);
    }

    public boolean canLevelUp() {
        return getLevel() < 5 && getLevelProgress() == 1;
    }
    
    /**
     * Brutely set the skill exp. This should only used by commands.
     */
    public void setSkillExp(Skill skill, float exp) {
        checkSide(Side.SERVER);
        checkSkill(skill);
        if (isSkillLearned(skill)) {
            skillExps[skill.getID()] = exp;
            if(!isClient()) {
                MinecraftForge.EVENT_BUS.post(new SkillExpChangedEvent(getEntity(), skill));
                scheduleUpdate(25);
            }
        }
    }

    /**
     * Server only. Learn all the skills.
     */
    public void learnAllSkills() {
        checkSide(Side.SERVER);

        if(hasCategory()) {
            learnedSkills.set(0, getCategory().getSkillCount(), true);
            sync();
        }
    }

    /**
     * Check whether a skill is learned.
     */
    public boolean isSkillLearned(Skill s) {
        return checkSkillSoft(s) && learnedSkills.get(s.getID());
    }

    private List<Skill> getSkillListFiltered(Predicate<Skill> predicate) {
        if (!hasCategory()) {
            return Collections.emptyList();
        } else {
            return getCategory().getSkillList()
                    .stream()
                    .filter(predicate)
                    .collect(Collectors.toList());
        }
    }
    
    private void scheduleUpdate(int ticks) {
        if(updateTicker == 0)
            updateTicker = ticks;
        else if(updateTicker != 1)
            updateTicker -= 1;
    }

    private void checkSkill(Skill s) {
        Preconditions.checkState(checkSkillSoft(s), "Skill " + s + " not in category #" + catID);
    }

    private float getLevelTotalExp() {
        if (hasCategory()) {
            List<Skill> testSkills = getCategory().getSkillList()
                    .stream()
                    .filter(skill -> skill.canControl() && skill.getLevel() == getLevel())
                    .collect(Collectors.toList());
            return testSkills.size();
        }

        return 0;
    }

    private boolean checkSkillSoft(Skill s) {
        return s.getCategory().getCategoryID() == catID;
    }

    private void addLevelProgress(float consumedExp) {
        float mul0 = getCategory().getProgIncrRate();
        float mul1 = (float) ACConfig.instance().getDouble("ac.ability.data.prog_incr_rate");
        expAddedThisLevel += consumedExp * mul0 * mul1;
    }

    private void checkLearned() {
        Preconditions.checkState(hasCategory(), "Player doesn't have category");
    }

    @Listener(channel=MSG_CAT_CHANGE, side={Side.CLIENT,Side.SERVER})
    private void informCategoryChange() {
        MinecraftForge.EVENT_BUS.post(new CategoryChangeEvent(getEntity()));
    }

    @Override
    public void tick() {
        if(!isClient()) {
            if(updateTicker > 0) {
                if(--updateTicker == 0) {
                    sync();
                }
            }
        }
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {
        NBTS11n.read(tag, this);
    }

    @Override
    public void toNBT(NBTTagCompound tag) {
        NBTS11n.write(tag, this);
    }

}