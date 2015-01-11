/**
 * 
 */
package cn.academy.core.client.gui.dev;

import org.lwjgl.opengl.GL11;

import cn.academy.core.block.dev.IDevAction;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.gui.widget.RandBufProgressBar;
import cn.liutils.api.gui.widget.TextButton;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.misc.Pair;
import cn.liutils.util.render.TextUtils;

/**
 * Learn ability/Upgrade level page.
 * @author WeathFolD
 */
public class PageLearn extends DevSubpage {
	
	boolean isFirst, isMaxLevel;
	IDevAction ida;

	public PageLearn(PageMain parent) {
		super(parent, "page.adlearning", ACClientProps.TEX_GUI_AD_LEARNING);
		isFirst = base.data.getCategoryID() == 0;
		isMaxLevel = base.data.getLevelID() == base.data.getLevelCount() - 1;
		ida = TileDeveloper.getAction(isFirst ? TileDeveloper.ID_DEVELOP : TileDeveloper.ID_LEVEL_UPGRADE, base.data.getLevelID() + 1);
		
		TextButton btn = new TextButton("btn_learn", this, 34F, 26F, 61.5F, 13.5F) {
			{
				setTexMapping(1, 448, 123, 27);
				setDownMapping(1, 419);
				setInvalidMapping(1, 477);
				if(!isFirst && isMaxLevel) {
					this.receiveEvent = false; //Unable to upgrade
				}
				setTexture(ACClientProps.TEX_GUI_AD_LEARNING, 512, 512);
				setTextProps(isFirst ? ACLangs.learnAbility() : ACLangs.upgradeLevel(), 8);
				this.setTextColor(base.DEFAULT_COLOR);
				this.setActiveColor(base.DEFAULT_COLOR);
				this.setInactiveColor(150, 150, 150, 255);
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				if(isFirst) {
					new DiagActionConfirm(base, TileDeveloper.ID_DEVELOP, 0);
				} else {
					new DiagActionConfirm(base, TileDeveloper.ID_LEVEL_UPGRADE, base.data.getLevelID() + 1);
				}
			}
		};
		
		new RandBufProgressBar("energybar", this, 8.5, 112.5, 122, 5.5) {
			{
				this.setTexture(ACClientProps.TEX_GUI_AD_LEARNING, 512, 512);
				this.setTexMapping(17, 293, 244, 11);
				this.fluctRegion = 0;
			}
			@Override
			public double getProgress() {
				return base.dev.curEnergy / base.dev.getMaxEnergy();
			}
		};
		
	}

	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		GL11.glPushMatrix(); {
			RenderUtils.bindColor(base.DEFAULT_COLOR);
			//Machine stat
			base.drawText(ACLangs.machineStat(), 6, 100.5, 9);
			
			//Current Energy
			base.drawText(
				String.format("%s: %.0f/%.0f EU", ACLangs.curEnergy(), base.dev.curEnergy, base.dev.getMaxEnergy()), 
				6, 121, 8);
			//Sync Rate
			base.drawText(String.format("%s: %.2f%%", ACLangs.devSyncRate(), base.dev.getSyncRateForDisplay()), 
				6, 129, 8);
			RenderUtils.bindIdentity();
			
			//Numeric indication
			if(isFirst || !isMaxLevel) {
				RenderUtils.bindColor(base.DEFAULT_COLOR);
				base.drawText(ACLangs.expConsumption(), 55, 50, 9);
				
				Pair<Integer, Double> exp = base.dev.getExpectation(ida, base.data);
				
				RenderUtils.bindColor(base.EXP_INDI_COLOR);
				base.drawText(String.format("%d", exp.first), 70, 59, 7.5F);
				
				RenderUtils.bindColor(base.EU_INDI_COLOR);
				base.drawText(String.format("%.0f", exp.second), 70, 67, 7.5F);
				
				RenderUtils.loadTexture(ACClientProps.TEX_GUI_AD_LEARNING);
				RenderUtils.bindIdentity();
				HudUtils.drawRect(57, 61, 374, 26, 8.5, 12, 17, 24);
			}
		} GL11.glPopMatrix();
	}
	
	static final int[] BUTTON_COLOR = {120, 206, 255};

}
