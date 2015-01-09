package cn.academy.core.client.gui;

import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.api.ctrl.Preset;
import cn.academy.api.ctrl.PresetManager;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.proxy.ACCommonProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.key.IKeyHandler;
import cn.liutils.api.key.LIKeyProcess;
import cn.liutils.api.register.Configurable;
import cn.liutils.registry.AttachKeyHandlerRegistry.RegAttachKeyHandler;
import cn.liutils.registry.ConfigurableRegistry.RegConfigurable;
import cn.liutils.util.ClientUtils;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.TextUtils;

/**
 * @author WeAthFolD
 *
 */
@RegistrationClass
@RegConfigurable
@SideOnly(Side.CLIENT)
public class GuiPresetSettings extends LIGuiScreen {

	@Configurable(category = "Control", key = "keyPresetSettings", defValueInt = Keyboard.KEY_N)
	@RegAttachKeyHandler(clazz = KeyHandler.class)
	public static int KEY_ID_PRESET_SETTINGS;
	
	public static class KeyHandler implements IKeyHandler {
		@Override
		public void onKeyDown(int keyCode, boolean tickEnd) {
			if(tickEnd || !ClientUtils.isPlayerInGame()) return;
			Minecraft mc = Minecraft.getMinecraft();
			guiHandler.openClientGui();
		}
		@Override public void onKeyUp(int keyCode, boolean tickEnd) {}
		@Override public void onKeyTick(int keyCode, boolean tickEnd) {}
	}
	
	@RegGuiHandler
	public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
		@SideOnly(Side.CLIENT)
		protected GuiScreen getClientGui() {
			return new GuiPresetSettings();
		}
	};
	
	private boolean isSetting;

	private int currentPage;
	private static final int MAX_PAGE = PresetManager.PRESET_COUNT;
	
	private Preset tempPreset; //Used for current setting
	private boolean isModifying;
	private int modKey;
	private PageMain pageMain;
	
	public GuiPresetSettings() {
		super();
		tempPreset = PresetManager.getCurrentPreset().clone();
		pageMain = new PageMain();
	}
	
	private class PageMain extends Widget {
		private static final float 
			RATIO = 1.75F, 
			HEIGHT = 80, 
			WIDTH = HEIGHT * RATIO,
			PAGE_STEP = 16;
		
		private class PartPageSel extends Widget {
			
			int id;

			public PartPageSel(int _id, float x) {
				super("sel" + _id, PageMain.this, x, 0, PAGE_STEP, HEIGHT / 6);
				id = _id;
				draw = true;
			}
			
			@Override
			public void draw(double mx, double my, boolean mouseHovering) {
				boolean draw = false;
				float color = 0F;
				if(id == currentPage) {
					draw = true;
					color = 0.1F;
				} else if(mouseHovering) {
					draw = true;
					color = 0.5F;
				}
				if(draw) {
					GL11.glColor4f(color, color, color, 0.6F);
					HudUtils.drawModalRect(0, 0, area.width, area.height);
				}
				color = .8F;
				GL11.glColor4f(color, color, color, .8F);
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, String.valueOf(id), 6, 2.5, 8);
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				if(id == currentPage) return;
				tempPreset = PresetManager.getPreset(id).clone();
				currentPage = id;
			}
			
		}
		
		private class PartKeyInfo extends Widget {
			
			int id;
			static final float STEP = 28.67F, WIDTH = STEP + 5.3F, LOGO_SIZE = 23.3F, HEIGHT = 46.7F;

			public PartKeyInfo(int _id) {
				super("ki" + _id, PageMain.this, 5 + WIDTH * _id, 18.5F, STEP, HEIGHT);
				id = _id;
				draw = true;
			}
			
			@Override
			public void draw(double mx, double my, boolean mouseHovering) {
				double tx = WIDTH / 2, ty = 4;
				float c = 0.8F;
				GL11.glColor4f(c, c, c, .8F);
				String str = LIKeyProcess.getKeyName(EventHandlerClient.getKeyId(id));
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, tx - TextUtils.getWidth(TextUtils.FONT_CONSOLAS_64, str, 8), ty, 8);
				tx = 2.5;
				ty = 20;
				HudUtils.drawRectOutline(tx, ty, LOGO_SIZE, LOGO_SIZE, 2);
				EntityPlayer player = Minecraft.getMinecraft().thePlayer;
				AbilityData data = AbilityDataMain.getData(player);
				ResourceLocation logo = data.getSkill(tempPreset.getSkillMapping(id)).getLogo();
				if(logo != null) {
					RenderUtils.loadTexture(logo);
					HudUtils.drawRect(tx, ty, LOGO_SIZE, LOGO_SIZE);
				}
				if(mouseHovering) {
					c = 0.6F;
					GL11.glColor4f(c, c, c, 0.6F);
					HudUtils.drawModalRect(0, 0, STEP, HEIGHT);
				}
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				modKey = id;
				isModifying = true;
				PageMain.this.visible = false;
				new PageModify();
			}
			
		}
		
		private class ButtonGeneric extends Widget {
			
			static final float WIDTH = 28f, HEIGHT = 10F;
			static final float 
				HOVERING_COLOR = 0.6F,
				ORDINARY_COLOR = 0.3F,
				TEXT_COLOR = 0.9F,
				TEXT_DISABLE_COLOR = 0.7F;

			public ButtonGeneric(String name, float x, float y) {
				super(name, PageMain.this, x, y, WIDTH, HEIGHT);
				draw = true;
			}
			
			@Override
			public void draw(double mx, double my, boolean mouseHovering) {
				float color;
				if(mouseHovering) {
					color = HOVERING_COLOR;
				} else color = ORDINARY_COLOR;
				GL11.glDepthFunc(GL11.GL_ALWAYS);
				GL11.glColor4f(color, color, color, 0.6F);
				HudUtils.drawModalRect(0, 0, WIDTH, HEIGHT);
				float fsize = 5F;
				color = TEXT_COLOR;
				GL11.glColor4f(color, color, color, 0.9F);
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, ID, 
						WIDTH / 2 - TextUtils.getWidth(TextUtils.FONT_CONSOLAS_64, ID, fsize) / 2,
						HEIGHT / 2 - TextUtils.getHeight(TextUtils.FONT_CONSOLAS_64, ID, fsize) / 2, fsize);
				GL11.glDepthFunc(GL11.GL_LEQUAL);
			}
			
		}
		
		public PageMain() {
			super("main", gui, 0, 0, WIDTH, HEIGHT);
			draw = true;
			
			for(int i = 0; i < MAX_PAGE; ++i) {
				new PartPageSel(i, PAGE_STEP * i);
			}
			for(int i = 0; i < EventHandlerClient.MAX_KEYS; ++i) {
				new PartKeyInfo(i);
			}
			
			new ButtonGeneric("Accept", 71, 67.5F) {
				@Override
				public void onMouseDown(double mx, double my) {
					PresetManager.setPreset(currentPage, tempPreset);
				}
			};
			
			new ButtonGeneric("Restore", 107.5F, 67.5F) {
				@Override
				public void onMouseDown(double mx, double my) {
					tempPreset = PresetManager.getPreset(currentPage).clone();
				}
			};
		}

		@Override
		public void draw(double mx, double my, boolean hover) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			HudUtils.setZLevel(zLevel);
			GL11.glPushMatrix(); {
				float color = 0.15F;
				GL11.glColor4f(color, color, color, 0.6F);
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				HudUtils.drawModalRect(0, 0, WIDTH, HEIGHT);
				
				color = 0.25F;
				GL11.glColor4f(color, color, color, 0.5F);
				HudUtils.drawModalRect(0, 0, WIDTH, HEIGHT / 6);
			} GL11.glPopMatrix(); 
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			
			//page text and page selection
			GL11.glColor4f(1, 1, 1, 0.8F);
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, ACLangs.presetSettings(), 73, 3, 7);
		}
		
	}
	
	private class PageModify extends Widget {
		
		private class PartSkillInfo extends Widget {
			
			SkillBase skill;
			public final int id;

			public PartSkillInfo(SkillBase _skill, int i, double beg) {
				super("sklmod" + i, PageModify.this, 
					beg + STEP * i , HEIGHT / 2 - WIDTH / 2,
					WIDTH, WIDTH);
				draw = true;
				skill = _skill;
				this.id = i;
			}
			
			@Override
			public void draw(double mx, double my, boolean mouseHovering) {
				final float lsize = 24;
			
				float c = .8F;
				GL11.glColor4f(c, c, c, .8F);
				float tx = WIDTH / 2 - lsize / 2;
				HudUtils.drawRectOutline(tx, tx, lsize, lsize, 2);
				ResourceLocation logo = skill.getLogo();
				if(logo != null) {
					GL11.glColor4f(1, 1, 1, 1);
					RenderUtils.loadTexture(logo);
					HudUtils.drawRect(tx, tx, lsize, lsize);
				}
				
				if(mouseHovering) {
					c = .4F;
					GL11.glColor4f(c, c, c, .5F);
					HudUtils.drawModalRect(0, 0, WIDTH, WIDTH);
				}
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				tempPreset.setSkillMapping(modKey, id);
				isModifying = false;
				pageMain.visible = true;
				PageModify.this.dispose();
			}
			
		}

		static final float HEIGHT = 50, WIDTH = 30, STEP = WIDTH + 10;

		public PageModify() {
			super("modify", gui, 0, 0, width, HEIGHT);
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			AbilityData data = AbilityDataMain.getData(player);
			
			List<Integer> learnedSkills = data.getLearnedSkillList();
			double beg = width / 2 - ((learnedSkills.size() - 1) * STEP + WIDTH) / 2;
			for(int i : learnedSkills) {
				new PartSkillInfo(data.getSkill(i), i, beg);
			}
			draw = true;
		}
		
		@Override
		public void draw(double mx, double my, boolean hover) {
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			HudUtils.setZLevel(zLevel);
			GL11.glDepthFunc(GL11.GL_ALWAYS);
			GL11.glPushMatrix(); {
				float c = .3F;
				GL11.glColor4f(c, c, c, .6F);
				
				RenderUtils.loadTexture(ACClientProps.TEX_GUI_KS_MASK);
				HudUtils.drawRect(0, 0, width, HEIGHT);
				
			} GL11.glPopMatrix();
			GL11.glDepthFunc(GL11.GL_LEQUAL);
		}
		
	}
	
}