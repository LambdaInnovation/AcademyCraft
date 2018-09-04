package cn.academy.advancements.triggers;

import cn.academy.ability.Category;

/**
 * @author EAirPeter
 */
public final class ACLevelChangeTrigger<Cat extends Category> extends AchAbility<Cat>
{
    public ACLevelChangeTrigger(int lv, Cat cat, String id) {
        super(cat, id, lv);
    }
}