/**
 * 
 */
package cn.academy.core.event;

import java.lang.reflect.Field;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraftforge.client.event.RenderPlayerEvent;
import cn.academy.core.block.dev.DevPlayerManip;
import cn.academy.core.client.render.ACModelBiped;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 */
@SideOnly(Side.CLIENT)
@RegistrationClass
@RegEventHandler(RegEventHandler.Bus.Forge)
public class ClientEvents {
	
	ModelBiped hackMain, hackArmor, hackChestplate, hackModel;
	ModelBiped realMain, realArmor, realChestplate, realModel;
	
	private static Field fldmain;
	static {
		try {
			fldmain = RendererLivingEntity.class.getDeclaredField("mainModel");
			fldmain.setAccessible(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	} 

	public ClientEvents() {
		hackMain = hackModel = new ACModelBiped(0.0F);
		hackChestplate = new ACModelBiped(1.0F);
		hackArmor = new ACModelBiped(0.5F);
	}
	
	@SubscribeEvent
	public void startRender(RenderPlayerEvent.Pre event) {
		RenderPlayer render = event.renderer;
		//Init last models
		if(realMain == null) {
			realMain = render.modelBipedMain;
			realArmor = render.modelArmor;
			realChestplate = render.modelArmorChestplate;
			realModel = getMainModel(render);
		}
		//System.out.println(realMain);
		
		//Replace
		if(DevPlayerManip.isDeveloping(event.entityPlayer)) {
			render.modelBipedMain = hackMain;
			render.modelArmor = hackArmor;
			render.modelArmorChestplate = hackChestplate;
			setMainModel(render, hackModel);
		}
		//System.out.println("start");
	}
	
	@SubscribeEvent
	public void endRender(RenderPlayerEvent.Post event) {
		//Restore last models
		//System.out.println("end");
		RenderPlayer render = event.renderer;
		render.modelBipedMain = realMain;
		render.modelArmor = realArmor;
		render.modelArmorChestplate = realChestplate;
		setMainModel(render, realModel);
	}
	
	private void setMainModel(RenderPlayer r, ModelBiped m) {
		try {
			fldmain.set(r, m);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private ModelBiped getMainModel(RenderPlayer r) {
		try {
			return (ModelBiped) fldmain.get(r);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
