package cn.academy.misc.achievements.pages;

import cn.academy.misc.achievements.aches.ACAchievement;
import net.minecraft.stats.Achievement;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.AchievementPage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author EAirPeter
 */
public abstract class ACAchievementPage extends AchievementPage {
    
    private String name;
    private LinkedList<ACAchievement> list = new LinkedList<ACAchievement>();
    private ArrayList<Achievement> wrapped = new ArrayList<Achievement>();
    
    ACAchievementPage(String id) {
        super("dummy call");
        if (id.equals("default"))
            name = "AcademyCraft";
        else
            name = "ac.achievementpage." + id;
    }

    protected final void add(ACAchievement... aches) {
        for (ACAchievement ach : aches) {
            ach.registerAll();
            list.add(ach);
            wrapped.add(ach);
        }
    }
    
    @Override
    public String getName() {
        return StatCollector.translateToLocal(name);
    }
    
    @Override
    public List<Achievement> getAchievements() {
        return wrapped;
    }
    
}