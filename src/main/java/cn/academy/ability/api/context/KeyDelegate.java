/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.context;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.Skill;
import cn.lambdalib.util.helper.Color;
import cpw.mods.fml.common.registry.RegistryDelegate.Delegate;
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

    public enum DelegateState {
        IDLE(0.7f, 0x00000000, false),
        CHARGE(1.0f, 0xffffad37, true),
        ACTIVE(1.0f, 0xff46b3ff, true);

        public final float alpha;
        public final Color glowColor;
        public final boolean sinEffect;

        DelegateState(float _alpha, int _glowColor, boolean _sinEffect) {
            alpha = _alpha;
            glowColor = new Color(_glowColor);
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
        return getMC().thePlayer;
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
