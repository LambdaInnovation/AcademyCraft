/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.util

import cn.academy.core.{ModuleCoreClient, AcademyCraft}
import cn.lambdalib.util.key.KeyManager
import cn.lambdalib.util.markdown.{MarkdownParser, GLMarkdownRenderer}

import scala.collection.Map

/**
  * This class renderers markdown with keyid display support.
  */
class ACMarkdownRenderer extends GLMarkdownRenderer {

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
  }

}
