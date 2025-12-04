package gay.menkissing.spectrumstorage

import gay.menkissing.spectrumstorage.content.{SpectrumStorageBlocks, SpectrumStorageItems}
import gay.menkissing.spectrumstorage.registries.{LumoLang, LumoScreens, LumoTags}
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.ResourceLocation
import org.slf4j.{Logger, LoggerFactory}

object SpectrumStorage extends ModInitializer:
  val ModId: String = "spectrumstorage"
  val Logger: Logger = LoggerFactory.getLogger("spectrumstorage")

  def locate(id: String): ResourceLocation = new ResourceLocation(ModId, id)

  override def onInitialize(): Unit =
    SpectrumStorageItems.init()
    SpectrumStorageBlocks.init()
    LumoScreens.init()
    LumoTags.init()
    LumoLang.init()
