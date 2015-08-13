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
public class CatMeltdowner extends Category {
	
	public static RayBarrage rayBarrage;
	public static ScatterBomb scatterBomb;
	public static LightShield lightShield;
	public static JetEngine jetEngine;
	public static MineRayBasic minerayBasic;
	public static MineRayExpert mineRayExpert;

	public CatMeltdowner() {
		super("melt_downer");
		this.colorStyle.setColor4i(126, 255, 132, 80);
		
		this.defineTypes("A", "B", "C", "D", "passive"); //For test
		
		this.addSkill("A", rayBarrage = new RayBarrage());
		this.addSkill("A", scatterBomb = new ScatterBomb());
		this.addSkill("A", lightShield = new LightShield());
		this.addSkill("A", jetEngine = new JetEngine());
		this.addSkill("A", minerayBasic = new MineRayBasic());
		this.addSkill("A", mineRayExpert = new MineRayExpert());
		
		ModuleVanilla.addGenericSkills(this);
	}

}
