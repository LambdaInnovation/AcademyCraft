package cn.academy.ability.api.context;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * {@link KeyDelegate} is provided by a skill and handles key delegation of ONE key.
 * @author WeAthFolD
 */
public abstract class KeyDelegate {
    
    private Object identifier;

    public void onKeyDown() {}

    public void onKeyUp() {}

    public void onKeyAbort() {}

    public void onKeyTick() {}

    @SideOnly(Side.CLIENT)
    protected final Minecraft getMC() {
        return Minecraft.getMinecraft();
    }

    @SideOnly(Side.CLIENT)
    protected final EntityPlayer getPlayer() {
        return getMC().thePlayer;
    }

    /**
     * @return The icon displayed in the key hint UI.
     */
    public abstract ResourceLocation getIcon();

    /**
     * @return The identifier used in cooldown
     */
    protected Object createID() {
        return this;
    }

    /**
     * @return The identifier of this KeyDelegate used in cooldown
     */
    public final Object getIdentifier() {
        if (identifier == null) {
            identifier = createID();
        }
        return identifier;
    }

}
