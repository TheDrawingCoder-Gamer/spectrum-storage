package gay.menkissing.lumospectrum.content

import gay.menkissing.lumospectrum.LumoSpectrum
import gay.menkissing.lumospectrum.content.block.BottomlessShelfBlock
import gay.menkissing.lumospectrum.content.block.entity.BottomlessShelfBlockEntity
import gay.menkissing.lumospectrum.util.registry.InfoCollector
import gay.menkissing.lumospectrum.util.registry.builder.{BlockBuilder, ItemBuilder}
import gay.menkissing.lumospectrum.util.resources.{*, given}
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

object LumoSpectrumBlocks:
  private val blockItems = mutable.ListBuffer[Item]()

  extension (builder: BlockBuilder[?])
    def registerItem(): Block =
      val i = builder.register()
      blockItems += i.asItem()
      i
  extension (builder: ItemBuilder[?])
    def registerItem(): Item =
      val i = builder.register()
      blockItems += i
      i

  def makeItem(rl: ResourceLocation, item: Item): Item =
    blockItems += item
    Registry.register(BuiltInRegistries.ITEM, rl, item)

  def makeEntity[T <: BlockEntity](name: String, factory: BlockEntityType.BlockEntitySupplier[T], blocks: Block*): BlockEntityType[T] =
    val id = LumoSpectrum.locate(name)
    // Seems like null is ok here, if it's null then it won't check data fixers?
    Registry.register[BlockEntityType[?], BlockEntityType[T]](BuiltInRegistries.BLOCK_ENTITY_TYPE, id, BlockEntityType.Builder.of[T](factory, blocks*).build(null))

  val bottomlessShelf: Block =
    InfoCollector.instance.block(LumoSpectrum.locate("bottomless_shelf"),
      BottomlessShelfBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1.5f)))
                 .lang("Bottomless Shelf")
                 .item()
                 .model(gen => item => gen.withExistingParent(item, bottomlessShelf.modelLoc.withSuffix("_inventory"))).build()
                 .tag(BlockTags.MINEABLE_WITH_AXE)
                 .dropSelf()
                 .registerItem()

  val bottomlessShelfBlockEntity: BlockEntityType[BottomlessShelfBlockEntity] =
    makeEntity("bottomless_shelf", (a, b) => BottomlessShelfBlockEntity(a, b), bottomlessShelf)

  def init(): Unit =
    BottomlessShelfBlockEntity.registerStorages()
    ItemGroupEvents.modifyEntriesEvent(LumoSpectrumItems.itemGroupKey).register: group =>
      blockItems.foreach(group.accept)
