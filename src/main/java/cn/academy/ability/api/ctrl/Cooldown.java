/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.ctrl;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.context.ClientRuntime;
import cn.academy.ability.api.context.ClientRuntime.CooldownData;
import cn.lambdalib.annoreg.core.Registrant;

/**
 * Class that handles cooldown. Currently, the cooldown in SERVER will be completely ignored, counting only
 *     client cooldown on SkillInstance startup. But you can still visit the method to avoid side dependency.
 * @author WeAthFolD
 * @deprecated implementation moved to {@link ClientRuntime}
 */
@Registrant
@Deprecated
public class Cooldown {
    
    public static void setCooldown(Controllable c, int cd) {
        ClientRuntime.instance().setCooldownRaw(c, cd);
    }
    
    public static boolean isInCooldown(Controllable c) {
        return ClientRuntime.instance().isInCooldownRaw(c);
    }
    
    public static CooldownData getCooldownData(Controllable c) {
        return null;
    }

}
