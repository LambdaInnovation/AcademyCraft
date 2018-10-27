package cn.academy.advancements;

import cn.academy.Resources;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.electromaster.CatElectromaster;
import cn.academy.ability.vanilla.meltdowner.CatMeltdowner;
import cn.academy.ability.vanilla.teleporter.CatTeleporter;
import cn.academy.ability.vanilla.vecmanip.CatVecManip;
import cn.academy.ability.vanilla.vecmanip.skill.*;
import cn.academy.AcademyCraft;
import cn.academy.advancements.triggers.ACLevelChangeTrigger;
import cn.academy.advancements.triggers.ACTrigger;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.advancements.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * @author EAirPeter, Paindar
 */
public class ACAchievements {

    public static final ACTrigger aJoinGame = new ACTrigger("join_game");
    public static final ACTrigger aOpenTutorial = new ACTrigger("open_tutorial");
    public static final ACTrigger aGettingPhase = new ACTrigger("getting_phase");
    public static final ACTrigger aPhaseGen = new ACTrigger("crafting_phase_gen");
    public static final ACTrigger aNode = new ACTrigger("crafting_node");
    public static final ACTrigger aMatrix = new ACTrigger("ac_matrix");
    public static final ACTrigger aTerminalInstalled = new ACTrigger("ac_terminal_installed");
    public static final ACTrigger aGettingFactor = new ACTrigger("getting_factor");
    public static final ACTrigger aDeveloper = new ACTrigger("ac_developer");
    public static final ACTrigger aDevCategory= new ACTrigger("dev_category");
    public static final ACTrigger aLevel3= new ACTrigger("ac_level_3");
    public static final ACTrigger aLevel5= new ACTrigger("ac_level_5");
    public static final ACTrigger aConvertCategory = new ACTrigger("convert_category");
    public static final ACTrigger aLearnSkill = new ACTrigger("ac_learning_skill");
    public static final ACTrigger aMilestone = new ACTrigger("ac_milestone");
    public static final ACTrigger aExpFull = new ACTrigger("ac_exp_full");
    public static final ACTrigger aOverload = new ACTrigger("ac_overload");


    @StateEventCallback
    public static void init(FMLInitializationEvent event) {
        DispatcherAch.init();
        CriteriaTriggers.register(aJoinGame);
        CriteriaTriggers.register(aOpenTutorial);
        CriteriaTriggers.register(aGettingPhase);
        CriteriaTriggers.register(aPhaseGen);
        CriteriaTriggers.register(aNode);
        CriteriaTriggers.register(aMatrix);
        CriteriaTriggers.register(aTerminalInstalled);
        CriteriaTriggers.register(aGettingFactor);
        CriteriaTriggers.register(aDeveloper);
        CriteriaTriggers.register(aDevCategory);
        CriteriaTriggers.register(aLevel3);
        CriteriaTriggers.register(aLevel5);
        CriteriaTriggers.register(aConvertCategory);
        CriteriaTriggers.register(aLearnSkill);
        CriteriaTriggers.register(aMilestone);
        CriteriaTriggers.register(aExpFull);
        CriteriaTriggers.register(aOverload);

    }
    
    /**
     * Trigger an achievement
     * @param player The player
     * @param achid The id of the achievement
     * @return true if succeeded
     * This method is server-only. --Paindar
     */
    public static boolean trigger(EntityPlayer player, ResourceLocation achid) {
        if (!(player instanceof EntityPlayerMP))
            return false;

        ICriterionTrigger ach = CriteriaTriggers.get(achid);
        if (ach == null || (!(ach instanceof ACTrigger))) {
            AcademyCraft.log.warn("AC Achievement '" + achid + "' does not exist");
            return false;
        }
        ((ACTrigger)ach).trigger((EntityPlayerMP) player);
        return true;
    }

    public static boolean trigger(EntityPlayer player, String achid) {
        return trigger(player, Resources.res(achid));
    }
    
}