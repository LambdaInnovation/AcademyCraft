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
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.meltdowner.skill.*;

/**
 * @author WeAthFolD
 */
public class CatMeltDowner extends Category {
	
	public static ElectronBomb electronBomb;
	public static RadiationIntensify radIntensify;
	public static RayBarrage rayBarrage;
	public static ScatterBomb scatterBomb;
	public static LightShield lightShield;
	public static MeltDowner meltDowner;
	public static JetEngine jetEngine;
	public static MineRayBasic mineRayBasic;
	public static MineRayExpert mineRayExpert;
	public static MineRayLuck mineRayLuck;
	public static ElectronMissile electronMissile;

	public CatMeltDowner() {
		super("melt_downer");
		this.colorStyle.setColor4i(126, 255, 132, 80);
		
		// Lv1
		this.addSkill(electronBomb = new ElectronBomb());
		this.addSkill(radIntensify = new RadiationIntensify());
		
		// Lv2
		this.addSkill(scatterBomb = new ScatterBomb());
		this.addSkill(lightShield = new LightShield());
		
		// Lv3
		this.addSkill(meltDowner = new MeltDowner());
		this.addSkill(mineRayBasic = new MineRayBasic());
		
		// Lv4
		this.addSkill(rayBarrage = new RayBarrage());
		this.addSkill(jetEngine = new JetEngine());
		this.addSkill(mineRayExpert = new MineRayExpert());
		
		// Lv5
		this.addSkill(mineRayLuck = new MineRayLuck());
		this.addSkill(electronMissile = new ElectronMissile());
		
		ModuleVanilla.addGenericSkills(this);
		
		// Deps
		scatterBomb.setParent(electronBomb, 0.8f);
		radIntensify.setParent(electronBomb, 0.5f);
		lightShield.setParent(electronBomb, 1.0f);
		meltDowner.setParent(scatterBomb, 0.8f);
		meltDowner.addSkillDep(lightShield, 0.8f);
		mineRayBasic.setParent(meltDowner, 0.3f);
		rayBarrage.setParent(meltDowner, 0.5f);
		jetEngine.setParent(meltDowner, 1.0f);
		mineRayExpert.setParent(mineRayBasic, 0.8f);
		mineRayLuck.setParent(mineRayExpert, 1.0f);
		electronMissile.setParent(jetEngine, 0.3f);
	}

}
