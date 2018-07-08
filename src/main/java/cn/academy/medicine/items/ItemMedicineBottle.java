package cn.academy.medicine.items;

import cn.academy.core.Resources;
import cn.academy.medicine.MedSynth;
import cn.academy.medicine.MedicineApplyInfo;
import cn.academy.medicine.ModuleMedicine;
import cn.academy.medicine.Properties;
import cn.lambdalib.pipeline.api.ShaderProgram;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.helper.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import java.util.List;

import static net.minecraftforge.client.IItemRenderer.ItemRenderType.ENTITY;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUseProgram;

public class ItemMedicineBottle extends ItemMedicineBase {

    @SideOnly(Side.CLIENT)
    public static class RenderMedicineBottle implements IItemRenderer {
        java.util.function.Function<ItemStack, Color> fn;

        public RenderMedicineBottle(java.util.function.Function<ItemStack, Color> fn){
            this.fn=fn;
        }
        ResourceLocation texture = Resources.getTexture("items/med_bottle");
        ShaderProgram program = ShaderProgram.load(
                Resources.getShader("med_bottle.vert"),
                Resources.getShader("med_bottle.frag")
        );
        int locColor = glGetUniformLocation(program.getProgramID(), "u_color");

        @Override
        public boolean handleRenderType(ItemStack item, ItemRenderType renderType){
            return true;
        }

        @Override
        public boolean shouldUseRenderHelper(ItemRenderType renderType, ItemStack item, ItemRendererHelper helper){
            return renderType == ENTITY;
        }

        @Override
        public void renderItem(ItemRenderType renderType, ItemStack stack , Object... data){
            RenderUtils.loadTexture(texture);
            glUseProgram(program.getProgramID());

            {
                Color color = fn.apply(stack);
                glUniform4f(locColor, (float)color.r, (float)color.g, (float)color.b, (float)color.a);
            }

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            switch(renderType){
                case EQUIPPED:
                case EQUIPPED_FIRST_PERSON:
                case ENTITY:
                    if (renderType == ENTITY) {
                        glTranslated(-.5, -0.1, 0);
                    }
                    RenderUtils.drawEquippedItem(.04f, texture);
                    break;
                case INVENTORY:
                    HudUtils.rect(0, 0, 16, 16);
            }
            glUseProgram(0);
        }
    }
    public ItemMedicineBottle() {
        super("medicine_bottle");
    }

    public static ItemStack create(MedicineApplyInfo info){
        ItemStack stack = new ItemStack(ModuleMedicine.medicineBottle);
        MedSynth.writeApplyInfo(stack, info);
        return stack;
    }
    @Override
    public void getSubItems(Item item, CreativeTabs cct, List list2){
        List<ItemStack> list = list2;

        // For debug
        list.add(create(new MedicineApplyInfo(Properties.instance.Targ_Life, Properties.instance.Str_Mild
                , 1.0f, Properties.instance.Apply_Instant_Incr, 0.5f)));
        list.add(create(new MedicineApplyInfo(Properties.instance.Targ_Life, Properties.instance.Str_Weak
                , 1.0f, Properties.instance.Apply_Instant_Incr, 0.5f)));
        list.add(create(new MedicineApplyInfo(Properties.instance.Targ_Life, Properties.instance.Str_Normal
                , 1.0f, Properties.instance.Apply_Instant_Incr, 0.5f)));
        list.add(create(new MedicineApplyInfo(Properties.instance.Targ_Life, Properties.instance.Str_Strong
                , 2.0f, Properties.instance.Apply_Instant_Decr, 0.5f)));
    }



}
