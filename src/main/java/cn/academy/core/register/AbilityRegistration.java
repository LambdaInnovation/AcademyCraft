package cn.academy.core.register;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.academy.api.ability.Abilities;
import cn.academy.api.ability.Category;
import cn.annoreg.core.AnnotationData;
import cn.annoreg.core.RegistryType;
import cn.annoreg.core.RegistryTypeDecl;

@RegistryTypeDecl
public class AbilityRegistration extends RegistryType {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RegAbility {
	}

	public AbilityRegistration() {
		super(RegAbility.class, "Ability");
	}

	@Override
	public boolean registerClass(AnnotationData data) throws Exception {
		Class<? extends Category> clazz = (Class<? extends Category>) data.getTheClass();
		Abilities.registerCat((Category) clazz.newInstance());
		return true;
	}

	@Override
	public boolean registerField(AnnotationData data) {
		return false;
	}

}
