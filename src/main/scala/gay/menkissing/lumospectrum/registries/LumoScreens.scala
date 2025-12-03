package gay.menkissing.lumospectrum.registries

import com.mojang.serialization.Codec
import gay.menkissing.lumospectrum.LumoSpectrum
import gay.menkissing.lumospectrum.screen.ToolContainerMenu
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.inventory.MenuType

object LumoScreens:
  val toolContainer: ExtendedScreenHandlerType[ToolContainerMenu] = new ExtendedScreenHandlerType[ToolContainerMenu](ToolContainerMenu.fromNetwork)
  Registry.register(BuiltInRegistries.MENU, LumoSpectrum.locate("tool_container"), toolContainer)

  def init(): Unit = ()
