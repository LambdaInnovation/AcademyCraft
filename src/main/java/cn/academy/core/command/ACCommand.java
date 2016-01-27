/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.command;

import cn.lambdalib.template.command.LICommandBase;
import net.minecraft.command.ICommandSender;

/**
 * @author WeAthFolD
 */
public abstract class ACCommand extends LICommandBase {
    
    final String commandName;
    protected String localName;

    public ACCommand(String name) {
        localName = commandName = name;
    }
    
    public String locInvalid() {
        return "ac.command.invalid";
    }
    
    public String locSuccessful() {
        return "ac.command.successful";
    }
    
    public String locNotLearned() {
        return "ac.command.notlearned";
    }
    
    public String locNoPlayer() {
        return "ac.command.noplayer";
    }
    
    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public String getCommandUsage(ICommandSender ics) {
        return getLoc("usage");
    }
    
    protected String getLoc(String s) {
        return "ac.command." + localName + "." + s;
    }
}
