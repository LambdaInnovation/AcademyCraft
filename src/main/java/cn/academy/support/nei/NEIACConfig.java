/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support.nei;

import cn.academy.core.AcademyCraft;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

/**
 * 
 * @author KSkun
 *
 */
public class NEIACConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        API.registerRecipeHandler(new FusorRecipeHandler());
        API.registerUsageHandler(new FusorRecipeHandler());
        API.registerRecipeHandler(new MetalFormerRecipeHandler());
        API.registerUsageHandler(new MetalFormerRecipeHandler());
    }

    @Override
    public String getName() {
        return "AcademyCraft";
    }

    @Override
    public String getVersion() {
        return AcademyCraft.VERSION;
    }

}
