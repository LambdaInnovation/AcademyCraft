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
package cn.academy.core.event;

import java.lang.reflect.Field;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraftforge.client.event.RenderPlayerEvent;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.client.render.ACModelBiped;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cn.liutils.util.RegUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * TODO: Rewrite to adapt to obfuscation, or completely find a new method
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
			fldmain = RegUtils.getObfField(RendererLivingEntity.class, "mainModel", "field_77045_g");
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
		
		//Replace
		if(event.entityPlayer.ridingEntity instanceof TileDeveloper.SitEntity) {
			render.modelBipedMain = hackMain;
			render.modelArmor = hackArmor;
			render.modelArmorChestplate = hackChestplate;
			setMainModel(render, hackModel);
		}
	}
	
	@SubscribeEvent
	public void endRender(RenderPlayerEvent.Post event) {
		//Restore last models
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
