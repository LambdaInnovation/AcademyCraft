package cn.academy.misc.tutorial.client;


import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
import cn.academy.energy.client.gui.EnergyUIHelper;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.helper.Font;
import cn.lambdalib.util.helper.Font.Align;
import cn.lambdalib.util.helper.GameTimer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.*;

// TODO Add ImagFusor and MetalFormer handlers
public enum RecipeHandler {
	instance;

	private ResourceLocation tex = Resources.getTexture("guis/tutorial/crafting_grid");

	private Field _$ShapedOreRecipe$fieldWidth;
	{
		try {
			_$ShapedOreRecipe$fieldWidth = ShapedOreRecipe.class.getDeclaredField("width");
			_$ShapedOreRecipe$fieldWidth.setAccessible(true);
		} catch(Exception e) {
			throw new RuntimeException("RecipeHandler reflection", e);
		}
	}

	private int getWidth(ShapedOreRecipe recipe) {
		try {
			return _$ShapedOreRecipe$fieldWidth.getInt(recipe);
		} catch(IllegalAccessException e) {
			throw new RuntimeException("Can't retrieve width of ShapedOreRecipe", e);
		}
	}

	/**
	 * This class displays a 3x3 crafting grid. Possible multiple recipes alternating.
	 */
	static class CraftingGridDisplay extends Widget {

		static final long ALTERNATE_TIME = 2000;
		static final int STEP = 43;

		private final List<StackDisplay[]> alternations;
		private final List<String> description;
		private final List<Widget> active = new ArrayList<>();

		private long lastAlternate;
		private int current;

		CraftingGridDisplay(List<StackDisplay[]> alt, List<String> desc) {
			alternations = alt;
			description = desc;

			transform.setSize(128, 128);
			transform.setCenteredAlign();
			transform.scale = 0.6;
			lastAlternate = GameTimer.getAbsTime();

			if(alternations.size() != 0) {
				rebuild();
				listen(FrameEvent.class, (w, e) ->
				{
					long time = GameTimer.getAbsTime();
					long dt = time - lastAlternate;
					if(dt > ALTERNATE_TIME) {
						lastAlternate = time;
						current = (current + 1) % alternations.size();
						rebuild();
					}

					// Renders recipe type hint
					String str = StatCollector.translateToLocal("ac.gui.crafttype." + description.get(current));
					Font.font.draw(str, transform.width / 2, -22, 17, 0xffffff, Align.CENTER);
				});
			}
			addComponent(new DrawTexture().setTex(instance.tex));
		}

		private void rebuild() {
			active.forEach(this::removeWidget);
			active.clear();

			StackDisplay[] display = alternations.get(current);
			for(int i = 0; i < display.length; ++i) {
				int col = i % 3, row = i / 3;

				StackDisplay original = display[i];
				if(original != null) {
					original.disposed = false;
					original.dirty = true;

					original.transform.setPos(5 + col * STEP, 5 + row * STEP);
					active.add(original);
					addWidget(original);
				}
			}
		}

	}

	/**
	 * This class displays one stack slot, possibly multiple stacks alternating.
	 */
	@SideOnly(Side.CLIENT)
	static class StackDisplay extends Widget {

		private static Minecraft mc = Minecraft.getMinecraft();
		private static RenderItem itemRender = RenderItem.getInstance();
		static final long ALTERNATE_TIME = 2000;

		private final ItemStack[] stacks;
		long lastAlternate;

		private int current = 0;

		public StackDisplay(ItemStack... _stacks) {
			stacks = _stacks;
			transform.setPos(0, 0).setSize(32, 34);
			lastAlternate = GameTimer.getAbsTime() + RandUtils.rangei(-1000, 1000);

			if(stacks.length != 0) { // Don't draw anything for empty stack
				listen(FrameEvent.class, (w, e) ->
				{
					long time = GameTimer.getAbsTime();
					long dt = time - lastAlternate;
					if(dt > ALTERNATE_TIME) {
						current = (current + 1) % stacks.length;
						lastAlternate = time;
					}

					ItemStack stack = stacks[current];

					// Hover effect
					if(e.hovering) {
						glColor4d(1, 1, 1, 0.15);
						HudUtils.colorRect(0, 0, transform.width, transform.height);
					}

					// Renders the stack

					glPushMatrix();
					glScaled(2, 2, 1);
					glTranslatef(0, 0, 1.0F);

					FontRenderer font = stack.getItem().getFontRenderer(stack);
					itemRender.renderItemIntoGUI(font, mc.getTextureManager(), stack, 0, 0);

					// WTF, you have opened up lighting???
					glDisable(GL_LIGHTING);
					glEnable(GL_BLEND);

					glPopMatrix();

					glPushMatrix();
					glTranslated(0, 0, 10);
					if (e.hovering) {
						EnergyUIHelper.drawTextBox(stack.getDisplayName(), e.mx + 10, e.my - 17, 17, 1000, Align.LEFT, true);
					}
					glPopMatrix();
				});
			}
		}

	}

	private ItemStack[] mapToStacks(Object obj) {
		if(obj == null) {
			return new ItemStack[0];
		}
		if(obj instanceof Collection) {
			return ((Collection<ItemStack>) obj).toArray(new ItemStack[0]);
		}
		if(obj instanceof Item) {
			Item item = (Item) obj;
			return IntStream.range(0, item.getMaxDamage() + 1)
					.mapToObj(i -> new ItemStack(item, 1, i))
					.toArray(ItemStack[]::new);
		}
		if(obj instanceof Block) {
			Block block = (Block) obj;
			Item item = Item.getItemFromBlock(block);
			return IntStream.range(0, item.getMaxDamage() + 1)
					.mapToObj(i -> new ItemStack(item, 1, i))
					.toArray(ItemStack[]::new);
		}
		return new ItemStack[] { (ItemStack) obj };
	}

	private StackDisplay[] remap(StackDisplay[] original, int width) {
		StackDisplay[] ret = new StackDisplay[9];
		for(int i = 0; i < original.length; ++i) {
			int row = i / width, col = i % width;
			ret[col + row * 3] = original[i];
		}
		return ret;
	}

	private StackDisplay[] toDisplay(Object[] objects) {
		return Arrays.stream(objects).map(x -> new StackDisplay(mapToStacks(x))).toArray(StackDisplay[]::new);
	}

	private StackDisplay[] toDisplay(ShapedOreRecipe recipe) {
		return remap(toDisplay(recipe.getInput()), getWidth(recipe));
	}

	private StackDisplay[] toDisplay(ShapedRecipes recipe) {
		return remap(toDisplay(recipe.recipeItems), recipe.recipeWidth);
	}

	private StackDisplay[] toDisplay(ShapelessRecipes recipe) {
		return toDisplay(recipe.recipeItems.toArray());
	}

	private StackDisplay[] toDisplay(ShapelessOreRecipe recipe) {
		return toDisplay(recipe.getInput().toArray());
	}

	private boolean matchStack(ItemStack s1, ItemStack s2) {
		if(s1 == null || s2 == null) {
			return false;
		}
		return s1.getItem() == s2.getItem();
	}

	public Widget recipeOfStack(ItemStack stack) {
		List<StackDisplay[]> displays = new ArrayList<>();
		List<String> descriptions = new ArrayList<>();
		for(IRecipe o : (List<IRecipe>) CraftingManager.getInstance().getRecipeList()) {
			if(matchStack(o.getRecipeOutput(), stack)) {
				AcademyCraft.log.info("Match " + o);
				if(o instanceof ShapedOreRecipe) {
					displays.add(toDisplay((ShapedOreRecipe) o));
					descriptions.add("shaped");
				} else if(o instanceof ShapedRecipes) {
					displays.add(toDisplay((ShapedRecipes) o));
					descriptions.add("shaped");
				} else if(o instanceof ShapelessRecipes) {
					displays.add(toDisplay((ShapelessRecipes) o));
					descriptions.add("shapeless");
				} else if(o instanceof ShapelessOreRecipe) {
					displays.add(toDisplay((ShapelessOreRecipe) o));
					descriptions.add("shapeless");
				}
			}
		}
		return new CraftingGridDisplay(displays, descriptions);
	}

}
