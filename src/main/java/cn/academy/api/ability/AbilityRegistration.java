package cn.academy.api.ability;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.annoreg.core.AnnotationData;
import cn.annoreg.core.RegistryType;
import cn.annoreg.core.RegistryTypeDecl;
import cn.annoreg.core.ctor.ConstructorUtils;

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
	public boolean registerClass(AnnotationData data) {
		Class<? extends Category> clazz = (Class<? extends Category>) data.getTheClass();
		Abilities.registerCat((Category) ConstructorUtils.newInstance(clazz));
		return true;
	}

	@Override
	public boolean registerField(AnnotationData data) {
		// TODO Auto-generated method stub
		return false;
	}

}
