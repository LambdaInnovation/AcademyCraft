package cn.academy.client.render.block;

import cn.academy.Resources;
import cn.academy.block.tileentity.TileWindGenBase;
import cn.lambdalib2.multiblock.RenderBlockMulti;
import cn.lambdalib2.registry.mc.RegTileEntityRender;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class RenderWindGenBase extends RenderBlockMulti<TileWindGenBase> {

    @RegTileEntityRender(TileWindGenBase.class)
    public static RenderWindGenBase renderer = new RenderWindGenBase();

    private ObjLegacyRender mdl = Resources.getModel("windgen_base");

    private ResourceLocation
        TEX_NORMAL = Resources.getTexture("models/windgen_base"),
        TEX_DISABLED = Resources.getTexture("models/windgen_base_disabled");

    @Override
    public void drawAtOrigin(TileWindGenBase te) {
        RenderUtils.loadTexture(te.isComplete() ? TEX_NORMAL : TEX_DISABLED);
        mdl.renderAll();
    }

}