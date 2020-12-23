package cn.academy.datapart;

import cn.academy.client.render.util.IHandRenderer;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.util.Debug;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@RegDataPart(value=EntityPlayer.class, side={Side.CLIENT})
@SideOnly(Side.CLIENT)
public class HandRenderOverrideData extends DataPart<EntityPlayer> {

    public static HandRenderOverrideData get(EntityPlayer p) {
        return EntityData.get(p).getPart(HandRenderOverrideData.class);
    }

    private IHandRenderer current = null;

    public void addInterrupt(IHandRenderer r) {
        current = r;
    }

    public void stopInterrupt(IHandRenderer r) {
        if (r == current)
            current = null;
    }

    public IHandRenderer getRenderer() {
        return Debug.assertNotNull(current);
    }

    public boolean isRendererPresent() {
        return current != null;
    }

}
