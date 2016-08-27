package cn.academy.medicine

import cn.academy.core.command.ACCommand
import cn.academy.medicine.MedSynth.MedicineApplyInfo
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegCommand
import cn.lambdalib.template.command.LICommandBase
import net.minecraft.command.{CommandBase, ICommandSender}
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack

@Registrant
@RegCommand
class CommandMedicine extends ACCommand("med") {
  val commands = List("help", "props", "synth")

  override def getCommandName: String = "med"

  override def processCommand(ics : ICommandSender, args : Array[String]): Unit = {
    def msg(content: String, args: Any*) = {
      LICommandBase.sendChat(ics, content, args.map(_.asInstanceOf[AnyRef]): _*)
    }

    def synthFromArgs(method: MedicineApplyInfo => ItemStack) = {
      val player = CommandBase.getCommandSenderAsPlayer(ics)
      val parsedProps = args.toList.drop(1).map(Properties.find)

      if (parsedProps.forall(_.isDefined)) {
        val props = parsedProps.flatten
        val applyInfo = MedSynthesizer.synth(props)
        val medicine = method(applyInfo)

        val entity = new EntityItem(player.worldObj, player.posX, player.posY + 1, player.posZ, medicine)
        player.worldObj.spawnEntityInWorld(entity)
      } else {
        msg(getLoc("no_prop"))
      }
    }

    if (args.length == 0) {
      msg(getCommandUsage(ics))
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
