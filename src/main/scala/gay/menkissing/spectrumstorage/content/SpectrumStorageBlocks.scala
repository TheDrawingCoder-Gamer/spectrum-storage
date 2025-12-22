package gay.menkissing.spectrumstorage.content

import de.dafuqs.fractal.api.ItemSubGroupEvents
import de.dafuqs.spectrum.api.item_group.ItemGroupIDs
import gay.menkissing.spectrumstorage.SpectrumStorage
import gay.menkissing.spectrumstorage.content.block.BottomlessStorageBlock.{BottomlessAmphoraBlock, BottomlessBarrelBlock}
import gay.menkissing.spectrumstorage.content.block.{BottomlessShelfBlock, BottomlessStorageBlock}
import gay.menkissing.spectrumstorage.content.block.entity.{BottomlessShelfBlockEntity, BottomlessStorageBlockEntity}
import gay.menkissing.spectrumstorage.util.registry.InfoCollector
import gay.menkissing.spectrumstorage.util.registry.builder.{BlockBuilder, ItemBuilder}
import gay.menkissing.spectrumstorage.util.resources.{*, given}
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.Util
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.{Block, SoundType}
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockBehaviour

import scala.collection.mutable

object SpectrumStorageBlocks:
  private val blockItems = mutable.Map[ResourceLocation, mutable.ArrayBuffer[Item]]()

  extension (builder: BlockBuilder[?])
    def registerItemInGroup(subgroup: ResourceLocation): Block =
      val i = builder.register()
      blockItems.getOrElseUpdate(subgroup, mutable.ArrayBuffer.empty) += i.asItem()
      i
  extension (builder: ItemBuilder[?])
    def registerItemInGroup(subgroup: ResourceLocation): Item =
      val i = builder.register()
      blockItems.getOrElseUpdate(subgroup, mutable.ArrayBuffer.empty) += i.asItem()
      i

  //def makeItem(rl: ResourceLocation, item: Item): Item =
  //  blockItems += item
  //  Registry.register(BuiltInRegistries.ITEM, rl, item)

  def makeEntity[T <: BlockEntity](name: String, factory: BlockEntityType.BlockEntitySupplier[T], blocks: Block*): BlockEntityType[T] =
    val id = SpectrumStorage.locate(name)
    // Seems like null is ok here, if it's null then it won't check data fixers?
    Registry.register[BlockEntityType[?], BlockEntityType[T]](BuiltInRegistries.BLOCK_ENTITY_TYPE, id, BlockEntityType.Builder.of[T](factory, blocks*).build(null))

  val bottomlessShelf: Block =
    InfoCollector.instance.block(SpectrumStorage.locate("bottomless_shelf"),
      BottomlessShelfBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1.5f)))
                 .lang("Bottomless Shelf")
                 .item()
                 .model(gen => item => gen.withExistingParent(item, bottomlessShelf.modelLoc.withSuffix("_inventory"))).build()
                 .tag(BlockTags.MINEABLE_WITH_AXE)
                 .dropSelf()
                 .registerItemInGroup(ItemGroupIDs.SUBTAB_FUNCTIONAL)

  val bottomlessBarrel: Block =
    InfoCollector.instance.block(SpectrumStorage.locate("bottomless_barrel"),
      BottomlessBarrelBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1.5f)))
                 .lang("Bottomless Barrel")
                 .tag(BlockTags.MINEABLE_WITH_AXE)
                 .simpleItem()
                 .dropSelf()
                 .blockstate(gen => block => gen.barrelBlock(block))
                 .registerItemInGroup(ItemGroupIDs.SUBTAB_FUNCTIONAL)

  val bottomlessAmphora: Block =
    InfoCollector.instance.block(SpectrumStorage.locate("bottomless_amphora"),
      BottomlessAmphoraBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(4.0f)))
                 .lang("Bottomless Amphora")
                 .tag(BlockTags.MINEABLE_WITH_AXE)
                 .simpleItem()
                 .dropSelf()
                 .blockstate(gen => block => gen.barrelBlock(block))
                 .registerItemInGroup(ItemGroupIDs.SUBTAB_FUNCTIONAL)

  val bottomlessShelfBlockEntity: BlockEntityType[BottomlessStorageBlockEntity] =
    makeEntity("bottomless_shelf", (a, b) => BottomlessShelfBlockEntity(a, b), bottomlessShelf)
  val bottomlessBarrelBlockEntity: BlockEntityType[BottomlessStorageBlockEntity] =
    makeEntity("bottomless_barrel", (a, b) => BottomlessStorageBlockEntity.BottomlessBarrelBlockEntity(a, b), bottomlessBarrel)
  val bottomlessAmphoraBlockEntity: BlockEntityType[BottomlessStorageBlockEntity] =
    makeEntity("bottomless_amphora", (a, b) => BottomlessStorageBlockEntity.BottomlessAmphoraBlockEntity(a, b), bottomlessAmphora)


  def init(): Unit =
    BottomlessStorageBlockEntity.registerStorages()
    blockItems.foreach: (key, items) =>
      ItemSubGroupEvents.modifyEntriesEvent(key).register: entries =>
        items.foreach(entries.accept)
