package cn.academy.medicine;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import java.util.*;
import java.util.function.Function;

public class MatExtraction {

  public static class ItemMeta{
      public Item item;
      public int meta;
      public String id;
      public ItemMeta(Item item, int meta) {
          this.item=item;
          this.meta=meta;
          String itemName = Item.itemRegistry.getNameForObject(item);
          id = item.getHasSubtypes()?(itemName + "_" + meta):itemName;
      }

      @Override
      public int hashCode() {
          return id.hashCode();
      }

      @Override
      public boolean equals(Object obj) {
          return obj instanceof ItemMeta && ((ItemMeta) obj).meta==this.meta && ((ItemMeta) obj).item == this.item;
      }
  }

    //type Recipe = (List[ItemMeta], List[Property])

    public static List<ItemMeta> ofBlock(Block block)
    {
        return ofItem(Item.getItemFromBlock(block));
    }

    public static List<ItemMeta> ofBlock(Block block, int... damages){
      return ofItem(Item.getItemFromBlock(block), damages);
    }

    public static List<ItemMeta> ofItem(Item item){
        int[] dmgs=new int[item.getMaxDamage()+1];
        for(int i=0;i<=item.getMaxDamage();i++){
            dmgs[i]=i;
        }
      return ofItem(item,dmgs);
    }
    public static List<ItemMeta> ofItem(Item item, int... damage){
        List<ItemMeta> ret=new ArrayList<>();
        for(int i:damage){
            ret.add(new ItemMeta(item,i));
        }
        return ret;
    }
    static class Recipe{
        public List<ItemMeta> metas;
        public List<Properties.Property> props;
        public Recipe(List<ItemMeta> metas, List<Properties.Property> props){
            this.metas=metas;
            this.props=props;
        }
    }
    public static Map<ItemMeta, List<Properties.Property>> allRecipes = new LinkedHashMap<ItemMeta, List<Properties.Property>>(){{
        for(ItemMeta meta:ofItem(Items.apple))
            put(meta, Arrays.asList(Properties.instance.Targ_Life, Properties.instance.Str_Normal));
        for(ItemMeta meta:ofItem(Items.potato))
            put(meta, Collections.singletonList(Properties.instance.Targ_CP));
        for(ItemMeta meta:ofItem(Items.poisonous_potato))
                put(meta, Arrays.asList(Properties.instance.Var_Fluct));
        for(ItemMeta meta:ofItem(Items.wheat))
                put(meta, Arrays.asList(Properties.instance.Apply_Continuous_Incr));
        for(ItemMeta meta:ofItem(Items.reeds))
                put(meta, Arrays.asList(Properties.instance.Targ_MoveSpeed));
        for(ItemMeta meta:ofBlock(Blocks.cactus))
                put(meta, Arrays.asList(Properties.instance.Str_Mild));
        for(ItemMeta meta:ofItem(Items.wheat_seeds))
                put(meta, Arrays.asList(Properties.instance.Apply_Continuous_Incr, Properties.instance.Str_Mild));
        for(ItemMeta meta:ofItem(Items.carrot))
                put(meta, Arrays.asList(Properties.instance.Str_Weak));
        for(ItemMeta meta:ofItem(Items.melon))
                put(meta, Arrays.asList(Properties.instance.Targ_Life));
        for(ItemMeta meta:ofBlock(Blocks.cocoa))
                put(meta, Arrays.asList(Properties.instance.Targ_Overload, Properties.instance.Apply_Continuous_Decr));
        for(ItemMeta meta:ofItem(Items.egg))
                put(meta, Arrays.asList(Properties.instance.Apply_Instant_Incr));
        for(ItemMeta meta:ofItem(Items.milk_bucket))
                put(meta, Arrays.asList(Properties.instance.Apply_Continuous_Incr, Properties.instance.Var_Stabilize));
        for(ItemMeta meta:ofItem(Items.feather))
                put(meta, Arrays.asList(Properties.instance.Targ_Jump));
        for(ItemMeta meta:ofItem(Items.spider_eye))
                put(meta, Arrays.asList(Properties.instance.Targ_Cooldown));
        for(ItemMeta meta:ofItem(Items.bone))
                put(meta, Arrays.asList(Properties.instance.Str_Mild));
        for(ItemMeta meta:ofItem(Items.rotten_flesh))
                put(meta, Arrays.asList(Properties.instance.Var_Fluct));
        for(ItemMeta meta:ofItem(Items.porkchop))
                put(meta, Arrays.asList(Properties.instance.Targ_CP));
        for(ItemMeta meta:ofItem(Items.cooked_porkchop))
                put(meta, Arrays.asList(Properties.instance.Targ_CP));
        for(ItemMeta meta:ofItem(Items.chicken))
                put(meta, Arrays.asList(Properties.instance.Targ_CP));
        for(ItemMeta meta:ofItem(Items.cooked_chicken))
                put(meta, Arrays.asList(Properties.instance.Targ_CP));
        for(ItemMeta meta:ofItem(Items.beef))
                put(meta, Arrays.asList(Properties.instance.Targ_CP));
        for(ItemMeta meta:ofItem(Items.cooked_beef))
                put(meta, Arrays.asList(Properties.instance.Targ_CP));
        for(ItemMeta meta:ofItem(Items.fish))
                put(meta, Arrays.asList(Properties.instance.Apply_Continuous_Incr)); // Var_Stablize
        for(ItemMeta meta:ofItem(Items.cooked_fished))
                put(meta, Arrays.asList(Properties.instance.Apply_Continuous_Incr));
        for(ItemMeta meta:ofItem(Items.ender_pearl))
                put(meta, Arrays.asList(Properties.instance.Targ_CP, Properties.instance.Targ_Overload,
                        Properties.instance.Var_Fluct));
        for(ItemMeta meta:ofItem(Items.blaze_powder))
                put(meta, Arrays.asList(Properties.instance.Targ_Attack, Properties.instance.Str_Strong));
        // for(ItemMeta meta:ofItem(Items.redstone))
        //        put(meta, Arrays.asList(Var_Neturalize));
        for(ItemMeta meta:ofItem(Items.glowstone_dust))
                put(meta, Arrays.asList(Properties.instance.Str_Strong, Properties.instance.Var_Desens));
        for(ItemMeta meta:ofItem(Items.nether_star))
                put(meta, Arrays.asList(Properties.instance.Var_Infinity));
    }};

    public static boolean checkIfReciped(Item item, int i){
        return allRecipes.containsKey(new ItemMeta(item, i));
    }

}
