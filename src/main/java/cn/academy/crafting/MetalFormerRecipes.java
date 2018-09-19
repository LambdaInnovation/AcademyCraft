package cn.academy.crafting;

import cn.academy.block.tileentity.TileMetalFormer.Mode;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib2.s11n.network.NetworkS11n.NetS11nAdaptor;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WeAthFolD
 */
public enum MetalFormerRecipes {
    INSTANCE;

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
                    input.getCount() <= stack.getCount() &&
                    input.getItemDamage() == stack.getItemDamage();
        }
    }
    
    List<RecipeObject> objects = new ArrayList<>();
    
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

    @StateEventCallback
    private static void _init(FMLInitializationEvent ev) {
        NetworkS11n.addDirect(RecipeObject.class, new NetS11nAdaptor<RecipeObject>() {
            @Override
            public void write(ByteBuf buf, RecipeObject obj) {
                buf.writeByte(obj.id);
            }

            @Override
            public RecipeObject read(ByteBuf buf) throws ContextException {
                return INSTANCE.objects.get(buf.readByte());
            }
        });
    }
    
}