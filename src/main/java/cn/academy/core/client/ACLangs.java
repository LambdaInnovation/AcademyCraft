/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client;

import net.minecraft.util.StatCollector;

/**
 * Generic language translations
 * @author WeathFolD
 */
public class ACLangs {
	
	public static String devNewAbility() {
		return local("ac.devnew");
	}
	
	public static String selectChannel() {
		return local("ac.selchannel");
	}
	
	public static String ssidnotnull() {
		//输入的ssid不能为空
		return local("ac.ssidnn");
	}
	
	public static String frClearConfirm() {
		//你确认要清除当前机器的连接么？
		return local("ac.frclearconfirm");
	}
	
	public static String frEstbConn() { //Establish Connection
		return local("ac.frestb");
	}
	
	public static String frClearConn() {
		return local("ac.frclear");
	}
	
	public static String frCurrentChannel() {
		return local("ac.frcur");
	}
	
	public static String frChannelSelect() {
		return local("ac.frselect");
	}
	
	public static String frSelectedChannel() {
		return local("ac.frchannel");
	}
	
	public static String freqReg() {
		return local("ac.freqreg");
	}
	
	public static String matChangePwd() {
		return local("ac.matpwd");
	}
	
	public static String wirelessLogin() {
		return local("ac.wllogin");
	}
	
	public static String opFailed() {
		return local("ac.opfail");
	}
	
	public static String presetSelect() {
		return local("ac.presselect");
	}
	
	public static String opSuccessful() {
		return local("ac.opsuc");
	}
	
	public static String channelExists() {
		return local("ac.cnnexist");
	}
	
	public static String opStatus() {
		return local("ac.opstat");
	}
	
	public static String transmitting() {
		return local("ac.transmitting");
	}
	
	public static String inconsistentPass() {
		return local("ac.inconsistentpass");
	}
	
	public static String loading() {
		return local("ac.loading");
	}
	
	public static String notConnected() {
		return local("ac.notconnected");
	}

	public static String presetPrefix() {
		return local("ac.preset");
	}
	
	public static String notLearned() {
		return local("ac.notlearned");
	}
	
	public static String devSyncRate() {
		return local("ac.syncrate");
	}
	
	public static String learnAbility() {
		return local("ac.learnability");
	}
	
	public static String upgradeLevel() {
		return local("ac.upgradelv");
	}
	
	public static String machineStat() {
		return local("ac.machinestat");
	}
	
	public static String curEnergy() {
		return local("ac.curenergy");
	}
	
	public static String presetSettings() {
		return local("ac.presset");
	}
	
	public static String pgLearn() {
		return local("page.adlearning");
	}
	
	public static String pgSkills() {
		return local("page.adskills");
	}
	
	public static String actionConfirm() {
		return local("page.adconfirm");
	}
	
	public static String confirm() {
		return local("ac.btnconfirm");
	}
	
	public static String cancel() {
		return local("ac.btncancel");
	}
	
	public static String upgradeTo() {
		return local("ac.upgradeto");
	}
	
	public static String confirmHead() {
		return local("ac.confirm.head");
	}
	
	public static String confirmTail() {
		return local("ac.confirm.tail");
	}
	
	public static String learnSkill() {
		return local("ac.learnskill");
	}
	
	public static String upgradeSkill() {
		return local("ac.upgradeskill");
	}
	
	public static String stimProg() {
		return local("ac.stimprog");
	}
	
	public static String curAction() {
		return local("ac.curaction");
	}
	
	public static String attemptes() {
		return local("ac.attempts");
	}
	
	public static String fails() {
		return local("ac.fails");
	}
	
	public static String aborted() {
		return local("ac.aborted");
	}
	
	public static String successful() {
		return local("ac.successful");
	}
	
	public static String expConsumption() {
		return local("ac.expconsumption");
	}
	
	public static String fullyLearned() {
		return local("ac.fullylearned");
	}
	
	public static String holoView() {
		return local("adev.holoview");
	}
	
	public static String ad_UserInfo() {
		return local("adev.userinfo");
	}
	
	public static String chooseSkill() {
		return local("prs.chooseskl");
	}
	
	public static String matInit() {
		return local("ac.matinit");
	}
	
	private static String local(String str) {
		return StatCollector.translateToLocal(str);
	}

}
