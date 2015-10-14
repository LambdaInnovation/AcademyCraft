package cn.academy.misc.tutorial;

import cn.academy.ability.ModuleAbility;
import cn.academy.ability.api.Category;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.electromaster.CatElectromaster;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

@Registrant
@RegInit
public class ModuleTutorial {
	public static void init(){
		try {
			ACTutorial.addTutorial("phase_liquid").addConditions(conditions)
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
