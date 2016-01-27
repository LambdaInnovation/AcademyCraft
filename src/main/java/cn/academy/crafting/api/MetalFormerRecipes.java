/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.crafting.api;

import cn.academy.crafting.block.TileMetalFormer.Mode;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.s11n.InstanceSerializer;
import cn.lambdalib.networkcall.s11n.RegSerializable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagInt;

import java.util.ArrayList;
import java.util.List;

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
