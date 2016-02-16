/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.test.command;

import cn.academy.core.command.ACCommand;
import cn.academy.misc.media.MediaManager;
import cn.academy.misc.media.MediaUtils;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegCommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;

/**
 * @author KSkun
 */
@Registrant
@RegCommand
public class CommandMediaTest extends ACCommand {

    public CommandMediaTest() {
        super("acmedia");
    }

    @Override
    public void processCommand(ICommandSender ics, String[] pars) {
        switch(pars[0]) {
            case "play":
                MediaUtils.playMedia(MediaManager.INSTANCE.getMedia(pars[1]), false);
                break;
            case "pause":
                MediaUtils.pauseMedia(MediaManager.INSTANCE.getMedia(pars[1]));
                break;
            case "volume":
                MediaUtils.setMediaVolume(MediaManager.INSTANCE.getMedia(pars[1]), Float.valueOf(pars[2]));
                break;
            case "stop":
                MediaUtils.stopMedia(MediaManager.INSTANCE.getMedia(pars[1]));
                break;
            case "rewind":
                MediaUtils.rewindMedia(MediaManager.INSTANCE.getMedia(pars[1]));
                break;
            case "medias":
                ics.addChatMessage(new ChatComponentTranslation(MediaManager.INSTANCE.getMediaIds().toString()));
                break;
        }
    }

}
