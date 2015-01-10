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
import cn.liutils.util.render.TextUtils;

/**
 * @author WeathFolD
 *
 */
public class PageLearn extends DevSubpage {
	
	boolean isFirst, isMaxLevel;
	IDevAction ida;

	public PageLearn(PageMainBase parent) {
		super(parent, "page.adlearning", ACClientProps.TEX_GUI_AD_LEARNING);
		isFirst = dev.data.getCategoryID() == 0;
		isMaxLevel = dev.data.getLevelID() == dev.data.getLevelCount() - 1;
		ida = TileDeveloper.getAction(isFirst ? TileDeveloper.ID_DEVELOP : TileDeveloper.ID_LEVEL_UPGRADE, dev.data.getLevelID() + 1);
		
		TextButton btn = new TextButton("btn_learn", this, 34F, 26F, 61.5F, 13.5F) {
			{
				setTexMapping(1, 448, 123, 27);
				setDownMapping(1, 419);
				setInvalidMapping(1, 477);
				if(!isFirst && isMaxLevel) {
					this.receiveEvent = false; //Unable to upgrade
				}
				setTexture(ACClientProps.TEX_GUI_AD_LEARNING, 512, 512);
				setTextProps(ACLangs.learnAbility(), 8);
				this.setTextColor(dev.DEFAULT_COLOR);
				this.setActiveColor(dev.DEFAULT_COLOR);
				this.setInactiveColor(150, 150, 150, 255);
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				if(isFirst) {
					new DiagActionConfirm(dev, TileDeveloper.ID_DEVELOP, 0);
				} else {
					new DiagActionConfirm(dev, TileDeveloper.ID_LEVEL_UPGRADE, dev.data.getLevelID() + 1);
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
				return dev.dev.curEnergy / dev.dev.getMaxEnergy();
			}
		};
		
	}

	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		GL11.glPushMatrix(); {
			RenderUtils.bindColor(dev.DEFAULT_COLOR);
			//Machine stat
			String str = ACLangs.machineStat();
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 100.5, 9);
			//Current Energy
			str = String.format("%s: %.0f/%.0f EU", ACLangs.curEnergy(), dev.dev.curEnergy, dev.dev.getMaxEnergy());
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 121, 8);
			//Sync Rate
			str = String.format("%s: %.2f%%", ACLangs.devSyncRate(), dev.dev.getSyncRateForDisplay());
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 6, 129, 8);
			GL11.glColor4f(1, 1, 1, 1);
			
			//Numeric indication
			
			if(isFirst || !isMaxLevel) {
				RenderUtils.bindColor(dev.DEFAULT_COLOR);
				str = ACLangs.expConsumption();
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 55, 50, 9);
				
				int eexp = dev.dev.getExpConsume() * ida.getExpectedStims(dev.data);
				double eene = dev.dev.getEUConsume() * ida.getExpectedStims(dev.data);
				dev.dev.action = ida;
				double prob = dev.dev.getSuccessProb(dev.data);
				eexp = (int) (eexp / prob);
				eene /= prob;
				
				RenderUtils.bindColor(dev.EU_INDI_COLOR);
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, String.format("%.0f", eene), 70, 67, 7.5F);
				
				RenderUtils.bindColor(dev.EXP_INDI_COLOR);
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, String.format("%d", eexp), 70, 59, 7.5F);
				
				RenderUtils.loadTexture(ACClientProps.TEX_GUI_AD_LEARNING);
				GL11.glColor4f(1, 1, 1, 1);
				HudUtils.drawRect(57, 61, 374, 26, 8.5, 12, 17, 24);
			}
		} GL11.glPopMatrix();
	}
	
	static final int[] BUTTON_COLOR = {120, 206, 255};

}
