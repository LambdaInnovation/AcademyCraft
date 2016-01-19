package cn.academy.ability.api.context;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * A KeyDelegate is provided by a skill and handles key delegation of ONE key.
 * @author WeAthFolD
 */
public abstract class KeyDelegate {

    public void onKeyDown() {}

    public void onKeyUp() {}

    public void onKeyAbort() {}

    public void onKeyTick() {}

    @SideOnly(Side.CLIENT)
    protected Minecraft getMC() {
        return Minecraft.getMinecraft();
    }

    @SideOnly(Side.CLIENT)
    protected EntityPlayer getPlayer() {
        return getMC().thePlayer;
    }

    /**
     * @return The icon displayed in the key hint UI.
     */
    public abstract ResourceLocation getIcon();

    private Object identifier;

    /**
     * @return The identifier used in cooldown
     */
    protected Object createID() {
        return this;
    }

    /**
     * @return The identifier of this KeyDelegate used in cooldown
     */
    public Object getIdentifier() {
        if (identifier == null) {
            identifier = createID();
        }
        return identifier;
    }

}
