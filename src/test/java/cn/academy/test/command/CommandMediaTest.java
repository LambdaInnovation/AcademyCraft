/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.test.command;

import cn.academy.core.command.ACCommand;
import cn.academy.misc.media.ACMedia;
import cn.academy.misc.media.MediaManager;
import cn.academy.misc.media.MediaUtils;
import cn.academy.misc.media.OnlineMediaManager;
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
                MediaUtils.playMedia(MediaUtils.getMedia(pars[1]), false);
                break;
            case "pause":
                MediaUtils.pauseMedia(MediaUtils.getMedia(pars[1]));
                break;
            case "volume":
                MediaUtils.setMediaVolume(MediaUtils.getMedia(pars[1]), Float.valueOf(pars[2]));
                break;
            case "stop":
                MediaUtils.stopMedia(MediaUtils.getMedia(pars[1]));
                break;
            case "medias":
                ics.addChatMessage(new ChatComponentTranslation(MediaUtils.getAllIds().toString()));
                break;
            case "download":
                OnlineMediaManager.INSTANCE.downloadMedia(OnlineMediaManager.INSTANCE.getMedia(pars[1]));
                break;
            case "remove":
                OnlineMediaManager.INSTANCE.removeLocalMedia(OnlineMediaManager.INSTANCE.getMedia(pars[1]));
                break;
            case "info":
                ACMedia m = MediaUtils.getMedia(pars[1]);
                ics.addChatMessage(new ChatComponentTranslation("====AcademyCraft Media System===="));
                ics.addChatMessage(new ChatComponentTranslation("Author: " + m.getAuthor()));
                ics.addChatMessage(new ChatComponentTranslation("Name: " + m.getName()));
                ics.addChatMessage(new ChatComponentTranslation("File: " + m.getFile().getPath()));
                ics.addChatMessage(new ChatComponentTranslation("Remark: " + m.getRemark()));
                ics.addChatMessage(new ChatComponentTranslation("Cover Picture: " + m.getCoverPic().getPath()));
                ics.addChatMessage(new ChatComponentTranslation("================================="));
        }
    }

}
