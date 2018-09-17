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

    public static final ACTrigger aPhaseLiquid = new ACTrigger("phase_liquid");
    public static final ACTrigger aMatrix1 = new ACTrigger("matrix1");
    public static final ACTrigger aMatrix2 = new ACTrigger("matrix2");
    public static final ACTrigger aNode = new ACTrigger("node");
    public static final ACTrigger aDeveloper1 = new ACTrigger("developer1");
    public static final ACTrigger aDeveloper2 = new ACTrigger("developer2");
    public static final ACTrigger aDeveloper3 = new ACTrigger("developer3");
    public static final ACTrigger aPhaseGen = new ACTrigger("phasegen");
    public static final ACTrigger aSolarGen = new ACTrigger("solargen");
    public static final ACTrigger aWindGen = new ACTrigger("windgen");
    public static final ACTrigger aCrystal = new ACTrigger("crystal");
    public static final ACTrigger aTerminal = new ACTrigger("terminal");

    public static final ACLevelChangeTrigger<CatElectromaster> eLv1 = new ACLevelChangeTrigger<>(1, VanillaCategories.electromaster, "lv1");
    public static final ACLevelChangeTrigger<CatElectromaster> eLv2 = new ACLevelChangeTrigger<>(2, VanillaCategories.electromaster, "lv2");
    public static final ACLevelChangeTrigger<CatElectromaster> eLv3 = new ACLevelChangeTrigger<>(3, VanillaCategories.electromaster, "lv3");
    public static final ACLevelChangeTrigger<CatElectromaster> eLv4 = new ACLevelChangeTrigger<>(4, VanillaCategories.electromaster, "lv4");
    public static final ACLevelChangeTrigger<CatElectromaster> eLv5 = new ACLevelChangeTrigger<>(5, VanillaCategories.electromaster, "lv5");

    public static final ACTrigger aArcGen = new ACTrigger(CatElectromaster.arcGen.getName());
    public static final ACTrigger aAtCreeper = new ACTrigger("attack_creeper");
    public static final ACTrigger aBodyIntensify = new ACTrigger(CatElectromaster.bodyIntensify.getName());
    public static final ACTrigger aMagMovement = new ACTrigger(CatElectromaster.magMovement.getName());
    public static final ACTrigger aMagManip = new ACTrigger(CatElectromaster.magMovement.getName());
    //public static final ACAchievement aIronSand;                //Manual
    public static final ACTrigger aMineDetect = new ACTrigger(CatElectromaster.mineDetect.getName());
    public static final ACTrigger aThunderBolt = new ACTrigger(CatElectromaster.thunderBolt.getName());
    public static final ACTrigger aRailgun = new ACTrigger(CatElectromaster.railgun.getName());
    public static final ACTrigger aThunderClap = new ACTrigger(CatElectromaster.thunderClap.getName());

    public static final ACLevelChangeTrigger<CatMeltdowner> mLv1 = new ACLevelChangeTrigger<>(1, VanillaCategories.meltdowner, "lv1");
    public static final ACLevelChangeTrigger<CatMeltdowner> mLv2 = new ACLevelChangeTrigger<>(2, VanillaCategories.meltdowner, "lv2");
    public static final ACLevelChangeTrigger<CatMeltdowner> mLv3 = new ACLevelChangeTrigger<>(3, VanillaCategories.meltdowner, "lv3");
    public static final ACLevelChangeTrigger<CatMeltdowner> mLv4 = new ACLevelChangeTrigger<>(4, VanillaCategories.meltdowner, "lv4");
    public static final ACLevelChangeTrigger<CatMeltdowner> mLv5 = new ACLevelChangeTrigger<>(5, VanillaCategories.meltdowner, "lv5");

    public static final ACTrigger aRadIntensify = new ACTrigger(CatMeltdowner.radIntensify.getName());
    public static final ACTrigger aLightShield = new ACTrigger(CatMeltdowner.lightShield.getName());
    public static final ACTrigger aMeltdowner = new ACTrigger(CatMeltdowner.meltdowner.getName());
    public static final ACTrigger aMineRay = new ACTrigger("mine_ray");
    public static final ACTrigger aJetEngine = new ACTrigger(CatMeltdowner.jetEngine.getName());
    public static final ACTrigger aElectronMissile = new ACTrigger(CatMeltdowner.electronMissile.getName());


    public static final ACLevelChangeTrigger<CatTeleporter> tLv1 = new ACLevelChangeTrigger<>(1, VanillaCategories.teleporter, "lv1");
    public static final ACLevelChangeTrigger<CatTeleporter> tLv2 = new ACLevelChangeTrigger<>(2, VanillaCategories.teleporter, "lv2");
    public static final ACLevelChangeTrigger<CatTeleporter> tLv3 = new ACLevelChangeTrigger<>(3, VanillaCategories.teleporter, "lv3");
    public static final ACLevelChangeTrigger<CatTeleporter> tLv4 = new ACLevelChangeTrigger<>(4, VanillaCategories.teleporter, "lv4");
    public static final ACLevelChangeTrigger<CatTeleporter> tLv5 = new ACLevelChangeTrigger<>(5, VanillaCategories.teleporter, "lv5");

    public static final ACTrigger aThreateningTeleport = new ACTrigger(CatTeleporter.threateningTP.getName());
    public static final ACTrigger aCriticalAttack  = new ACTrigger("critical_attack");
    public static final ACTrigger aIgnoreBarrier = new ACTrigger("ignore_barrier");
    public static final ACTrigger aFlashing = new ACTrigger(CatTeleporter.flashing.getName());
    public static final ACTrigger aMastery = new ACTrigger("mastery");


    public static final ACLevelChangeTrigger<CatVecManip> vLv1 = new ACLevelChangeTrigger<>(1, VanillaCategories.vecManip, "lv1");
    public static final ACLevelChangeTrigger<CatVecManip> vLv2 = new ACLevelChangeTrigger<>(2, VanillaCategories.vecManip, "lv2");
    public static final ACLevelChangeTrigger<CatVecManip> vLv3 = new ACLevelChangeTrigger<>(3, VanillaCategories.vecManip, "lv3");
    public static final ACLevelChangeTrigger<CatVecManip> vLv4 = new ACLevelChangeTrigger<>(4, VanillaCategories.vecManip, "lv4");
    public static final ACLevelChangeTrigger<CatVecManip> vLv5 = new ACLevelChangeTrigger<>(5, VanillaCategories.vecManip, "lv5");

    public static final ACTrigger aGroundshock = new ACTrigger(Groundshock.getName());
    public static final ACTrigger aDirBlast = new ACTrigger(DirectedBlastwave.getName());
    public static final ACTrigger aStormWing = new ACTrigger(StormWing.getName());
    public static final ACTrigger aBloodRetro = new ACTrigger(BloodRetrograde.getName());
    public static final ACTrigger aVecReflection = new ACTrigger(VecReflection.getName());

    @StateEventCallback
    public static void init(FMLInitializationEvent event) {
        DispatcherAch.init();

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
        if (ach == null || ach instanceof ACTrigger == false) {
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