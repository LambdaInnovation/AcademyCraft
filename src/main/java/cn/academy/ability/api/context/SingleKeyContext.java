/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.context;

import net.minecraft.entity.player.EntityPlayer;

public class SingleKeyContext extends Context {

    // Key messages for single key context.
    public static final String
        MSG_KEYDOWN = "kdn",
        MSG_KEYUP = "kdu",
        MSG_KEYABORT = "kab";

    public SingleKeyContext(EntityPlayer player) {
        super(player);
    }

}
