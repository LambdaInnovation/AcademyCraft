package cn.academy.misc.tutorial.client;


import cn.academy.core.client.Resources;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.helper.GameTimer;
import cn.lambdalib.util.mc.StackUtils;
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
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.*;

public enum RecipeHandler {
	instance;

	private ResourceLocation tex = Resources.getTexture("guis/tutorial/crafting_grid.png");

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
		private final List<Widget> active = new ArrayList<>();

		private long lastAlternate;
		private int current;

		CraftingGridDisplay(List<StackDisplay[]> alt) {
			alternations = alt;

			transform.setSize(128, 128);
			lastAlternate = GameTimer.getAbsTime();

			addComponent(new DrawTexture().setTex(instance.tex));
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
				});
			}
		}

		private void rebuild() {
			active.forEach(this::removeWidget);
			active.clear();

			StackDisplay[] display = alternations.get(current);
			for(int i = 0; i < display.length; ++i) {
				int col = i % 3, row = i / 3;

				StackDisplay original = display[i];
				original.disposed = false;

				original.transform.setPos(col * STEP, row * STEP);
				active.add(original);
				addWidget(original);
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

		public StackDisplay(String hint, ItemStack... _stacks) {
			stacks = _stacks;
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

					// Renders the stack
					glPushMatrix();
					glTranslatef(0.0F, 0.0F, 32.0F);
					itemRender.zLevel = 200.0F;
					FontRenderer font = stack.getItem().getFontRenderer(stack);
					itemRender.renderItemAndEffectIntoGUI(font, mc.getTextureManager(), stack, 0, 0);
					itemRender.renderItemOverlayIntoGUI(font, mc.getTextureManager(), stack,
							0, 0 - (e.hovering ? 0 : 8), null);
					itemRender.zLevel = 0.0F;
					glPopMatrix();
				});
			}
		}

	}

	private ItemStack[] mapToStacks(Object obj) {
		if(obj == null) {
			return new ItemStack[0];
		}
		if(obj instanceof String) {
			return OreDictionary.getOres((String) obj).toArray(new ItemStack[] {});
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
		for(int i = 0; i < ret.length; ++i) {
			int row = i / width, col = i % width;
			ret[col + row * 3] = original[i];
		}
		return ret;
	}

	private StackDisplay[] toDisplay(String hint, Object[] objects) {
		return Arrays.stream(objects).map(x -> new StackDisplay(hint, mapToStacks(x))).toArray(StackDisplay[]::new);
	}

	private StackDisplay[] toDisplay(ShapedOreRecipe recipe) {
		return remap(toDisplay("ShapedOre", recipe.getInput()), getWidth(recipe));
	}

	private StackDisplay[] toDisplay(ShapedRecipes recipe) {
		return remap(toDisplay("Shaped", recipe.recipeItems), recipe.recipeWidth);
	}

	private StackDisplay[] toDisplay(ShapelessRecipes recipe) {
		return toDisplay("Shapeless", recipe.recipeItems.toArray());
	}

	private StackDisplay[] toDisplay(ShapelessOreRecipe recipe) {
		return toDisplay("ShapelessOre", recipe.getInput().toArray());
	}

	private boolean matchStack(ItemStack s1, ItemStack s2) {
		return StackUtils.isStackDataEqual(s1, s2);
	}

	public Widget recipeOfStack(ItemStack stack) {
		List<StackDisplay[]> displays = new ArrayList<>();
		for(IRecipe o : (List<IRecipe>) CraftingManager.getInstance().getRecipeList()) {
			if(matchStack(o.getRecipeOutput(), stack)) {
				if(o instanceof ShapedOreRecipe) {
					displays.add(toDisplay((ShapedOreRecipe) o));
				} else if(o instanceof ShapedRecipes) {
					displays.add(toDisplay((ShapedRecipes) o));
				} else if(o instanceof ShapelessRecipes) {
					displays.add(toDisplay((ShapelessRecipes) o));
				} else if(o instanceof ShapelessOreRecipe) {
					displays.add(toDisplay((ShapelessOreRecipe) o));
				}
			}
		}
		return new CraftingGridDisplay(displays);
	}

}
