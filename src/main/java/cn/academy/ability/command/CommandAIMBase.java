/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.command;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.cooldown.CooldownData;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.command.ACCommand;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegCommand;
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType;
import cn.lambdalib.util.datapart.PlayerDataTag;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StatCollector;

import java.util.List;

/**
 * @author WeAthFolD
 */
@Registrant
@NetworkS11nType
public abstract class CommandAIMBase extends ACCommand {

    private static final String MSG_CLEAR_COOLDOWN = "clearcd";
    
    /**
     * This is the command used by the client, doesn't specify the player and works on the user.
     * This command will display a warning before you can use it.
     */
    @Registrant
    @RegCommand
    public static class CommandAIM extends CommandAIMBase {
        
        static final String ID = "aim_cheats";

        public CommandAIM() {
            super("aim");
        }
        
        @Override
        public void processCommand(ICommandSender commandSender, String[] pars) {
            EntityPlayer player = super.getCommandSenderAsPlayer(commandSender);

            if(!isActive(player) && player.getEntityWorld().getWorldInfo().areCommandsAllowed()) {
                setActive(player, true);
            }

            if(pars.length == 1) {
                switch(pars[0]) {
                case "cheats_on":
                    setActive(player, true);
                    sendChat(commandSender, locSuccessful());
                    sendChat(commandSender, getLoc("warning"));
                    return;
                case "cheats_off":
                    setActive(player, false);
                    sendChat(commandSender, locSuccessful());
                    return;
                }
            }
            
            if(!isActive(player) && !player.capabilities.isCreativeMode) {
                sendChat(commandSender, getLoc("notactive"));
                return;
            }
            
            if(pars.length == 0) {
                sendChat(commandSender, getLoc("help"));
                return;
            }
            
            matchCommands(commandSender, CommandBase.getCommandSenderAsPlayer(commandSender), pars);
        }
        
        private void setActive(EntityPlayer player, boolean data) {
            PlayerDataTag.get(player).getTag().setBoolean(ID, data);
        }
        
        private boolean isActive(EntityPlayer player) {
            return PlayerDataTag.get(player).getTag().getBoolean(ID);
        }
        
    }
    
    /**
     * This is the command for the OPs and server console. You must specify the player name.
     */
    @Registrant
    @RegCommand
    public static class CommandAIMP extends CommandAIMBase {

        public CommandAIMP() {
            super("aimp");
        }
        
        @Override
        public void processCommand(ICommandSender ics, String[] pars) {
            if(pars.length == 0) {
                sendChat(ics, getLoc("help"));
                return;
            }
            
            //Try to locate the player.
            EntityPlayer player = null;
            
            //Using player parameter
            for(Object p : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                EntityPlayer p2 = (EntityPlayer) p;
                if(p2.getCommandSenderName().equals(pars[0])) {
                    player = p2;
                    break;
                }
            }
            
            if(player != null) {
                String[] newPars = new String[pars.length - 1];
                for(int i = 0; i < newPars.length; ++i) {
                    newPars[i] = pars[i + 1];
                }
                
                matchCommands(ics, player, newPars);
            } else {
                sendChat(ics, locNoPlayer());
            }
        }
        
    }
    
    String[] commands = {
        "help", "cat", "catlist", 
        "learn", "learn_all", "reset",
        "learned", "skills", "fullcp",
        "level", "exp", "cd_clear", "maxout"
    };

    public CommandAIMBase(String name) {
        super(name);
        localName = "aim";
    }
    
    protected void matchCommands(ICommandSender ics, EntityPlayer player, String[] pars) {
        AbilityData aData = AbilityData.get(player);
        switch(pars[0]) {
        case "?":
        case "help": {
            for(String c : commands) {
                sendChat(ics, getLoc(c));
            }
            return;
        }
        
        case "cat": {
            if(pars.length == 1) {
                sendChat(ics, getLoc("curcat"), aData.hasCategory() ?
                        aData.getCategory().getDisplayName() :
                        StatCollector.translateToLocal(getLoc("nonecat")));
                return;
            } else if(pars.length == 2) {
                String catName = pars[1];
                Category cat = CategoryManager.INSTANCE.getCategory(catName);
                if(cat != null) {
                    aData.setCategory(cat);
                    sendChat(ics, locSuccessful());
                } else {
                    sendChat(ics, getLoc("nocat"));
                }
                return;
            }
            break;    
        }
        
        case "catlist": {
            sendChat(ics, getLoc("cats"));
            List<Category> catList = CategoryManager.INSTANCE.getCategories();
            for(int i = 0; i < catList.size(); ++i) {
                Category cat = catList.get(i);
                sendChat(ics, "#" + i + " " + cat.getName() + ": " + cat.getDisplayName());
            }
            return;
        }
        
        case "learn": {
            if (aData.hasCategory()) {
                Skill s = tryParseSkill(aData.getCategory(), pars[1]);
                if(s == null) {
                    sendChat(ics, getLoc("noskill"));
                } else {
                    aData.learnSkill(s);
                }
            } else {
                sendChat(ics, getLoc("nonecathint"));
            }
            return;
        }
        
        case "unlearn": {
            if (aData.hasCategory()) {
                Category cat = aData.getCategory();
                Skill s = tryParseSkill(cat, pars[1]);
                if(s == null) {
                    sendChat(ics, getLoc("noskill"));
                } else {
                    aData.setSkillLearnState(s, false);
                }
            } else {
                sendChat(ics, getLoc("nonecathint"));
            }
            return;
        }
        
        case "learn_all": {
            if (aData.hasCategory()) {
                aData.learnAllSkills();
                sendChat(ics, locSuccessful());
            } else {
                sendChat(ics, getLoc("nonecathint"));
            }
            return;
        }
        
        case "reset": {
            aData.setCategory(null);
            sendChat(ics, locSuccessful());
            return;
        }
        
        case "learned": {
            StringBuilder sb = new StringBuilder();
            
            boolean begin = true;
            for(Skill s : aData.getLearnedSkillList()) {
                sb.append(begin ? "" : ", " + s.getName());
                begin = false;
            }
            
            sendChat(ics, getLoc("learned.format"), sb.toString());
            return;
        }
        
        case "skills": {
            if (aData.hasCategory()) {
                Category cat = aData.getCategory();
                for(Skill s : cat.getSkillList()) {
                    sendChat(ics, "#" + s.getID() + " " + s.getName() + ": " + s.getDisplayName());
                }
            } else {
                sendChat(ics, getLoc("nonecathint"));
            }
            return;
        }
        
        case "level": {
            
            if(pars.length == 1) {
                sendChat(ics, "" + aData.getLevel());
            } else {
                int lv = Integer.valueOf(pars[1]);
                if(lv > 0 && lv <= 5) {
                    aData.setLevel(lv);
                    sendChat(ics, locSuccessful());
                } else {
                    sendChat(ics, this.getLoc("outofrange"), 1, 5);
                }
            }
            
            return;
        }
        
        case "fullcp": {

            if (aData.hasCategory()) {
                CPData cpData = CPData.get(player);
                cpData.setCP(cpData.getMaxCP());
                cpData.setOverload(0);
                sendChat(ics, locSuccessful());
                return;
            } else {
                sendChat(ics, getLoc("nonecathint"));
            }
            return;
        }
        
        case "exp": {

            if (aData.hasCategory()) {
                Category cat = aData.getCategory();

                if (pars.length == 1) {
                    sendChat(ics, this.locInvalid());
                } else {
                    Skill skill = tryParseSkill(cat, pars[1]);
                    if(skill == null) {
                        sendChat(ics, getLoc("noskill"));
                    } else {
                        if(pars.length == 2) {
                            sendChat(ics, this.getLoc("curexp"), skill.getDisplayName(), aData.getSkillExp(skill) * 100);
                        } else if(pars.length == 3) {
                            Float exp = tryParseFloat(pars[2]);
                            if(exp < 0 || exp > 1) {
                                sendChat(ics, this.getLoc("outofrange"), 0.0f, 1.0f);
                            } else {
                                aData.setSkillExp(skill, exp);
                                sendChat(ics, this.locSuccessful());
                            }
                        } else {
                            sendChat(ics, this.locInvalid());
                        }
                    }
                }
            } else {
                sendChat(ics, getLoc("nonecathint"));
            }
            return;
        }

        case "cd_clear": {

            if (aData.hasCategory()) {
                CooldownData.of(player).clear();
                sendChat(ics, locSuccessful());
                return;
            } else {
                sendChat(ics, getLoc("nonecathint"));
            }
            return;
        }

        case "maxout": {

            if (aData.hasCategory()) {
                aData.maxOutLevelProgress();
                sendChat(ics, locSuccessful());
                return;
            } else {
                sendChat(ics, getLoc("nonecathint"));
            }
            return;
        }

        default: {
            sendChat(ics, getLoc("nocomm"));
            return;
        }
        }
    }
    
    private Integer tryParseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch(NumberFormatException e) {
            return null;
        }
    }
    
    private Float tryParseFloat(String str) {
        try {
            return Float.parseFloat(str);
        } catch(NumberFormatException e) {
            return null;
        }
    }
    
    private Skill tryParseSkill(Category cat, String str) {
        if(cat == null)
            return null;
        Integer i = tryParseInt(str);
        if(i != null)
            return cat.getSkill(i);
        return cat.getSkill(str);
    }

}
