/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
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
