package gay.menkissing.lumospectrum.datagen

import com.klikli_dev.modonomicon.api.datagen.book.{BookCategoryModel, BookModel}
import gay.menkissing.lumospectrum.content.block.BottomlessShelfBlock
import gay.menkissing.lumospectrum.content.{LumoSpectrumBlocks, LumoSpectrumItems}
import gay.menkissing.lumospectrum.util.registry.InfoCollector
import net.fabricmc.fabric.api.datagen.v1.provider.{FabricCodecDataProvider, FabricModelProvider}
import net.fabricmc.fabric.api.datagen.v1.{DataGeneratorEntrypoint, FabricDataGenerator, FabricDataOutput}
import net.minecraft.core.Direction
import net.minecraft.data.models.blockstates.{Condition, MultiPartGenerator, Variant, VariantProperties}
import net.minecraft.data.models.{BlockModelGenerators, ItemModelGenerators}
import net.minecraft.data.models.model.{ModelLocationUtils, ModelTemplate, ModelTemplates, TextureMapping, TextureSlot}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.properties.{BlockStateProperties, EnumProperty}
import com.klikli_dev.modonomicon.api.datagen.{BookProvider, LanguageProviderCache}
import gay.menkissing.lumospectrum.LumoSpectrum

import scala.collection.mutable


object LumoDatagen extends DataGeneratorEntrypoint:
  override def onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator): Unit =
    val pack = fabricDataGenerator.createPack()

    pack.addProvider((o: FabricDataOutput) => ModelGenerator(o))

    InfoCollector.instance.registerDataGenerators(pack)

  private case class CoolerModelSlotKey(template: ModelTemplate, str: String)

  private class ModelGenerator(output: FabricDataOutput) extends FabricModelProvider(output):
    private val coolerCache = mutable.HashMap[CoolerModelSlotKey, ResourceLocation]()

    def addShelfModelSlot(modelGens: BlockModelGenerators, generator: MultiPartGenerator, condition: Condition.TerminalCondition, rot: VariantProperties.Rotation, slotProp: EnumProperty[BottomlessShelfBlock.ShelfSlotOccupiedBy],
                          template: ModelTemplate)(propValue: BottomlessShelfBlock.ShelfSlotOccupiedBy): Unit =
      val str = "_" + propValue.getSerializedName
      val mapping = new TextureMapping()
        .put(TextureSlot.TEXTURE, TextureMapping.getBlockTexture(LumoSpectrumBlocks.bottomlessShelf, str))
      val key = CoolerModelSlotKey(template, str)
      val model = coolerCache.getOrElseUpdate(key, template
        .createWithSuffix(LumoSpectrumBlocks.bottomlessShelf, str, mapping, modelGens.modelOutput))
      generator.`with`(Condition.and(condition, Condition.condition().term(slotProp, propValue)), Variant.variant()
                                                                                                         .`with`(VariantProperties
                                                                                                           .MODEL, model)
                                                                                                         .`with`(VariantProperties
                                                                                                           .Y_ROT, rot))


    def addSlotAndRotationVariants(modelGens: BlockModelGenerators, multiPartGenerator: MultiPartGenerator, condition: Condition.TerminalCondition, rotation: VariantProperties.Rotation): Unit =
      List(
        (BottomlessShelfBlock.SHELF_SLOT_0_OCCUPIED_BY, ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_LEFT),
        (BottomlessShelfBlock.SHELF_SLOT_1_OCCUPIED_BY, ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_MID),
        (BottomlessShelfBlock.SHELF_SLOT_2_OCCUPIED_BY, ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_RIGHT),
        (BottomlessShelfBlock.SHELF_SLOT_3_OCCUPIED_BY, ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_LEFT),
        (BottomlessShelfBlock.SHELF_SLOT_4_OCCUPIED_BY, ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_MID),
        (BottomlessShelfBlock.SHELF_SLOT_5_OCCUPIED_BY, ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_RIGHT)
      ).foreach { (prop, template) =>
        val freakyFunc = addShelfModelSlot(modelGens, multiPartGenerator, condition, rotation, prop, template)
        freakyFunc(BottomlessShelfBlock.ShelfSlotOccupiedBy.Empty)
        freakyFunc(BottomlessShelfBlock.ShelfSlotOccupiedBy.Bottle)
        freakyFunc(BottomlessShelfBlock.ShelfSlotOccupiedBy.Bundle)
      }

    def generateBottomlessShelfModels(modelGenerators: BlockModelGenerators): Unit =
      import VariantProperties.Rotation

      val block = LumoSpectrumBlocks.bottomlessShelf
      val rl = ModelLocationUtils.getModelLocation(block)
      val multiPartGenerator = MultiPartGenerator.multiPart(block)
      List(
        (Direction.NORTH, Rotation.R0),
        (Direction.EAST, Rotation.R90),
        (Direction.SOUTH, Rotation.R180),
        (Direction.WEST, Rotation.R270)
      ).foreach { (dir, rot) =>
        val cond = Condition.condition().term(BlockStateProperties.HORIZONTAL_FACING, dir)
        multiPartGenerator
          .`with`(cond, Variant.variant().`with`(VariantProperties.MODEL, rl).`with`(VariantProperties.Y_ROT, rot)
                               .`with`(VariantProperties.UV_LOCK, true))
        this.addSlotAndRotationVariants(modelGenerators, multiPartGenerator, cond, rot)
      }
      modelGenerators.delegateItemModel(block, ModelLocationUtils.getModelLocation(block, "_inventory"))
      modelGenerators.blockStateOutput.accept(multiPartGenerator)

      coolerCache.clear()

    override def generateItemModels(itemModelGenerator: ItemModelGenerators): Unit =
      ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(LumoSpectrumItems.bottomlessBottle, "_base"), TextureMapping.layer0(LumoSpectrumItems.bottomlessBottle), itemModelGenerator.output)

    override def generateBlockStateModels(blockStateModelGenerator: BlockModelGenerators): Unit =
      generateBottomlessShelfModels(blockStateModelGenerator)

