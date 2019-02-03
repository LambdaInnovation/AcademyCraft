package cn.academy.item;

import cn.academy.AcademyCraft;
import cn.academy.Resources;
import cn.academy.client.render.item.BakedModelForTEISR;
import cn.academy.client.render.item.TEISRModel;
import cn.academy.entity.EntityMagHook;
import cn.lambdalib2.render.TransformChain;
import cn.lambdalib2.util.SideUtils;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Matrix4f;

import java.util.Objects;

/**
 * Elec Move Support Hook
 * @author WeathFolD
 */
public class ItemMagHook extends Item {

    private ModelResourceLocation _modelLocation;

    public ItemMagHook() {
        setCreativeTab(AcademyCraft.cct);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SuppressWarnings("sideonly")
    public void afterRegistry() {
        if (SideUtils.isClient())
            initClient();
    }

    @SideOnly(Side.CLIENT)
    private void initClient() {
        _modelLocation = new ModelResourceLocation(
            Objects.requireNonNull(getRegistryName()),
            "inventory");
        ModelLoader.setCustomMeshDefinition(this, stack -> _modelLocation);
        setTileEntityItemStackRenderer(new TEISRModel(
            Resources.getModel("maghook"),
            Resources.getTexture("models/maghook"),
            new TransformChain().scale(0.01f).build()
        ));
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onModelBake(ModelBakeEvent ev) {
        BakedModelForTEISR bakedModel = new BakedModelForTEISR(_modelLocation);

        IBakedModel original = ev.getModelRegistry().getObject(_modelLocation);
        bakedModel.mapModel(TransformType.GUI, original);

        Matrix4f fpTrans = new TransformChain()
            .scale(1.4f)
            .rotate(0, 90, 180)
            .translate(0f, .5f, 0.4f)
            .build();

        Matrix4f fpTransLeft = new TransformChain(fpTrans)
            .translate(1.4f, 0f, 0f)
            .build();

        Matrix4f tpTrans = new TransformChain()
            .rotate(0, 90, 180)
            .scale(.8f)
            .translate(-.4f, .5f, .7f)
            .build();

        Matrix4f tpTransLeft = new TransformChain(tpTrans)
            .translate(0.9f, 0f, 0f)
            .build();

        bakedModel.mapTransform(TransformType.FIRST_PERSON_LEFT_HAND, fpTransLeft);
        bakedModel.mapTransform(TransformType.FIRST_PERSON_RIGHT_HAND, fpTrans);

        bakedModel.mapTransform(TransformType.THIRD_PERSON_LEFT_HAND, tpTransLeft);
        bakedModel.mapTransform(TransformType.THIRD_PERSON_RIGHT_HAND, tpTrans);

        bakedModel.mapTransform(TransformType.GROUND,
            new TransformChain().rotate(0, 90, 180).translate(-.4f, .9f, .7f).scale(.5f).build());

        ev.getModelRegistry().putObject(_modelLocation, bakedModel);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        world.playSound(
            player,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_EGG_THROW,
            SoundCategory.PLAYERS,
            0.5F,
            0.4F / (itemRand.nextFloat() * 0.4F + 0.8F)
        );
        if(!world.isRemote) {
            world.spawnEntity(new EntityMagHook(player));
            if(!player.capabilities.isCreativeMode)
                stack.shrink(1);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

}