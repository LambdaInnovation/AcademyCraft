package cn.academy.misc.achievements.pages;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cn.academy.misc.achievements.aches.ACAchievement;
import cn.academy.misc.achievements.aches.AchBasic;
import cn.academy.misc.achievements.aches.AchCAnd;
import cn.academy.misc.achievements.aches.AchCSingle;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;

public class ACAchievementPage extends AchievementPage {
	
	private String name;
	private LinkedList<ACAchievement> list = new LinkedList();
	
	public ACAchievementPage(String pName) {
		super(pName);
		this.name = pName;
	}
	
	protected final AchBasic addAchBasic(String id, int x, int y, Item display, Achievement parent) {
		AchBasic ach = new AchBasic(id, x, y, display, parent);
		list.add(ach);
		return ach;
	}
	protected final AchBasic addAchBasic(String id, int x, int y, Block display, Achievement parent) {
		AchBasic ach = new AchBasic(id, x, y, display, parent);
		list.add(ach);
		return ach;
	}
	protected final AchBasic addAchBasic(String id, int x, int y, ItemStack display, Achievement parent) {
		AchBasic ach = new AchBasic(id, x, y, display, parent);
		list.add(ach);
		return ach;
	}
	protected final AchBasic addAchBasic(AchBasic ach) {
		list.add(ach);
		return ach;
	}
	
	protected final AchCAnd addAchCAnd(String id, int x, int y, Item display, Achievement parent) {
		AchCAnd ach = new AchCAnd(id, x, y, display, parent);
		list.add(ach);
		return ach;
	}
	protected final AchCAnd addAchCAnd(String id, int x, int y, Block display, Achievement parent) {
		AchCAnd ach = new AchCAnd(id, x, y, display, parent);
		list.add(ach);
		return ach;
	}
	protected final AchCAnd addAchCAnd(String id, int x, int y, ItemStack display, Achievement parent) {
		AchCAnd ach = new AchCAnd(id, x, y, display, parent);
		list.add(ach);
		return ach;
	}
	protected final AchCAnd addAchCAnd(AchCAnd ach) {
		list.add(ach);
		return ach;
	}
	
	protected final AchCSingle addAchCSingle(String id, int x, int y, Item display, Achievement parent) {
		AchCSingle ach = new AchCSingle(id, x, y, display, parent);
		list.add(ach);
		return ach;
	}
	protected final AchCSingle addAchCSingle(String id, int x, int y, Block display, Achievement parent) {
		AchCSingle ach = new AchCSingle(id, x, y, display, parent);
		list.add(ach);
		return ach;
	}
	protected final AchCSingle addAchCSingle(String id, int x, int y, ItemStack display, Achievement parent) {
		AchCSingle ach = new AchCSingle(id, x, y, display, parent);
		list.add(ach);
		return ach;
	}
	protected final AchCSingle addAchCSingle(AchCSingle ach) {
		list.add(ach);
		return ach;
	}
	
	protected final ACAchievement addACAchievement(ACAchievement ach) {
		list.add(ach);
		return ach;
	}
	
	protected final void clearAchievements() {
		for (ACAchievement ach : list)
			ach.urItemCrafted();
		list.clear();
	}

	List<Achievement> gened = new LinkedList<Achievement>();
	protected final void genList() {
		gened = new LinkedList<Achievement>(list);
		
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public List<Achievement> getAchievements() {
		return gened;
	}
	
}
