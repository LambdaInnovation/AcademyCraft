/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Project;

import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.internal.PatternDown;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 传说中的超电磁炮~~！
 * TODO 施工中
 * @author WeathFolD
 */
@RegistrationClass
public class SkillRailgun extends SkillBase {

	public SkillRailgun() {
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public boolean onKeyDown(EntityPlayer player) {
				if(!player.worldObj.isRemote) {
					player.worldObj.spawnEntityInWorld(new EntityRailgun(player));
				}
				return true;
			}
			
		});
	}
	
	public String getInternalName() {
		return "em_railgun";
	}
	
	public int getMaxSkillLevel() {
		return 200;
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_RAILGUN;
	}

}
