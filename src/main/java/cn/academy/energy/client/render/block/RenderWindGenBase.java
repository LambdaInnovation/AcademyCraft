/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.client.render.block;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import cn.academy.core.client.Resources;
import cn.academy.energy.block.wind.TileWindGenBase;
import cn.lambdalib.multiblock.RenderBlockMultiModel;
import cn.lambdalib.util.deprecated.TileEntityModelCustom;

/**
 * @author WeAthFolD
 */
public class RenderWindGenBase extends RenderBlockMultiModel {
    
    ResourceLocation 
        TEX_NORMAL = Resources.getTexture("models/windgen_base"),
        TEX_DISABLED = Resources.getTexture("models/windgen_base_disabled");

    public RenderWindGenBase() {
        super(new TileEntityModelCustom(Resources.getModel("windgen_base")),
                null);
    }
    
    @Override
    public void drawAtOrigin(TileEntity te) {
        TileWindGenBase tile = (TileWindGenBase) te;
        this.tex = tile.complete ? TEX_NORMAL : TEX_DISABLED;
        super.drawAtOrigin(te);
    }
    
}
