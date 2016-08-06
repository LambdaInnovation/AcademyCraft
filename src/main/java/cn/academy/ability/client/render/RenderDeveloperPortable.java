/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.client.render;

import cn.academy.core.Resources;
import cn.lambdalib.template.client.render.item.RenderModelItem;
import cn.lambdalib.util.deprecated.ItemModelCustom;
import cn.lambdalib.util.generic.VecUtils;

/**
 * @author WeAthFolD
 */
public class RenderDeveloperPortable extends RenderModelItem {

    public RenderDeveloperPortable() {
        super(new ItemModelCustom(Resources.getModel("developer_portable")), 
            Resources.getTexture("models/developer_portable"));
        renderInventory = false;
        this.scale = 6;
        this.equipRotation = VecUtils.vec(0, -10, -5);
        this.equipOffset = VecUtils.vec(0.6, 0, -.2);
        this.entityItemOffset.zCoord = 0.2;
        this.entityItemRotation.xCoord = 15;
        this.thirdPersonScale = 0.6;
        this.thirdPersonOffset = VecUtils.vec(0.1, 0.05, 0.2);
    }

}
