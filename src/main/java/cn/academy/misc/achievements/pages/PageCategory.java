/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.achievements.pages;

import cn.academy.ability.api.Category;

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
