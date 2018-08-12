package cn.academy.client.render.block;

import cn.academy.Resources;
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