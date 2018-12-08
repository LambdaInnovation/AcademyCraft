package cn.academy.tutorial.client;


import cn.academy.Resources;
import cn.academy.crafting.ImagFusorRecipes;
import cn.academy.crafting.ImagFusorRecipes.IFRecipe;
import cn.academy.crafting.MetalFormerRecipes;
import cn.academy.client.gui.EnergyUIHelper;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.WidgetContainer;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.*;

// TODO Add ImagFusor and MetalFormer handlers
@SideOnly(Side.CLIENT)
public enum RecipeHandler {
    instance;

    private WidgetContainer windows;
    private ResourceLocation tex = Resources.getTexture("guis/tutorial/crafting_grid");

    @StateEventCallback
    private static void __init(FMLInitializationEvent ev) {
        instance.windows = CGUIDocument.read(new ResourceLocation("academy:guis/tutorial_windows.xml"));
    }

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
     * This class displays a 3x3 crafting grid of a recipe.
     */
    static class CraftingGridDisplay extends Widget {

        static final FontOption option = new FontOption(24, FontAlign.CENTER, Colors.white());

        static final int STEP = 43;

        private final StackDisplay[] stacks;
        private final StackDisplay output;
        private final String description;

        CraftingGridDisplay(StackDisplay _output, StackDisplay[] _stacks, String desc) {
            stacks = _stacks;
            output = _output;
            description = desc;

            size(196, 128).centered().scale(0.6f);

            for(int i = 0; i < stacks.length; ++i) {
                int col = i % 3, row = i / 3;

                StackDisplay original = stacks[i];
                if(original != null) {
                    original.disposed = false;
                    original.dirty = true;

                    original.pos(5 + col * STEP, 5 + row * STEP);
                    addWidget(original);
                }
            }

            output.pos(148 + 5, 44 + 5);
            addWidget(output);

            listen(FrameEvent.class, (w, e) ->
            {
                // Renders recipe type hint
                String str = I18n.format("ac.gui.crafttype." + description);
                Resources.font().draw(str, transform.width / 2 - 30, -28, option);
            });
            addComponent(new DrawTexture().setTex(instance.tex));
        }

    }

    /**
     * This class displays one stack slot, possibly multiple stacks alternating.
     */
    @SideOnly(Side.CLIENT)
    static class StackDisplay extends Widget {

        private static Minecraft mc = Minecraft.getMinecraft();
//        private static RenderItem itemRender = RenderItem.getInstance();
        static final double ALTERNATE_TIME = 2;

        private final ItemStack[] stacks;
        double lastAlternate;

        private int current = 0;

        public StackDisplay(ItemStack... _stacks) {
            stacks = _stacks;
            transform.setPos(0, 0).setSize(32, 34);
            lastAlternate = GameTimer.getAbsTime() + RandUtils.ranged(-1, 1);

            if(stacks.length != 0) { // Don't draw anything for empty stack
                listen(FrameEvent.class, (w, e) ->
                {
                    double time = GameTimer.getAbsTime();
                    double dt = time - lastAlternate;
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

                    RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
                    glDisable(GL_LIGHTING);
                    RenderHelper.enableStandardItemLighting();
                    renderItem.renderItemIntoGUI(stack, 0, 0);
                    RenderHelper.disableStandardItemLighting();

                    // WTF, you have opened up lighting???
                    glDisable(GL_LIGHTING);
                    glEnable(GL_BLEND);

                    glPopMatrix();

                    glPushMatrix();
                    glTranslated(0, 0, 20);
                    if (e.hovering) {
                        EnergyUIHelper.drawTextBox(stack.getDisplayName(),
                                e.mx + 10, e.my - 17, 1000,
                                new FontOption(10 / w.scale, FontAlign.LEFT), true);
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
        if (obj instanceof Ingredient) {
            Ingredient ingredient = ((Ingredient) obj);
            return ingredient.getMatchingStacks();
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
        return remap(toDisplay(recipe.getIngredients().toArray()), getWidth(recipe));
    }

    private StackDisplay[] toDisplay(ShapedRecipes recipe) {
        return remap(toDisplay(recipe.recipeItems.toArray()), recipe.recipeWidth);
    }

    private StackDisplay[] toDisplay(ShapelessRecipes recipe) {
        return toDisplay(recipe.recipeItems.toArray());
    }

    private StackDisplay[] toDisplay(ShapelessOreRecipe recipe) {
        return toDisplay(recipe.getIngredients().toArray());
    }

    private boolean matchStack(ItemStack s1, ItemStack s2) {
        if(s1 == null || s2 == null) {
            return false;
        }
        if (s1 == ItemStack.EMPTY || s2 == ItemStack.EMPTY)
            return false;
        return s1.getItem() == s2.getItem() &&
                (!s1.getItem().getHasSubtypes() || s1.getItemDamage() == s2.getItemDamage());
    }

    private Widget drawMFRecipe(MetalFormerRecipes.RecipeObject recipe) {
        // CGUI Rocks
        Widget ret = windows.getWidget("MetalFormer").copy();
        ret.getWidget("slot_in").addWidget(new StackDisplay(recipe.input).centered());
        ret.getWidget("slot_out").addWidget(new StackDisplay(recipe.output).centered());
        DrawTexture.get(ret.getWidget("mode")).setTex(recipe.mode.texture);
        return ret;
    }

    private Widget drawIFRecipe(IFRecipe recipe) {
        Widget ret = windows.getWidget("ImagFusor").copy();
        ret.getWidget("slot_in").addWidget(new StackDisplay(recipe.consumeType).centered());
        ret.getWidget("slot_out").addWidget(new StackDisplay(recipe.output).centered());
        TextBox.get(ret.getWidget("amount")).setContent(String.valueOf(recipe.consumeLiquid));

        return ret;
    }

    private Widget drawSmeltRecipe(ItemStack in, ItemStack out) {
        Widget ret = windows.getWidget("Smelting").copy();

        ret.getWidget("slot_in").addWidget(new StackDisplay(in).centered());
        ret.getWidget("slot_out").addWidget(new StackDisplay(out).centered());

        return ret;
    }

    public Widget[] recipeOfBlock(Block block) {
        return recipeOfItem(Item.getItemFromBlock(block));
    }

    public Widget[] recipeOfItem(Item item) {
        List<ItemStack> lst = new ArrayList<>();
        if (item.getHasSubtypes()) {
            lst.addAll(IntStream.range(0, item.getMaxDamage())
                    .mapToObj(i -> new ItemStack(item, 1, i))
                    .collect(Collectors.toList()));
        } else {
            lst.add(new ItemStack(item));
        }

        return lst.stream().flatMap(stk -> Arrays.stream(recipeOfStack(stk))).toArray(Widget[]::new);
    }

    @SuppressWarnings("unchecked")
    public Widget[] recipeOfStack(ItemStack stack) {
        List<Widget> ret = new ArrayList<>();

        for(IRecipe o : CraftingManager.REGISTRY) {
            if(matchStack(o.getRecipeOutput(), stack)) {
                StackDisplay[] arr;
                String desc;

                if(o instanceof ShapedOreRecipe) {
                    arr = toDisplay((ShapedOreRecipe) o);
                    desc = "shaped";
                } else if(o instanceof ShapedRecipes) {
                    arr = toDisplay((ShapedRecipes) o);
                    desc = "shaped";
                } else if(o instanceof ShapelessRecipes) {
                    arr = toDisplay((ShapelessRecipes) o);
                    desc = "shapeless";
                } else if(o instanceof ShapelessOreRecipe) {
                    arr = toDisplay((ShapelessOreRecipe) o);
                    desc = "shapeless";
                } else {
                    throw new RuntimeException("Invalid recipe " + o);
                }

                ret.add(new CraftingGridDisplay(new StackDisplay(o.getRecipeOutput()), arr, desc));
            }
        }
        { // IF Recipes
            List<Widget> recipes = ImagFusorRecipes.INSTANCE.getAllRecipe().stream()
                    .filter(recipe -> itemDamageEqual(recipe.output, stack))
                    .map(instance::drawIFRecipe)
                    .collect(Collectors.toList());
            ret.addAll(recipes);
        }
        { // MF Recipes
            List<Widget> recipes = MetalFormerRecipes.INSTANCE.getAllRecipes().stream()
                    .filter(recipe -> itemDamageEqual(recipe.output, stack))
                    .map(instance::drawMFRecipe)
                    .collect(Collectors.toList());
            ret.addAll(recipes);
        }
        { // Smelting
            Map<ItemStack, ItemStack> smeltingList = FurnaceRecipes.instance().getSmeltingList();
            smeltingList.entrySet()
                    .stream()
                    .filter(entry -> {
                        ItemStack stack2 = entry.getValue();
                        return stack2.getItem() == stack.getItem() &&
                                (stack2.getItemDamage() == 32767 ||
                                 stack2.getItemDamage() == stack.getItemDamage());
                    })
                    .forEach(entry -> ret.add(drawSmeltRecipe(entry.getKey(), stack)));
        }

        return ret.toArray(new Widget[ret.size()]);
    }

    private boolean itemDamageEqual(ItemStack s1, ItemStack s2) {
        if (s1 == null || s2 == null) { return false; }

        return s1.getItem() == s2.getItem() && s1.getItemDamage() == s2.getItemDamage();
    }

}