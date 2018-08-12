package cn.academy.achievement.pages;

import cn.academy.ability.Category;

/**
 * @author EAirPeter
 */
public abstract class PageCategory<Cat extends Category> extends ACAchievementPage{

    //PageCt
    
    protected final Cat category;
    
    public PageCategory(Cat cat) {
        super("cat_" + cat.getName());
        category = cat;
    }
    
    public final Category getCategory() {
        return category;
    }
    
}