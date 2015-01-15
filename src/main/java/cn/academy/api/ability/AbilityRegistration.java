package cn.academy.api.ability;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
		// TODO Auto-generated method stub
		return false;
	}

}
