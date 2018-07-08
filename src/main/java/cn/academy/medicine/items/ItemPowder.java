package cn.academy.medicine.items;

import cn.academy.core.item.ACItem;
import cn.academy.medicine.MatExtraction;
import cn.academy.medicine.Properties;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ItemPowder extends ACItem {
    public MatExtraction.ItemMeta source;
    public Properties.Property prop;

    final ItemStack dummyStack;

    public ItemPowder(MatExtraction.ItemMeta source, Properties.Property prop) {
        super("powder");
        this.source=source;
        this.prop=prop;
        dummyStack = new ItemStack(source.item, 1, source.meta);
        setTextureName("academy:powder/" + internalID());
    }

    public String internalID()
    {
        String id = source.id;
        int test=id.indexOf(":");
        return id.substring(test+1) + "_" + prop.internalID();
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack){
        return source.item.getItemStackDisplayName(dummyStack) + " " + super.getItemStackDisplayName(stack);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list2, boolean wtf){
        List<String> list = list2;
        list.add(prop.stackDisplayHint());
    }

    public static Properties.Property getProperty(ItemStack stack) {
        Item item = stack.getItem();
        if(item instanceof ItemPowder)
        {
            return ((ItemPowder) item).prop;
        }
        throw new IllegalArgumentException("Given itemStack is not a powder");
    }

    public static void _init(){
        for(Map.Entry<MatExtraction.ItemMeta, List<Properties.Property>> entry: MatExtraction.allRecipes.entrySet())
        {
            for(Properties.Property p:entry.getValue()) {
                ItemPowder item = new ItemPowder(entry.getKey(), p);
                GameRegistry.registerItem(item, item.internalID());
            }
        }
    }
}
