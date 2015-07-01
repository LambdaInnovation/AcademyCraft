package cn.academy.vanilla.heatmaster;

import cn.academy.ability.api.Category;
import cn.academy.knowledge.KnowledgeData;
import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.electromaster.skill.ArcGen;
import cn.academy.vanilla.electromaster.skill.MagAttract;
import cn.academy.vanilla.electromaster.skill.MagMovement;
import cn.academy.vanilla.electromaster.skill.MineDetect;
import cn.academy.vanilla.electromaster.skill.Railgun;
import cn.academy.vanilla.heatmaster.skill.WorldHeater;

public class CatHeatMatser extends Category
{
	public WorldHeater worldhearter;

	public CatHeatMatser()
	{
		super("heat_master");

		defineTypes("default","passive");

		addSkill("default",worldhearter= new WorldHeater());

		//KnowledgeData.addKnowledges(new String[] {});

		ModuleVanilla.addGenericSkills(this);
	}
}
