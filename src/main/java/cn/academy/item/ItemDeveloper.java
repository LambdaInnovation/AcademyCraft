package cn.academy.item;

import cn.academy.Resources;
import cn.academy.ability.client.ui.DeveloperUI;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.PortableDevData;
import cn.academy.client.render.item.TEISRModel;
import cn.academy.client.render.item.BakedModelForTEISR;
import cn.lambdalib2.render.TransformChain;
import cn.lambdalib2.util.SideUtils;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.lwjgl.util.vector.Matrix4f;

/**
 * @author WeAthFolD
 */
public class ItemDeveloper extends ItemEnergyBase {

//    @SideOnly(Side.CLIENT)
//    @RegItem.Render
//    public static RenderDeveloperPortable renderer;
//
    public static final DeveloperType type = DeveloperType.PORTABLE;

    private final ModelResourceLocation _modelLocation = new ModelResourceLocation("academy:developer_portable", "inventory");

    public ItemDeveloper() {
        super(type.getEnergy(), type.getBandwidth());
        this.bFull3D = true;

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SuppressWarnings("sideonly")
    public void afterRegistry() {
        if (SideUtils.isClient())
            initClient();
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onModelBake(ModelBakeEvent ev) {
        BakedModelForTEISR bakedModel = new BakedModelForTEISR(_modelLocation);
        Matrix4f fpTrans = new TransformChain()
            .rotate(0f, 180f, 0f)
            .scale(.3f)
            .translate(.34f, -.1f, -.1f)
            .build();
        bakedModel.mapTransform(TransformType.FIRST_PERSON_LEFT_HAND, fpTrans);
        bakedModel.mapTransform(TransformType.FIRST_PERSON_RIGHT_HAND, fpTrans);

        Matrix4f tpTrans = new TransformChain()
            .rotate(0, 180, 0).scale(.2f)
            .build();
        bakedModel.mapTransform(TransformType.THIRD_PERSON_LEFT_HAND, tpTrans);
        bakedModel.mapTransform(TransformType.THIRD_PERSON_RIGHT_HAND, tpTrans);

        bakedModel.mapTransform(TransformType.GROUND,
            new TransformChain().scale(-.15f, -.15f, .15f).translate(0, 0.1f, 0).build());

        IBakedModel original = ev.getModelRegistry().getObject(_modelLocation);
        bakedModel.mapModel(TransformType.GUI, original);

        ev.getModelRegistry().putObject(_modelLocation, bakedModel);
    }

    @SideOnly(Side.CLIENT)
    private void initClient() {
        ModelLoader.setCustomMeshDefinition(this, stack -> _modelLocation);
        setTileEntityItemStackRenderer(new TEISRModel(
            Resources.getModel("developer_portable"),
            Resources.getTexture("models/developer_portable"),
            new TransformChain().build()
        ));
    }

    @Override
    @SuppressWarnings("sideonly")
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if(player.world.isRemote) {
            displayGui(player);
        }
        
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
    
    @SideOnly(Side.CLIENT)
    private void displayGui(EntityPlayer player) {
        Minecraft.getMinecraft().displayGuiScreen(DeveloperUI.apply(PortableDevData.get(player)));
    }

    @Override
    public double getMaxEnergy() {
        return type.getEnergy();
    }

    @Override
    public double getBandwidth() {
        return type.getBandwidth();
    }

}