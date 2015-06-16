package cn.academy.vanilla;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.registry.CategoryRegistration.RegCategory;
import cn.academy.core.registry.RegACKeyHandler;
import cn.academy.vanilla.electromaster.CatElectroMaster;
import cn.academy.vanilla.electromaster.item.ItemCoin;
import cn.academy.vanilla.teleporter.entity.EntityTPMarking;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegItem;
import cn.liutils.util.helper.KeyHandler;

@Registrant
@RegInit
public class ModuleVanilla {
	
	@RegItem
	@RegItem.HasRender
	public static ItemCoin coin;
	
	@RegCategory
	public static CatElectroMaster electroMaster;
	
	@RegACKeyHandler(name = "FFF", defaultKey = Keyboard.KEY_MINUS)
	public static KeyHandler test = new Test();

	public static void init() {}
	
	private static class Test extends KeyHandler {
		
		EntityTPMarking mark;
		
		@Override
		public void onKeyDown() {
			mark = new EntityTPMarking(getPlayer()) {

				@Override
				protected double getMaxDistance() {
					return 20;
				}
				
			};
			getPlayer().worldObj.spawnEntityInWorld(mark);
		}
		
		@Override
		public void onKeyUp() {
			if(mark != null)
				mark.setDead();
		}
	}
	
}
