package gay.menkissing.spectrumstorage.datagen

import gay.menkissing.spectrumstorage.content.block.BottomlessShelfBlock
import gay.menkissing.spectrumstorage.util.VariantHelpers, VariantHelpers.conditionWhen
import gay.menkissing.spectrumstorage.util.registry.provider.generators.LumoBlockStateGenerator
import net.minecraft.core.Direction
import net.minecraft.data.models.blockstates.{Condition, MultiPartGenerator, VariantProperties}
import net.minecraft.data.models.model.{ModelLocationUtils, ModelTemplate, ModelTemplates, TextureMapping, TextureSlot}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.properties.{BlockStateProperties, EnumProperty}

import scala.collection.mutable

object ComplexBlockstateDatagen:


  object BottomlessShelf:
    private case class CoolerModelSlotKey(template: ModelTemplate, str: String)

    private class BabyGenerator(val gen: LumoBlockStateGenerator, val block: Block):
      val cache = mutable.HashMap[CoolerModelSlotKey, ResourceLocation]()

      def addShelfModelSlot(generator: MultiPartGenerator, condition: Condition.TerminalCondition, rot: VariantProperties.Rotation, slotProp: EnumProperty[BottomlessShelfBlock.ShelfSlotOccupiedBy],
                                  template: ModelTemplate)(propValue: BottomlessShelfBlock.ShelfSlotOccupiedBy): Unit =
        val str = "_" + propValue.getSerializedName
        val mapping = new TextureMapping()
          .put(TextureSlot.TEXTURE, TextureMapping.getBlockTexture(block, str))
        val key = CoolerModelSlotKey(template, str)
        val model = cache.getOrElseUpdate(key, template.createWithSuffix(block, str, mapping, gen.models.addRaw))
        generator.`with`(Condition.and(condition, Condition.condition().term(slotProp, propValue)), VariantHelpers.Builder(model).yRot(rot).build())

      def addSlotAndRotationVariants(generator: MultiPartGenerator, condition: Condition.TerminalCondition, rotation: VariantProperties.Rotation): Unit =
        List(
          BottomlessShelfBlock.SHELF_SLOT_0_OCCUPIED_BY -> ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_LEFT,
          BottomlessShelfBlock.SHELF_SLOT_1_OCCUPIED_BY -> ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_MID,
          BottomlessShelfBlock.SHELF_SLOT_2_OCCUPIED_BY -> ModelTemplates.CHISELED_BOOKSHELF_SLOT_TOP_RIGHT,
          BottomlessShelfBlock.SHELF_SLOT_3_OCCUPIED_BY -> ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_LEFT,
          BottomlessShelfBlock.SHELF_SLOT_4_OCCUPIED_BY -> ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_MID,
          BottomlessShelfBlock.SHELF_SLOT_5_OCCUPIED_BY -> ModelTemplates.CHISELED_BOOKSHELF_SLOT_BOTTOM_RIGHT
        ).foreach: (prop, template) =>
          val freakyFunc = addShelfModelSlot(generator, condition, rotation, prop, template)
          freakyFunc(BottomlessShelfBlock.ShelfSlotOccupiedBy.Empty)
          freakyFunc(BottomlessShelfBlock.ShelfSlotOccupiedBy.Bottle)
          freakyFunc(BottomlessShelfBlock.ShelfSlotOccupiedBy.Bundle)
      
      def generateBottomlessShelfModels(): Unit =
        import VariantProperties.Rotation
        
        val rl = ModelLocationUtils.getModelLocation(block)
        val multiPartGenerator = MultiPartGenerator.multiPart(block)
        List(
          Direction.NORTH -> Rotation.R0,
          Direction.EAST -> Rotation.R90,
          Direction.SOUTH -> Rotation.R180,
          Direction.WEST -> Rotation.R270
        ).foreach: (dir, rot) =>
          val cond = BlockStateProperties.HORIZONTAL_FACING.conditionWhen(dir)
          multiPartGenerator.`with`(cond, VariantHelpers.Builder(rl).yRot(rot).uvLock(true).build())
          this.addSlotAndRotationVariants(multiPartGenerator, cond, rot)
          
        gen.blockStates(block) = multiPartGenerator

    val genBottomlessShelf: LumoBlockStateGenerator => Block => Unit =
      gen => block =>
        val generator = new BabyGenerator(gen, block)
        generator.generateBottomlessShelfModels()


