/**
 * 
 */
package cn.academy.core.client.render;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cn.academy.api.client.render.SkillRenderer.HandRenderType;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.SkillStateManager;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.template.LIClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The generic render pipeline of SkillRender. 
 * Multiple events are handled here, 
 * while multiple rendering routines sharing same util/drawing functions.
 * @author WeathFolD
 */
@RegistrationClass
@RegSubmoduleInit(side = RegSubmoduleInit.Side.CLIENT_ONLY)
@SideOnly(Side.CLIENT)
public class SkillRenderingHandler {
	
	private static SkillRenderingHandler instance = new SkillRenderingHandler();
	
	public static void init() {
		LIClientRegistry.addPlayerRenderingHook(new PRHSkillRender());
		MinecraftForge.EVENT_BUS.register(instance);
	}
	
	@SubscribeEvent
	public void renderHudEvent(RenderGameOverlayEvent e) {
		ScaledResolution sr = e.resolution;
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		for(SkillState ss : SkillStateManager.getState(player)) {
			ss.getRender().renderHud(player, ss, sr);
		}
	}
	
	public static void renderThirdPerson(EntityLivingBase ent, ItemStack stack, ItemRenderType type) {
		if(type == ItemRenderType.EQUIPPED_FIRST_PERSON || !(ent instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer) ent;
		Item item = stack.getItem();
		Block block = Block.getBlockFromItem(stack.getItem());
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix(); {
			
			GL11.glColor4d(1, 0.6, 0.6, 0.55);
			
			if(stack.getItemSpriteNumber() == 0 && item instanceof ItemBlock) { //block render routine
				GL11.glTranslated(0, 0, 1.5);
				GL11.glScalef(2F, 2F, 2F);
				GL11.glRotated(45, 1, 0, 1);
				GL11.glRotated(45, 0, 1, 0);
				GL11.glRotated(180, 1, 0, 0);
			} else if(item.isFull3D()) {
				GL11.glTranslated(0.1, 0.8, -.4);
				GL11.glScalef(.8F, .8F, .8F);
				GL11.glRotated(45, 0, 0, -1);
			} else {
				GL11.glTranslated(-.3, 1.2, -.6);
				GL11.glRotatef(90, 1, 0, 0);
				GL11.glScalef(1.5F, 1.5F, 1.5F);
			}
			
			traverseHandRender(player, HandRenderType.EQUIPPED);
			
		} GL11.glPopMatrix();
	}
	
	public static void renderFirstPerson() {
		//System.out.println("rfp");
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix(); {
			GL11.glTranslated(0.28, -0.55, -.9);
			GL11.glColor4d(1, 0.6, 0.6, 0.55);
			GL11.glScalef(0.66F, 0.66F, 0.66F);
			GL11.glRotatef(20, 1, 0, 0);

			//RenderUtils.loadTexture(ACClientProps.TEX_ARC_SHELL[0]);
			//RenderUtils.drawCube(1, 1, 1, false);
			traverseHandRender(Minecraft.getMinecraft().thePlayer, HandRenderType.FIRSTPERSON);
			
		} GL11.glPopMatrix();
	}
	
	private static void traverseHandRender(EntityPlayer player, HandRenderType type) {
		List<SkillState> states = SkillStateManager.getState(player);
		for(SkillState s : states) {
			s.getRender().renderHandEffect(player, s, type);
		}
	}
	
	private static Vec3 vec(double x, double y, double z) {
		return Vec3.createVectorHelper(x, y, z);
	}
	
}
