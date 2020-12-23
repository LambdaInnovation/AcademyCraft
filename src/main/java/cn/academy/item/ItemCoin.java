package cn.academy.item;

import cn.academy.Resources;
import cn.academy.client.render.item.BakedModelForTEISR;
import cn.academy.entity.EntityCoinThrowing;
import cn.academy.event.CoinThrowEvent;
import cn.lambdalib2.render.TransformChain;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.SideUtils;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.Item;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author KSkun
 */
public class ItemCoin extends Item {

    // Key: PlayerName
    private static Map<String, EntityCoinThrowing> client = new HashMap<>(), server = new HashMap<>();

    private final ModelResourceLocation _modelLocation = new ModelResourceLocation("academy:coin", "inventory");

    public ItemCoin() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SuppressWarnings("sideonly")
    public void afterRegistry() {
        if (SideUtils.isClient())
            initClient();
    }

    @SideOnly(Side.CLIENT)
    private void initClient() {
        ModelLoader.setCustomMeshDefinition(this, stack -> _modelLocation);

        setTileEntityItemStackRenderer(new TileEntityItemStackRenderer() {
            @Override
            public void renderByItem(ItemStack itemStackIn) {
                GL11.glPushMatrix();
                GL11.glTranslated(0, 0, .5);
                RenderUtils.drawEquippedItem(0.04f, Resources.TEX_COIN_BACK, Resources.TEX_COIN_FRONT);
                GL11.glPopMatrix();
            }
        });
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onModelBake(ModelBakeEvent ev) {
        BakedModelForTEISR bakedModel = new BakedModelForTEISR(_modelLocation);
        Matrix4f fpTrans = new TransformChain().scale(.5f, .5f, .5f).translate(.2f, 0, -.1f).build();
        bakedModel.mapTransform(TransformType.FIRST_PERSON_LEFT_HAND, fpTrans);
        bakedModel.mapTransform(TransformType.FIRST_PERSON_RIGHT_HAND, fpTrans);

        Matrix4f tpTrans = new TransformChain().scale(.2f).translate(0, 0, .0f).build();
        bakedModel.mapTransform(TransformType.THIRD_PERSON_LEFT_HAND, tpTrans);
        bakedModel.mapTransform(TransformType.THIRD_PERSON_RIGHT_HAND, tpTrans);

        bakedModel.mapTransform(TransformType.GROUND,
            new TransformChain()
                .scale(-0.3f, -0.3f, 0.3f)
                .translate(0f, 0.1f, 0f)
                .build()
        );

        ev.getModelRegistry().putObject(_modelLocation, bakedModel);
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        EntityPlayer player = event.player;
        Map<String, EntityCoinThrowing> map = getMap(player);
        EntityCoinThrowing etc = getPlayerCoin(player);
        if(etc != null) {
            if(etc.isDead || 
                etc.world.provider.getDimension() != player.world.provider.getDimension()) {
                map.remove(player.getName());
            }
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if(getPlayerCoin(player) != null) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        //Spawn at both side, not syncing for render effect purpose
        EntityCoinThrowing etc = new EntityCoinThrowing(player, stack);
        world.spawnEntity(etc);

        player.playSound(Resources.sound("entity.flipcoin"), 0.5f, 1.0f);
        setPlayerCoin(player, etc);
        
        MinecraftForge.EVENT_BUS.post(new CoinThrowEvent(player, etc));
        if(!player.capabilities.isCreativeMode) {
            stack.setCount(stack.getCount() - 1);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    
    public static EntityCoinThrowing getPlayerCoin(EntityPlayer player) {
        EntityCoinThrowing etc = getMap(player).get(player.getName());
        if(etc != null && !etc.isDead)
            return etc;
        return null;
    }
    
    public static void setPlayerCoin(EntityPlayer player, EntityCoinThrowing etc) {
        Map<String, EntityCoinThrowing> map = getMap(player);
        map.put(player.getName(), etc);
    }
    
    private static Map<String, EntityCoinThrowing> getMap(EntityPlayer player) {
        return player.world.isRemote ? client : server;
    }

}