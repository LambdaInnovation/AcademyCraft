/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.crafting.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagInt;
import cn.academy.crafting.block.TileMetalFormer.Mode;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.s11n.InstanceSerializer;
import cn.lambdalib.networkcall.s11n.RegSerializable;

/**
 * @author WeAthFolD
 */
@Registrant
public enum MetalFormerRecipes {
    INSTANCE;
    
    @RegSerializable(instance = RecipeSerializer.class)
    public static class RecipeObject {
        private int id = -1;
        
        public final Mode mode;
        public final ItemStack input;
        public final ItemStack output;
        
        private RecipeObject(ItemStack _input, ItemStack _output, Mode _mode) {
            input = _input;
            output = _output;
            mode = _mode;
        }
        
        public boolean accepts(ItemStack stack, Mode mode2) {
            return  stack != null &&
                    mode == mode2 &&
                    input.getItem() == stack.getItem() &&
                    input.stackSize <= stack.stackSize && 
                    input.getItemDamage() == stack.getItemDamage();
        }
    }
    
    List<RecipeObject> objects = new ArrayList();
    
    public void add(ItemStack in, ItemStack out, Mode mode) {
        RecipeObject add = new RecipeObject(in, out, mode);
        add.id = objects.size();
        objects.add(add);
    }
    
    public RecipeObject getRecipe(ItemStack input, Mode mode) {
        for(RecipeObject recipe : objects) {
            if(recipe.accepts(input, mode))
                return recipe;
        }
        return null;
    }
    
    public List<RecipeObject> getAllRecipes() {
        return objects;
    }
    
    public static class RecipeSerializer implements InstanceSerializer<RecipeObject> {

        @Override
        public RecipeObject readInstance(NBTBase nbt) throws Exception {
            return INSTANCE.objects.get(((NBTTagInt)nbt).func_150287_d());
        }

        @Override
        public NBTBase writeInstance(RecipeObject obj) throws Exception {
            return new NBTTagInt(obj.id);
        }
        
    }
    
}
