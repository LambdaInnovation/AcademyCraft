package cn.academy.testmod

import cn.academy.entity.EntityBlock
import cn.lambdalib2.input.KeyHandler
import cn.lambdalib2.input.KeyManager
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.s11n.network.NetworkMessage
import cn.lambdalib2.s11n.network.NetworkS11n
import cn.lambdalib2.util.VecUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.math.Vec3d
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.input.Keyboard

object TestEntityBlock {

    @JvmStatic
    @SideOnly(Side.CLIENT)
    @StateEventCallback
    private fun init(ev: FMLInitializationEvent) {
        println("INIT!")
        NetworkS11n.addDirectInstance(TestEntityBlock)
        KeyManager.dynamic.addKeyHandler(Keyboard.KEY_P, object : KeyHandler() {
            override fun onKeyDown() {
                println("KeyPressed!")
                val player = Minecraft.getMinecraft().player
                NetworkMessage.sendToServer(TestEntityBlock, "create", player)
            }
        })
    }

    @NetworkMessage.Listener(channel = "create", side = [Side.SERVER])
    private fun createInServer(player: EntityPlayer) {
        println("CreateInServer!")
        val entity = EntityBlock(player)
        val block = Blocks.GRASS
        val blockState = block.defaultState

        entity.setBlock(block, blockState)
        val npos = VecUtils.add(player.getPositionEyes(0f), player.lookVec.scale(2.0))
        entity.setPosition(npos.x, npos.y, npos.z)

        require(player.world.spawnEntity(entity))
    }

}