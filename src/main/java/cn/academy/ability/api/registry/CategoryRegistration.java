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
package cn.academy.ability.api.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.ability.api.registry.CategoryRegistration.RegCategory;
import cn.annoreg.base.RegistrationFieldSimple;
import cn.annoreg.core.LoadStage;
import cn.annoreg.core.RegistryTypeDecl;

/**
 * Use this anno to register category on the fly.
 * @author WeAthFolD
 */
@RegistryTypeDecl
public class CategoryRegistration extends RegistrationFieldSimple<RegCategory, Category> {
	
	public CategoryRegistration() {
		super(RegCategory.class, "ac_Category");
		setLoadStage(LoadStage.PRE_INIT);
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface RegCategory {}

	@Override
	protected void register(Category value, RegCategory anno, String field)
			throws Exception {
		CategoryManager.INSTANCE.register(value);
	}
	
}
