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
package cn.academy.ability.develop.condition;

import cn.academy.ability.develop.IDeveloper;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;

/**
 * Skill development constraint. This should represent a single atomic constraint on player.
 * If it has a icon to display, it will be displayed on the developer's skill description page.
 * @author WeAthFolD
 */
public interface IDevCondition {
    
    /**
     * @param developer The develope that performs this action
     * @return Whether the player can learn the given skill
     */
    boolean accepts(AbilityData data, IDeveloper developer, Skill skill);
    
    /**
     * @return The icon displayed in skill desc page
     */
    ResourceLocation getIcon();
    
    /**
     * @return The hovering text displayed in skill desc page
     */
    String getHintText();
    
}
