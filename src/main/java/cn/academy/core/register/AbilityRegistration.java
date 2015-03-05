/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.register;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.academy.api.ability.Abilities;
import cn.academy.api.ability.Category;
import cn.annoreg.base.RegistrationClassSimple;
import cn.annoreg.core.LoadStage;
import cn.annoreg.core.RegistryTypeDecl;

@RegistryTypeDecl
public class AbilityRegistration extends RegistrationClassSimple<AbilityRegistration.RegAbility, Category> {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RegAbility {
	}

	public AbilityRegistration() {
		super(RegAbility.class, "Ability");
		this.setLoadStage(LoadStage.INIT);
	}

	@Override
	protected void register(Class<? extends Category> theClass, RegAbility anno) throws Exception {
		Abilities.registerCat(theClass.newInstance());	
	}

}
