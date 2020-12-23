package cn.academy.util;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

/**
 * @author WeAthFolD
 */
public abstract class ACCommand extends CommandBase  {
    
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
    public String getName() {
        return commandName;
    }

    @Override
    public String getUsage(ICommandSender ics) {
        return getLoc("usage");
    }
    
    protected String getLoc(String s) {
        return "ac.command." + localName + "." + s;
    }
}