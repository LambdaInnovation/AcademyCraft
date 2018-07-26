/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.client.render.block;

import cn.academy.core.Resources;
import cn.lambdalib2.template.client.render.block.RenderTileEntityModel;
import cn.lambdalib2.util.deprecated.TileEntityModelCustom;

/**
 * @author WeAthFolD
 */
public class RenderWindGenPillar extends RenderTileEntityModel {

    public RenderWindGenPillar() {
        super(new TileEntityModelCustom(Resources.getModel("windgen_pillar")), 
                Resources.getTexture("models/windgen_pillar"));
    }

}
