package cn.academy.core.client.gui;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.api.ability.Category;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.api.ctrl.Preset;
import cn.academy.api.ctrl.PresetManager;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.client.TextUtils;
import cn.liutils.api.client.gui.GuiScreenLIAdaptor;
import cn.liutils.api.client.gui.LIGuiPage;
import cn.liutils.api.client.gui.part.LIGuiButton;
import cn.liutils.api.client.gui.part.LIGuiPart;
import cn.liutils.api.client.util.HudUtils;
import cn.liutils.api.client.util.RenderUtils;
import cn.liutils.api.register.IGuiElement;
import cn.liutils.core.client.register.LIKeyProcess;

/**
 * @author WeAthFolD
 *
 */
public class GuiPresetSettings extends GuiScreenLIAdaptor {
	
	private boolean isSetting;
	
	private LIGuiPage pageMain = new PageMain();
	private PageModify pageModify = new PageModify();

	private int currentPage;
	private static final int MAX_PAGE = PresetManager.PRESET_COUNT;
	
	private Preset tempPreset; //Used for current setting
	private boolean isModifying;
	private int modKey;
	
	public GuiPresetSettings() {
		super(720, 360);
		tempPreset = PresetManager.getCurrentPreset().clone();
	}

	@Override
	protected void update() {
		screen.xSize = width;
		screen.ySize = height;
		super.update();
	}
	
    public void drawScreen(int mx, int my, float par3)
    {
    	GL11.glPushMatrix(); {
    		super.drawDefaultBackground();
    		super.drawElements(mx, my);
    	} GL11.glPopMatrix();
    }
	
	@Override
	public void updateActivedPages(Set<LIGuiPage> pages) {
		pages.add(isModifying ? pageModify : pageMain);
	}
	
	private class PageMain extends LIGuiPage {
		private static final float 
			RATIO = 1.75F, 
			HEIGHT = 80, 
			WIDTH = HEIGHT * RATIO,
			PAGE_STEP = 16;
		
		private class PartPageSel extends LIGuiPart {
			
			int id;

			public PartPageSel(int _id, float x) {
				super("sel" + _id, x, 0, PAGE_STEP, HEIGHT / 6);
				id = _id;
				doesDraw = true;
			}
			
			@Override
			public void drawAtOrigin(float mx, float my, boolean mouseHovering) {
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
					HudUtils.drawModalRect(0, 0, this.width, this.height);
				}
				color = .8F;
				GL11.glColor4f(color, color, color, .8F);
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, String.valueOf(id), 6, 2.5, 8);
			}
			
		}
		
		private class PartKeyInfo extends LIGuiPart {
			
			int id;
			static final float STEP = 28.67F, WIDTH = STEP + 5.3F, LOGO_SIZE = 23.3F, HEIGHT = 46.7F;

			public PartKeyInfo(int _id) {
				super("ki" + _id, 5 + WIDTH * _id, 18.5F, STEP, HEIGHT);
				id = _id;
				doesDraw = true;
			}
			
			@Override
			public void drawAtOrigin(float mx, float my, boolean mouseHovering) {
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
					HudUtils.drawTexturedModalRect(tx, ty, LOGO_SIZE, LOGO_SIZE);
				}
				if(mouseHovering) {
					c = 0.6F;
					GL11.glColor4f(c, c, c, 0.6F);
					HudUtils.drawModalRect(0, 0, STEP, HEIGHT);
				}
			}
			
			
		}
		
		private class ButtonGeneric extends LIGuiButton {
			
			static final float WIDTH = 28f, HEIGHT = 10F;
			static final float 
				HOVERING_COLOR = 0.6F,
				ORDINARY_COLOR = 0.3F,
				TEXT_COLOR = 0.9F,
				TEXT_DISABLE_COLOR = 0.7F;

			public ButtonGeneric(String name, float x, float y) {
				super(name, x, y, WIDTH, HEIGHT);
				this.doesDraw = true;
			}
			
			@Override
			public void drawAtOrigin(float mx, float my, boolean mouseHovering) {
				float color;
				if(mouseHovering && !isInvalid) {
					color = HOVERING_COLOR;
				} else color = ORDINARY_COLOR;
				GL11.glDepthFunc(GL11.GL_ALWAYS);
				GL11.glColor4f(color, color, color, 0.6F);
				HudUtils.drawModalRect(0, 0, WIDTH, HEIGHT);
				float fsize = 5F;
				color = isInvalid ? TEXT_DISABLE_COLOR : TEXT_COLOR;
				GL11.glColor4f(color, color, color, 0.9F);
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, name, 
						WIDTH / 2 - TextUtils.getWidth(TextUtils.FONT_CONSOLAS_64, name, fsize) / 2,
						HEIGHT / 2 - TextUtils.getHeight(TextUtils.FONT_CONSOLAS_64, name, fsize) / 2, fsize);
				GL11.glDepthFunc(GL11.GL_LEQUAL);
			}
			
		}
		
		public PageMain() {
			super(GuiPresetSettings.this, "main", width / 2 - WIDTH / 2, height / 2 - HEIGHT / 2);
			for(int i = 0; i < MAX_PAGE; ++i) {
				this.guiParts.add(new PartPageSel(i, PAGE_STEP * i));
			}
			for(int i = 0; i < EventHandlerClient.MAX_KEYS; ++i) {
				guiParts.add(new PartKeyInfo(i));
			}
			guiParts.add(new ButtonGeneric("Accept", 71, 67.5F));
			guiParts.add(new ButtonGeneric("Restore", 107.5F, 67.5F));
			float size = HEIGHT / 11;
			guiParts.add(new LIGuiPart("Close", WIDTH - size - 3, HEIGHT / 12 - size / 2, size, size)
				.setTextureOverride(ACClientProps.TEX_GUI_CLOSE)
				.setTextureCoords(0, 0)
				.setTexSize(1, 1));
		}

		@Override
		public void drawPage() {
			this.originX = width / 2 - WIDTH / 2;
			this.originY = height / 2 - HEIGHT / 2;
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			float color = 0.15F;
			GL11.glColor4f(color, color, color, 0.6F);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			HudUtils.setZLevel(zLevel);
			HudUtils.drawModalRect(0, 0, WIDTH, HEIGHT);
			color = 0.25F;
			GL11.glColor4f(color, color, color, 0.5F);
			HudUtils.drawModalRect(0, 0, WIDTH, HEIGHT / 6);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			
			//page text and page selection
			GL11.glColor4f(1, 1, 1, 0.8F);
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, "preset settings", 73, 3, 7);
		} 

		@Override
		public void addElements(Set<LIGuiPart> set) {
			
		}

		@Override
		public void onPartClicked(LIGuiPart part, float subX, float subY) {
			if(part.name.contains("sel")) {
				int i = Integer.parseInt(part.name.substring(3));
				if(i == currentPage) return;
				tempPreset = PresetManager.getPreset(i).clone();
				currentPage = i;
			} else if(part.name.equals("Accept")) {
				PresetManager.setPreset(currentPage, tempPreset);
			} else if(part.name.equals("Restore")) {
				tempPreset = PresetManager.getPreset(currentPage).clone();
			} else if(part.name.equals("Close")) {
				Minecraft.getMinecraft().thePlayer.closeScreen();
			}
			if(part instanceof PartKeyInfo) {
				PartKeyInfo ki = (PartKeyInfo) part;
				modKey = ki.id;
				isModifying = true;
				pageModify.init();
			}
		}
		
	}
	
	private class PageModify extends LIGuiPage {
		
		private class PartSkillInfo extends LIGuiPart {
			
			SkillBase skill;
			public final int id;

			public PartSkillInfo(SkillBase _skill, int i) {
				super("sklmod", 
						screen.xSize / 2 -WIDTH / 2 + (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2) * STEP
						, PageModify.this.height / 2 - WIDTH / 2, WIDTH, WIDTH);
				this.posY = PageModify.this.height / 2 - WIDTH / 2;
				skill = _skill;
				this.id = i;
				this.doesDraw = true;
			}
			
			@Override
			public void drawAtOrigin(float mx, float my, boolean mouseHovering) {
				final float lsize = 24;
			
				float c = .8F;
				GL11.glColor4f(c, c, c, .8F);
				float tx = width / 2 - lsize / 2;
				HudUtils.drawRectOutline(tx, tx, lsize, lsize, 2);
				ResourceLocation logo = skill.getLogo();
				if(logo != null) {
					GL11.glColor4f(1, 1, 1, 1);
					RenderUtils.loadTexture(logo);
					HudUtils.drawTexturedModalRect(tx, tx, lsize, lsize);
				}
				
				if(mouseHovering) {
					c = .4F;
					GL11.glColor4f(c, c, c, .5F);
					HudUtils.drawModalRect(0, 0, width, height);
				}
			}
			
		}
		
		float MASK_SCALE = 11;
		float WIDTH = 30, STEP = WIDTH + 10;
		float height = 0;

		public PageModify() {
			super(GuiPresetSettings.this, "modify", 0, screen.ySize / 2);
		}
		
		public void init() {
			this.guiParts.clear();
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			AbilityData data = AbilityDataMain.getData(player);
			Category ct = data.getCategory();
			height = screen.xSize / MASK_SCALE;
			WIDTH = height * .8F;
			STEP = WIDTH * 1.33F;
			this.originY = screen.ySize / 2 - height / 2;
			if(ct != null) {
				for(int i = 0; i < ct.getSkillCount(); ++i) {
					guiParts.add(new PartSkillInfo(ct.getSkill(i), i));
				}
			}
		}
		
		@Override
		public void drawPage() {
			GL11.glPushMatrix(); {
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				HudUtils.setZLevel(zLevel);
				
				float c = .3F;
				GL11.glColor4f(c, c, c, .6F);
				GL11.glDepthFunc(GL11.GL_ALWAYS);
				RenderUtils.loadTexture(ACClientProps.TEX_GUI_KS_MASK);
				HudUtils.drawTexturedModalRect(0, 0, screen.xSize, height);
				GL11.glDepthFunc(GL11.GL_LEQUAL);
			} GL11.glPopMatrix();
		}

		@Override
		public void addElements(Set<LIGuiPart> set) {}

		@Override
		public void onPartClicked(LIGuiPart part, float subX, float subY) {
			PartSkillInfo psi = (PartSkillInfo) part;
			tempPreset.setSkillMapping(modKey, psi.id);
			isModifying = false;
		}
		
	}
	
	public static final IGuiElement element = new IGuiElement() {

		@Override
		public Object getServerContainer(EntityPlayer player, World world,
				int x, int y, int z) {
			return null;
		}

		@Override
		public Object getClientGui(EntityPlayer player, World world, int x,
				int y, int z) {
			return new GuiPresetSettings();
		}
		
	};
	
}