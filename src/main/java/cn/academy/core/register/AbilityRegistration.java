package cn.academy.core.register;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.academy.api.ability.Abilities;
import cn.academy.api.ability.Category;
import cn.annoreg.base.RegistrationClassSimple;
import cn.annoreg.core.AnnotationData;
import cn.annoreg.core.RegistryTypeDecl;

@RegistryTypeDecl
public class AbilityRegistration extends RegistrationClassSimple<AbilityRegistration.RegAbility, Category> {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RegAbility {
	}

	public AbilityRegistration() {
		super(RegAbility.class, "Ability");
	}

	@Override
	protected void register(Class<? extends Category> theClass, RegAbility anno) throws Exception {
		Abilities.registerCat(theClass.newInstance());	
	}

}
