/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.tutorial.client

import cn.academy.core.{AcademyCraft, LocalHelper}
import cn.academy.misc.tutorial.TutorialData
import cn.lambdalib2.util.key.KeyManager
import cn.lambdalib2.util.markdown.{GLMarkdownRenderer, MarkdownParser}
import cn.lambdalib2.util.mc.SideHelper

import scala.collection.Map

/**
  * This class renderers markdown with keyid display support.
  */
class ACMarkdownRenderer extends GLMarkdownRenderer {

  private val tutLocal = LocalHelper.at("ac.tutorial")

  override def onTag(name: String, attr: Map[String, String]) = {
    super.onTag(name, attr)
    def render(keyName: String) = onTextContent(keyName, Set(MarkdownParser.Reference()))

    if (name == "key") {
      val keyid = attr("id")
      val cfg = AcademyCraft.config
      if (cfg.hasKey("keys", keyid)) {
        render(KeyManager.getKeyName(cfg.get("keys", keyid, -1).getInt))
      } else {
        render("???")
      }
    }

    if (name == "misakaname") {
      val name = tutLocal.getFormatted("misaka",
        TutorialData.get(SideHelper.getThePlayer).getMisakaID.asInstanceOf[AnyRef])
      onTextContent(name, Set(MarkdownParser.Strong()))
    }
  }

}
