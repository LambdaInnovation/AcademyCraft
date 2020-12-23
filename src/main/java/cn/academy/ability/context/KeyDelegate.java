package cn.academy.ability.context;

import cn.academy.ability.Skill;
//import net.minecraftforge.fml.common.registry.RegistryDelegate.Delegate;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * {@link KeyDelegate} is provided by a skill and handles key delegation of ONE key.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public abstract class KeyDelegate {

    private Integer identifier = null;

    public void onKeyDown() {}

    public void onKeyUp() {}

    public void onKeyAbort() {}

    public void onKeyTick() {}

    protected final Minecraft getMC() {
        return Minecraft.getMinecraft();
    }

    protected final EntityPlayer getPlayer() {
        return getMC().player;
    }

    /**
     * @return The icon displayed in the key hint UI.
     */
    public abstract ResourceLocation getIcon();

    /**
     * @return The skill subID used in cooldown
     */
    public abstract int createID();

    /**
     * @return The skill that this delegate belongs to
     */
    public abstract Skill getSkill();

    /**
     * @return The identifier of this KeyDelegate used in cooldown
     */
    public final Integer getIdentifier() {
        if (identifier == null) {
            identifier = createID();
        }
        return identifier;
    }

    public DelegateState getState() {
        return DelegateState.IDLE;
    }

}