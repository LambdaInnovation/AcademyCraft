package cn.academy.medicine

import cn.academy.medicine.MedSynth.MedicineApplyInfo
import cn.academy.util.ACCommand
import cn.lambdalib2.registry.mc.RegCommand
import cn.lambdalib2.util.PlayerUtils
import net.minecraft.command.{CommandBase, ICommandSender}
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer

@RegCommand
class CommandMedicine extends ACCommand("med") {
  val commands = List("help", "props", "synth")

  override def getName: String = "med"

  override def execute(svr: MinecraftServer, ics : ICommandSender, args : Array[String]): Unit = {
    def msg(content: String, args: Any*) = {
      PlayerUtils.sendChat(ics, content, args.map(_.asInstanceOf[AnyRef]): _*)
    }

    def synthFromArgs(method: MedicineApplyInfo => ItemStack) = {
      val player = CommandBase.getCommandSenderAsPlayer(ics)
      val parsedProps = args.toList.drop(1).map(Properties.find)

      if (parsedProps.forall(_.isDefined)) {
        val props = parsedProps.flatten
        val applyInfo = MedSynthesizer.synth(props)
        val medicine = method(applyInfo)

        val entity = new EntityItem(player.world, player.posX, player.posY + 1, player.posZ, medicine)
        player.world.spawnEntity(entity)
      } else {
        msg(getLoc("no_prop"))
      }
    }

    if (args.length == 0) {
      msg(getUsage(ics))
    } else {
      args.head match {
        case "help" | "?" =>
          for (c <- commands) {
            msg(getLoc(c))
          }

        case "props" =>
          Properties.allProperties.map(prop => prop.internalID + ": " + prop.displayDesc).foreach(msg(_))

        case "synth" => synthFromArgs(ItemMedicineBottle.create)

        case "synth_syringe" => ()

        case _ =>
          msg(locInvalid())
      }
    }
  }



}
