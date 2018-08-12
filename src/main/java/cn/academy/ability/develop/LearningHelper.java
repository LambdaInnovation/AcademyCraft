package cn.academy.ability.develop;

import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.develop.action.IDevelopAction;
import cn.academy.ability.develop.condition.IDevCondition;
import net.minecraft.entity.player.EntityPlayer;

/**
 * All sorts of judging utilities about ability learning.
 * Available in both client and server.
 * @author WeAthFolD
 */
public class LearningHelper {
    
    /**
     * @return Whether the given player can level up currently
     */
    public static boolean canLevelUp(DeveloperType type, AbilityData aData) {
        return !aData.hasCategory() || aData.canLevelUp();
    }
    
    /**
     * Skills that can be potentially learned will be displayed on the Skill Tree gui.
     */
    public static boolean canBePotentiallyLearned(AbilityData data, Skill skill) {
        return data.getLevel() >= skill.getLevel() ||
                data.isSkillLearned(skill) ||
                (skill.getParent() == null || data.isSkillLearned(skill.getParent()));
    }
    
    /**
     * @return Whether the given skill can be learned.
     */
    public static boolean canLearn(AbilityData data, IDeveloper dev, Skill skill) {
        for(IDevCondition cond : skill.getDevConditions()) {
            if(!cond.accepts(data, dev, skill))
                return false;
        }
        return true;
    }

    public static double getEstimatedConsumption(EntityPlayer player, DeveloperType blktype, IDevelopAction type) {
        return blktype.getCPS() * type.getStimulations(player);
    }
    
}