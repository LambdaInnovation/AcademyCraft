/**
 * 
 */
package cn.academy.core.client.gui.dev;

import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.gui.Widget;


/**
 * @author WeathFolD
 */
public class PageMainOrdinary extends PageMainBase {

	public PageMainOrdinary(GuiDeveloper gd) {
		super(gd);
		
		new Widget("last", this, 88.5, 6.5, 8.5, 7.5) {
			@Override
			public void onMouseDown(double mx, double my) {
				dev.pageID = Math.max(dev.pageID - 1, 0);
				dev.updateVisiblility();
			}
		};
		
		new Widget("next", this, 215.5, 6.5, 8.5, 7.5) {
			@Override
			public void onMouseDown(double mx, double my) {
				dev.pageID = Math.min(dev.pageID + 1, dev.subs.size() - 1);
				dev.updateVisiblility();
			}
		};
	}
	

}
