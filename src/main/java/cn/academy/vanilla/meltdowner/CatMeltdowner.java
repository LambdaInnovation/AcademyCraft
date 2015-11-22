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
package cn.academy.vanilla.meltdowner;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.meltdowner.skill.*;

/**
 * @author WeAthFolD
 */
public class CatMeltdowner extends Category {

	public static final Skill
		electronBomb = ElectronBomb.instance,
		radIntensify = RadiationIntensify.instance,
		rayBarrage = RayBarrage.instance,
		scatterBomb = ScatterBomb.instance,
		lightShield = LightShield.instance,
		meltdowner = Meltdowner.instance,
		jetEngine = JetEngine.instance,
		mineRayBasic = MineRayBasic.instance,
		mineRayExpert = MineRayExpert.instance,
		mineRayLuck = MineRayLuck.instance,
		electronMissile = ElectronMissile.instance;

	public CatMeltdowner() {
		super("meltdowner");
		this.colorStyle.setColor4i(126, 255, 132, 80);

		// Lv1
		this.addSkill(electronBomb);
		this.addSkill(radIntensify);

		// Lv2
		this.addSkill(scatterBomb);
		this.addSkill(lightShield);

		// Lv3
		this.addSkill(meltdowner);
		this.addSkill(mineRayBasic);

		// Lv4
		this.addSkill(rayBarrage);
		this.addSkill(jetEngine);
		this.addSkill(mineRayExpert);

		// Lv5
		this.addSkill(mineRayLuck);
		this.addSkill(electronMissile);

		ModuleVanilla.addGenericSkills(this);

		// Deps
		scatterBomb.setParent(electronBomb, 0.8f);
		radIntensify.setParent(electronBomb, 0.5f);
		lightShield.setParent(electronBomb, 1.0f);
		meltdowner.setParent(scatterBomb, 0.8f);
		meltdowner.addSkillDep(lightShield, 0.8f);
		mineRayBasic.setParent(meltdowner, 0.3f);
		rayBarrage.setParent(meltdowner, 0.5f);
		jetEngine.setParent(meltdowner, 1.0f);
		mineRayExpert.setParent(mineRayBasic, 0.8f);
		mineRayLuck.setParent(mineRayExpert, 1.0f);
		electronMissile.setParent(jetEngine, 0.3f);
	}

}
