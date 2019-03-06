package cn.academy.misc.media

import java.io.{File, FileInputStream, IOException, InputStreamReader}
import java.net.{URL, URLDecoder}
import java.nio.file.{Files, StandardCopyOption}

import cn.academy.{AcademyCraft, Resources}
import javax.imageio.ImageIO
import cn.lambdalib2.registry.StateEventCallback
import cn.lambdalib2.util.ResourceUtils
import com.jcraft.jorbis.VorbisFile
import com.typesafe.config.ConfigFactory
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.{DynamicTexture, SimpleTexture}
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.translation.I18n
import net.minecraftforge.common.config.Property
import net.minecraftforge.fml.common.event.FMLInitializationEvent

private object MediaManagerInit {
  import scala.collection.JavaConversions._

  val missingCover = Resources.getTexture("guis/icons/icon_nomedia")

  @StateEventCallback
  def init(ev: FMLInitializationEvent) = {
    runSide match {
      case Side.CLIENT => loadClient()
      case Side.SERVER => loadServer()
    }
  }

  @SideOnly(Side.CLIENT)
  def loadClient() = {
    val conf = readDefaultConfig()
    val missingCoverTexture = new SimpleTexture(missingCover)

    val path = checkPath(rootFolder)

    // Copy the readme_template.txt to the folder.
    {
      val dest = new File(path, "README.txt").toPath
      try {
        Files.copy(ResourceUtils.getResourceStream(
          Resources.res("media/readme_template.txt")),
          dest, StandardCopyOption.REPLACE_EXISTING)
      } catch {
        case _: IOException => AcademyCraft.log.error("Can't copy media readme file.")
      }
    }

    // Parse default medias.
    for (id <- conf.getStringList("default_medias")) try {
        val dst = new File(path, id + ".ogg").toPath
        Files.copy(ResourceUtils.getResourceStream(
          Resources.res("media/source/" + id + ".ogg")),
          dst, StandardCopyOption.REPLACE_EXISTING)

        MediaManager.register(newInternal(id, dst.toUri.toURL))
      } catch {
        case _: IOException => AcademyCraft.log.error("Can't copy media file " + id + ".")
      }

    // Parse external medias.
    val coverPath = checkPath(new File(path, "cover"))
    val sourcePath = checkPath(new File(path, "source"))

    sourcePath.listFiles
      .filter(_.getName.endsWith(".ogg"))
      .foreach(sound => {
        val id = sound.getName.takeWhile(_ != '.')
        newExternal(id, sound.toURI.toURL) match {
          case Some(media) =>
            val cover = media.cover

            // Manually override the texture
            val coverFile = new File(coverPath, id + ".png")
            try {
              val textureManager = Minecraft.getMinecraft.getTextureManager
              if (coverFile.isFile) {
                val image = ImageIO.read(new FileInputStream(coverFile))
                textureManager.loadTexture(cover, new DynamicTexture(image))
              } else {
                textureManager.loadTexture(cover, missingCoverTexture)
              }
            } catch {
              case _: IOException => // Ignore
            }

            MediaManager.register(media)
          case _ =>
        }
      })

  }

  def loadServer() = {
    val conf = readDefaultConfig()
    for (id <- conf.getStringList("default_medias")) {
      MediaManager.register(newInternal(id, null))
    }
  }

  def checkPath(file: File): File = {
    if (!file.exists) {
      require(file.mkdirs())
    }
    file
  }

  def calculateLength(url: URL): Option[Float] = {
    if (url == null) {
      Some(0)
    } else try {
      val path = URLDecoder.decode(url.getFile, "utf-8")
      val vf = new VorbisFile(path)
      val ret = vf.time_total(-1)
      vf.close()

      Some(ret)
    } catch {
      case _: Exception => None
    }
  }

  def runSide = FMLCommonHandler.instance.getSide

  def newExternal(id: String, url: URL): Option[Media] = calculateLength(url).map(length => {
    new Media(true, id, url, length) {
      override def name: String = propExternalName().getString
      override def desc: String = propExternalDesc().getString
    }
  })

  def newInternal(id: String, url: URL): Media = new Media(false, id, url, calculateLength(url).get) {
    override def name: String = I18n.translateToLocal("ac.media."+id+".name")
    override def desc: String = I18n.translateToLocal("ac.media."+id+".desc")
  }

  def rootFolder: File = runSide match {
    case Side.SERVER => new File("/")
    case Side.CLIENT => _rootFolder_c
  }

  def readDefaultConfig() = {
    val rdr = new InputStreamReader(ResourceUtils.getResourceStream(Resources.res("media/default.conf")))
    ConfigFactory.parseReader(rdr)
  }

  @SideOnly(Side.CLIENT)
  def _rootFolder_c: File = new File(Minecraft.getMinecraft.gameDir, "acmedia")

}

object MediaManager {
  import collection.mutable

  private val medias: mutable.Map[String, Media] = mutable.Map()
  private val internalMediasList = mutable.ArrayBuffer[Media]()
  private val mediasList = mutable.ArrayBuffer[Media]()

  def register(media: Media) = {
    medias += (media.id -> media)
    mediasList += media
    if (!media.external) {
      internalMediasList += media
    }
  }

  def get(id: String): Media = medias(id)

  def internalMedias: Seq[Media] = internalMediasList

  def allMedias: Seq[Media] = mediasList

}

abstract class Media(val external: Boolean,
            val id: String,
            val source: URL,
            val cover: ResourceLocation,
            val lengthSecs: Float) {

  def this(external: Boolean, id: String, source: URL, length: Float) = this(external,
    id, source, new ResourceLocation("academy:media/cover/" + id + ".png"), length)

  def propExternalName(): Property = {
    AcademyCraft.config.get("media", id + "_name", id)
  }

  def propExternalDesc(): Property = {
    AcademyCraft.config.get("media", id + "_desc", id)
  }

  def name: String
  def desc: String

  def displayLength: String = MediaBackend.getDisplayTime(lengthSecs)

}
