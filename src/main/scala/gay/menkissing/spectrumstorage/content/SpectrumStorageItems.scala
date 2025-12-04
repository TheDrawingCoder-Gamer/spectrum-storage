package gay.menkissing.spectrumstorage.content

import gay.menkissing.spectrumstorage.SpectrumStorage
import gay.menkissing.spectrumstorage.content.item.{BottomlessBottleItem, ToolContainerItem}
import gay.menkissing.spectrumstorage.util.registry.InfoCollector
import gay.menkissing.spectrumstorage.util.registry.builder.ItemBuilder
import net.fabricmc.fabric.api.itemgroup.v1.{FabricItemGroup, ItemGroupEvents}
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.{ResourceKey, ResourceLocation}
import net.minecraft.world.item.{CreativeModeTab, Item, ItemStack}
import de.dafuqs.fractal.api.{ItemSubGroup, ItemSubGroupEvents}
import de.dafuqs.spectrum.api.item_group.ItemGroupIDs

import scala.collection.mutable

object SpectrumStorageItems:
  private val items = mutable.Map[ResourceLocation, mutable.ArrayBuffer[Item]]()

  extension (builder: ItemBuilder[?])
    def addToSubGroup(sub: ResourceLocation): Item =
      val i = builder.register()
      items.getOrElseUpdate(sub, mutable.ArrayBuffer.empty) += i
      i

  val bottomlessBottle: Item =
    InfoCollector.instance.item(SpectrumStorage.locate("bottomless_bottle"), new BottomlessBottleItem(Item.Properties().stacksTo(1)))
                 .lang("Bottomless Bottle")
                 .tooltip("empty", "Empty")
                 .tooltip("usage_pickup", "Use to pickup")
                 .tooltip("usage_place", "Sneak-use to place")
                 .tooltip("count_mb", "%1$s mB / %2$s buckets")
                 .addToSubGroup(ItemGroupIDs.SUBTAB_EQUIPMENT)

  val toolContainer: Item =
    InfoCollector.instance.item(SpectrumStorage.locate("tool_container"), new ToolContainerItem(Item.Properties().stacksTo(1)))
                 .lang("Tool Container")
                 .defaultModel()
                 .addToSubGroup(ItemGroupIDs.SUBTAB_EQUIPMENT)

  def init(): Unit =
    BottomlessBottleItem.registerCauldronInteractions()
    items.foreach: (subgroup, items) =>
      ItemSubGroupEvents.modifyEntriesEvent(subgroup).register: entries =>
        items.foreach(entries.accept)

    FluidStorage.ITEM.registerForItems((stack: ItemStack, context: ContainerItemContext) => BottomlessBottleItem.BottomlessBottleContents.BottomlessBottleStorage(context),
      bottomlessBottle)