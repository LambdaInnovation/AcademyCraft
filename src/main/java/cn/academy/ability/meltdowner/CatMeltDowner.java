package cn.academy.ability.meltdowner;

import cn.academy.api.ability.AbilityRegistration.RegAbility;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.annoreg.core.RegistrationClass;

/**
 * @author acaly
 *
 */
@RegistrationClass
@RegAbility
public class CatMeltDowner extends Category {

	@Override
	protected void register() {
		this.addLevel(new Level(this, 400.0f, 800.0f, 0.5f, 1.0f));
		this.addLevel(new Level(this, 800.0f, 2000.0f, 1.5f, 1.8f));
		this.addLevel(new Level(this, 2000.0f, 3500.0f, 2.2f, 2.6f));
		this.addLevel(new Level(this, 3500.0f, 6000.0f, 3.0f, 3.5f));
		this.addLevel(new Level(this, 6000.0f, 10000.0f, 4.0f, 5.0f));
	}
	
}
