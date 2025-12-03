package gay.menkissing.lumospectrum.content

import gay.menkissing.lumospectrum.LumoSpectrum
import gay.menkissing.lumospectrum.content.item.{BottomlessBottleItem, ToolContainerItem}
import gay.menkissing.lumospectrum.util.registry.InfoCollector
import gay.menkissing.lumospectrum.util.registry.builder.ItemBuilder
import net.fabricmc.fabric.api.itemgroup.v1.{FabricItemGroup, ItemGroupEvents}
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.{ResourceKey, ResourceLocation}
import net.minecraft.world.item.{CreativeModeTab, Item, ItemStack}

import scala.collection.mutable

object LumoSpectrumItems:
  private val items: mutable.ListBuffer[Item] = mutable.ListBuffer()

  extension (builder: ItemBuilder[?])
    def make(): Item =
      val i = builder.register()
      items.append(i)
      i

  val itemGroupKey: ResourceKey[CreativeModeTab] = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), LumoSpectrum.locate("creative_tab"))
  val itemGroup: CreativeModeTab = FabricItemGroup.builder()
                                                  .icon(() => new ItemStack(bottomlessBottle))
                                                  .title(Component.translatable("itemGroup.lumospectrum"))
                                                  .build()

  val bottomlessBottle: Item =
    InfoCollector.instance.item(LumoSpectrum.locate("bottomless_bottle"), new BottomlessBottleItem(Item.Properties().stacksTo(1)))
                 .lang("Bottomless Bottle")
                 .tooltip("empty", "Empty")
                 .tooltip("usage_pickup", "Use to pickup")
                 .tooltip("usage_place", "Sneak-use to place")
                 .tooltip("count_mb", "%1$s mB / %2$s buckets")
                 .make()

  val toolContainer: Item =
    InfoCollector.instance.item(LumoSpectrum.locate("tool_container"), new ToolContainerItem(Item.Properties().stacksTo(1)))
                 .lang("Tool Container")
                 .defaultModel()
                 .make()

  def init(): Unit =
    Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, itemGroupKey, itemGroup)
    BottomlessBottleItem.registerCauldronInteractions()
    ItemGroupEvents.modifyEntriesEvent(itemGroupKey).register: group =>
      items.foreach(group.accept)

    FluidStorage.ITEM.registerForItems((stack: ItemStack, context: ContainerItemContext) => BottomlessBottleItem.BottomlessBottleContents.BottomlessBottleStorage(context),
      bottomlessBottle)