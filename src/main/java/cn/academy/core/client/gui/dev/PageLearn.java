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
package cn.academy.core.client.gui.dev;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.block.dev.IDevAction;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.util.ACUtils;
import cn.liutils.api.gui.widget.RandBufProgressBar;
import cn.liutils.api.gui.widget.StateButton;
import cn.liutils.util.ExpUtils;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.misc.Pair;

/**
 * Learn ability/Upgrade level page.
 * @author WeathFolD
 */
public class PageLearn extends DevSubpage {
	
	boolean isFirst, isMaxLevel;
	IDevAction ida;
	
	private static final ResourceLocation TEXTURE = ACClientProps.TEX_GUI_AD_LEARNING;	

	public PageLearn(GuiDeveloper parent) {
		super(parent, "page.adlearning", TEXTURE);
		isFirst = base.data.getCategoryID() == 0;
		isMaxLevel = base.data.getLevelID() == base.data.getLevelCount() - 1;
		ida = TileDeveloper.getAction(isFirst ? TileDeveloper.ID_DEVELOP : TileDeveloper.ID_LEVEL_UPGRADE, 
				base.data.getLevelID() + 1);
	}
	
	@Override
	public void onAdded() {
		addWidget(new StateButton(34F, 26F, 61.5F, 13.5F, 
				TEXTURE, 123, 27, new double[][] {
				{1, 448},
				{1, 419},
				{1, 477}
			}) {
			{
				if(!base.data.canUpgradeLevel()) {
					this.setInvalid(true); //Unable to upgrade
				}
				this.setFont(ACClientProps.font());
				this.setText(isFirst ? ACLangs.learnAbility() : ACLangs.upgradeLevel());
				this.setTextData(6, new int[][] {
					base.DEFAULT_COLOR,
					base.DEFAULT_COLOR,
					{150, 150, 150, 255}
				});
			}
			
			@Override
			public void buttonPressed(double mx, double my) {
				if(isFirst) {
					base.getGui().addWidget(new DiagActionConfirm(base, TileDeveloper.ID_DEVELOP, 0));
				} else {
					base.getGui().addWidget(new DiagActionConfirm(base, TileDeveloper.ID_LEVEL_UPGRADE, base.data.getLevelID() + 1));
				}
			}
		});
		
		addWidget(new RandBufProgressBar(8.5, 112.5, 122, 5.5, TEXTURE, 17, 293, 244, 11) {
			{
				this.fluctRegion = 0;
			}
			@Override
			public double getProgress() {
				return base.dev.curEnergy / base.dev.getMaxEnergy();
			}
		});
	}

	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		GL11.glPushMatrix(); {
			RenderUtils.bindColor(base.DEFAULT_COLOR);
			//Machine stat
			ACUtils.drawText(ACLangs.machineStat(), 6, 100.5, 7.5);
			
			//Current Energy
			ACUtils.drawText(
				String.format("%s: %.0f/%.0f EU", ACLangs.curEnergy(), base.dev.curEnergy, base.dev.getMaxEnergy()), 
				6, 121, 6);
			//Sync Rate
			ACUtils.drawText(String.format("%s: %.2f%%", ACLangs.devSyncRate(), base.dev.getSyncRateForDisplay()), 
				6, 130, 6);
			RenderUtils.bindIdentity();
			
			//Numeric indication
			if(isFirst || !isMaxLevel) {
				RenderUtils.bindColor(base.DEFAULT_COLOR);
				ACUtils.drawText(ACLangs.expConsumption(), 56, 50, 5.6);
				
				Pair<Integer, Double> exp = base.dev.getExpectation(ida, base.data);
				
				RenderUtils.bindColor(base.EXP_INDI_COLOR);
				ACUtils.drawText(String.format("%d (lv%d)", exp.first, ExpUtils.getLevel(exp.first)), 70, 59, 5.8F);
				
				RenderUtils.bindColor(base.EU_INDI_COLOR);
				ACUtils.drawText(String.format("%.0f", exp.second), 70, 67, 5.8F);
				
				RenderUtils.loadTexture(TEXTURE);
				RenderUtils.bindIdentity();
				HudUtils.drawRect(57, 61, 374, 26, 8.5, 12, 17, 24);
			}
		} GL11.glPopMatrix();
		
		RenderUtils.loadTexture(TEXTURE);
	}
	
	static final int[] BUTTON_COLOR = {120, 206, 255};

}
