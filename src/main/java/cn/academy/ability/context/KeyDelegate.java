package cn.academy.ability.context;

import cn.academy.ability.Skill;
import cn.lambdalib2.util.Colors;
//import net.minecraftforge.fml.common.registry.RegistryDelegate.Delegate;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.Color;

/**
 * {@link KeyDelegate} is provided by a skill and handles key delegation of ONE key.
 * @author WeAthFolD
 */
public abstract class KeyDelegate {

    public enum DelegateState {
        IDLE(0.7f, 0x00000000, false),
        CHARGE(1.0f, 0xffffad37, true),
        ACTIVE(1.0f, 0xff46b3ff, true);

        public final float alpha;
        public final Color glowColor;
        public final boolean sinEffect;

        DelegateState(float _alpha, int _glowColor, boolean _sinEffect) {
            alpha = _alpha;
            glowColor = Colors.fromHexColor(_glowColor);
            sinEffect = _sinEffect;
        }
    }

    private Integer identifier = null;

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