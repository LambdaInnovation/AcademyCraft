package cn.academy.ability.api.context;

import net.minecraft.entity.player.EntityPlayer;

public class SkillContext extends Context {

    // Key messages for single key context.
    public static final String
        MSG_KEYDOWN = "kdn",
        MSG_KEYUP = "kdu",
        MSG_KEYABORT = "kab";

    public SkillContext(EntityPlayer player) {
        super(player);
    }

}
