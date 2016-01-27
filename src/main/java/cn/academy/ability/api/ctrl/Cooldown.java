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
