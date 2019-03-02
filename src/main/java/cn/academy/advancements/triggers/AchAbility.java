package cn.academy.advancements.triggers;

import cn.academy.ability.Category;

/**
 * @author EAirPeter
 */
public class AchAbility<Cat extends Category> extends ACLevelTrigger
{

    //Ach
    //AchEv
    protected final Cat category;
    
    public AchAbility(Cat cat, String id, int level) {
        super(cat.getName() + "." + id, level);
        category = cat;
    }
}