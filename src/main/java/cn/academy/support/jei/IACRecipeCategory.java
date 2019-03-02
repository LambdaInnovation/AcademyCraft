package cn.academy.support.jei;

import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.List;

public abstract class IACRecipeCategory  implements IRecipeCategory<IRecipeWrapper>
{
    Block block;
    public IACRecipeCategory(Block block)
    {
        this.block = block;

    }

    protected abstract List<SlotPos> getInputs();
    protected abstract List<SlotPos> getOutputs();
    protected List<List<ItemStack>> getInputStacks(IIngredients ingredients) {
        return ingredients.getInputs(ItemStack.class);
    }

    protected List<List<ItemStack>> getOutputStacks(IIngredients ingredients) {
        return ingredients.getOutputs(ItemStack.class);
    }

    @Override
    public String getUid()
    {
        return block.getTranslationKey();
    }

    @Override
    public String getTitle()
    {
        return block.getLocalizedName();
    }

    @Override
    public String getModName()
    {
        return "academy";
    }

    public ItemStack getBlockStack() {
        return new ItemStack(ItemBlock.getItemFromBlock(block), 1);
    }


    @Override
    public void setRecipe(IRecipeLayout iRecipeLayout, IRecipeWrapper iRecipeWrapper, IIngredients iIngredients)
    {
        IGuiItemStackGroup itemStacks = iRecipeLayout.getItemStacks();
        List<SlotPos> inputSlots = this.getInputs();//获取GUI上所有的输入槽
        List<List<ItemStack>> inputStacks = this.getInputStacks(iIngredients);//(推测是)获取NEI中的输入表（可能是空的）

        int idx;
        for(idx = 0; idx < inputSlots.size(); ++idx) {
            SlotPos pos = inputSlots.get(idx);
            itemStacks.init(idx, true, pos.x, pos.y);//初始化可用槽位，以便放入所有可用的ItemStack类型？
            if (idx < inputStacks.size()) {
                itemStacks.set(idx, inputStacks.get(idx));//？
            }
        }

        List<SlotPos> outputSlots = this.getOutputs();
        List<List<ItemStack>> outputStacks = this.getOutputStacks(iIngredients);

        for(int i = 0; i < outputSlots.size(); ++idx) {
            SlotPos pos = outputSlots.get(i);
            itemStacks.init(idx, false, pos.x, pos.y);
            if (i < outputStacks.size()) {
                itemStacks.set(idx, outputStacks.get(i));
            }

            ++i;
        }
    }
}
