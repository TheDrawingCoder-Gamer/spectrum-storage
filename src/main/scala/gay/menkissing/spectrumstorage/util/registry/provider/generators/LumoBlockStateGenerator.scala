/*
 * Copyright (c) BulbyVR/TheDrawingCoder-Gamer 2025.
 *
 * This file is part of Lumomancy.
 *
 * Lumomancy is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the license, or (at your option) any later version.
 *
 * Lumomancy is distributed in the hopes it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License along with Lumomancy. If not,
 *  see <https://www.gnu.org/licenses/>
 */

package gay.menkissing.spectrumstorage.util.registry.provider.generators

import com.google.common.base.Supplier
import com.google.gson.{JsonElement, JsonObject}
import gay.menkissing.spectrumstorage.util.VariantHelpers
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.minecraft.core.Direction.Axis
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.{CachedOutput, DataProvider, PackOutput}
import net.minecraft.data.models.BlockModelGenerators
import net.minecraft.data.models.blockstates.{BlockStateGenerator, Condition, MultiPartGenerator, MultiVariantGenerator, PropertyDispatch, Variant, VariantProperties}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.{Block, ButtonBlock, DoorBlock, FenceGateBlock, PipeBlock, PressurePlateBlock, RotatedPillarBlock, SlabBlock, StairBlock, TrapDoorBlock}
import gay.menkissing.spectrumstorage.util.resources.{*, given}
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.{AttachFace, BlockStateProperties, DoorHingeSide, DoubleBlockHalf, Half, SlabType, StairsShape}

import java.util.concurrent.CompletableFuture
import scala.collection.mutable

abstract class LumoBlockStateGenerator(val output: FabricDataOutput) extends DataProvider:
  val models = new LumoModelProvider(output)
  val itemModels = new LumoItemModelProvider(output)

  val blockStates: mutable.Map[Block, BlockStateGenerator] = mutable.LinkedHashMap()


  private def rotationOfInt(n: Int): VariantProperties.Rotation =
    VariantProperties.Rotation.values()((n % 360) / 90)

  private def extend(rl: ResourceLocation, suffix: String): ResourceLocation =
    ResourceLocationExt.fromNamespaceAndPath(rl.getNamespace, rl.getPath + suffix)

  private def blockTexture(block: Block): ResourceLocation =
    val key = BuiltInRegistries.BLOCK.getKey(block)
    ResourceLocationExt.fromNamespaceAndPath(key.getNamespace, "block/" + key.getPath)

  def createColumnWithFacing(): PropertyDispatch =
    import VariantProperties.Rotation
    PropertyDispatch.property(BlockStateProperties.FACING)
                    .select(Direction.DOWN, Variant.variant().`with`(VariantProperties.X_ROT, Rotation.R180))
                    .select(Direction.UP, Variant.variant())
                    .select(Direction.NORTH, Variant.variant().`with`(VariantProperties.X_ROT, Rotation.R90))
                    .select(Direction.SOUTH, Variant.variant().`with`(VariantProperties.X_ROT, Rotation.R90)
                                                    .`with`(VariantProperties.Y_ROT, Rotation.R180))
                    .select(Direction.WEST, Variant.variant().`with`(VariantProperties.X_ROT, Rotation.R90)
                                                   .`with`(VariantProperties.Y_ROT, Rotation.R270))
                    .select(Direction.EAST, Variant.variant().`with`(VariantProperties.X_ROT, Rotation.R90)
                                                   .`with`(VariantProperties.Y_ROT, Rotation.R90))
    
  def barrelBlock(block: Block): Unit =
    val sideTexture = blockTexture(block).withSuffix("_side")
    val topTexture = blockTexture(block).withSuffix("_top")
    val bottomTexture = blockTexture(block).withSuffix("_bottom")
    barrelBlock(block, sideTexture, topTexture, bottomTexture)
  def barrelBlock(block: Block, side: ResourceLocation, top: ResourceLocation, bottom: ResourceLocation): Unit =
    val closeModel = models.cubeBottomTop(block, side, bottom, top)
    val openModel = models.cubeBottomTop(block.modelLoc.withSuffix("_open"), side, bottom, top.withSuffix("_open"))
    blockStates(block) =
      MultiVariantGenerator.multiVariant(block).`with`(createColumnWithFacing()).`with`:
        PropertyDispatch.property(BlockStateProperties.OPEN)
          .select(false, Variant.variant().`with`(VariantProperties.MODEL, closeModel.location))
          .select(true, Variant.variant().`with`(VariantProperties.MODEL, openModel.location))

  def crossBlock(block: Block, texture: ResourceLocation): Unit =
    val model = models.cross(block, texture)
    simpleBlock(block, model)

  def signBlock(block: Block, wall: Block, particle: ResourceLocation): Unit =
    val model = models.sign(block, particle)
    simpleBlock(block, model)
    simpleBlock(wall, model)


  def stairsBlock(block: Block, texture: ResourceLocation): Unit =
    stairsBlock(block, texture, texture, texture)

  def stairsBlock(block: Block, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): Unit =
    val straight = models.stairs(block, side, bottom, top)
    val inner = models.stairsInner(block.modelLoc.withSuffix("_inner"), side, bottom, top)
    val outer = models.stairsOuter(block.modelLoc.withSuffix("_outer"), side, bottom, top)

    stairsBlock(block, straight, inner, outer)



  def stairsBlock(block: Block, straight: LumoModelFile, inner: LumoModelFile, outer: LumoModelFile): Unit =
    blockStates(block) =
      MultiVariantGenerator.multiVariant(block).`with`:
        PropertyDispatch.properties(StairBlock.SHAPE, StairBlock.FACING, StairBlock.HALF).generate: (shape, facing, half) =>
          var yRot = facing.getClockWise.toYRot.toInt
          if shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT then
            yRot += 270

          if shape != StairsShape.STRAIGHT && half == Half.TOP then
            yRot += 90

          yRot %= 360
          val uvlock = yRot != 0 || half == Half.TOP
          VariantHelpers.ofModel {
            shape match
              case StairsShape.STRAIGHT => straight.location
              case StairsShape.INNER_LEFT | StairsShape.INNER_RIGHT => inner.location
              case _ => outer.location
          }
            .`with`(VariantProperties.X_ROT, if half == Half.BOTTOM then VariantProperties.Rotation.R0 else VariantProperties.Rotation.R180)
            .`with`(VariantProperties.Y_ROT, rotationOfInt(yRot))
            .`with`(VariantProperties.UV_LOCK, uvlock)


  def slabBlock(block: Block, doubleSlab: ResourceLocation, texture: ResourceLocation): Unit =
    slabBlock(block, doubleSlab, texture, texture, texture)

  def slabBlock(block: Block, doubleSlab: ResourceLocation, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): Unit =
    slabBlock(
      block,
      models.slab(block, side, bottom, top),
      models.slabTop(block.modelLoc.withSuffix("_top"), side, bottom, top),
      ExistingModelFile(doubleSlab)
    )

  def slabBlock(block: Block, bottom: LumoModelFile, top: LumoModelFile, doubleSlab: LumoModelFile): Unit =
    blockStates(block) =
      MultiVariantGenerator.multiVariant(block).`with`:
        PropertyDispatch.property(SlabBlock.TYPE)
                        .select(SlabType.BOTTOM, VariantHelpers.ofModel(bottom.location))
                        .select(SlabType.TOP, VariantHelpers.ofModel(top.location))
                        .select(SlabType.DOUBLE, VariantHelpers.ofModel(doubleSlab.location))


  def buttonBlock(button: Block, texture: ResourceLocation): Unit =
    val normal = models.button(button, texture)
    val pressed = models.buttonPressed(button.modelLoc.withSuffix("_pressed"), texture)

    ButtonBlock.POWERED
    blockStates(button) =
      MultiVariantGenerator.multiVariant(button).`with`:
        PropertyDispatch.properties(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.ATTACH_FACE, BlockStateProperties.POWERED).generate: (facing, face, powered) =>
          VariantHelpers.Builder(if powered then pressed.location else normal.location)
                        .xRot {
                          face match
                            case AttachFace.FLOOR => 0
                            case AttachFace.WALL => 90
                            case AttachFace.CEILING => 180
                        }
                        .yRot {
                          if face == AttachFace.CEILING then
                            facing.toYRot.toInt
                          else
                            facing.getOpposite.toYRot.toInt
                        }
                        .uvLock(face == AttachFace.WALL)
                        .build()

  def pressurePlateBlock(block: Block, texture: ResourceLocation): Unit =
    val normal = models.pressurePlate(block, texture)
    val pressed = models.pressurePlateDown(block.modelLoc.withSuffix("_down"), texture)

    blockStates(block) =
      MultiVariantGenerator.multiVariant(block).`with`:
        PropertyDispatch.property(PressurePlateBlock.POWERED)
                        .select(false, Variant.variant().`with`(VariantProperties.MODEL, normal.location))
                        .select(true, Variant.variant().`with`(VariantProperties.MODEL, pressed.location))

  def simpleBlock(block: Block, model: LumoModelFile): Unit =
    blockStates(block) = MultiVariantGenerator.multiVariant(block, Variant.variant().`with`(VariantProperties.MODEL, model.location))


  def simpleBlock(block: Block): Unit =
    simpleBlock(block, models.cubeAll(block.modelLoc, blockTexture(block)))

  def axisBlock(block: Block, model: LumoModelFile): Unit =
    blockStates(block) =
      MultiVariantGenerator.multiVariant(block).`with`:
        PropertyDispatch.property(RotatedPillarBlock.AXIS)
                        .select(Axis.Y, VariantHelpers.ofModel(model.location))
                        .select(Axis.Z, VariantHelpers.Builder(model.location).xRot(90).build())
                        .select(Axis.X, VariantHelpers.Builder(model.location).xRot(90).yRot(90).build())

  def axisBlock(block: Block, side: ResourceLocation, ends: ResourceLocation): Unit =
    val model = models.cubeColumn(block, side, ends)
    axisBlock(block, model)

  def axisBlockWithHorizontal(block: Block, vertical: LumoModelFile, horizontal: LumoModelFile): Unit =
    blockStates(block) = MultiVariantGenerator.multiVariant(block)
                                              .`with`(PropertyDispatch.property(RotatedPillarBlock.AXIS)
                                                                      .select(Axis.Y, Variant.variant().`with`(VariantProperties.MODEL, vertical.location))
                                              .select(Axis.Z, Variant.variant().`with`(VariantProperties.MODEL, horizontal.location)
                                              .`with`(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                                              .select(Axis.X, Variant.variant().`with`(VariantProperties.MODEL, horizontal.location)
                                              .`with`(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                                              .`with`(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)))

  def axisBlockWithHorizontal(block: Block, side: ResourceLocation, ends: ResourceLocation): Unit =
    axisBlockWithHorizontal(block,
      models.cubeColumn(block.modelLoc, side, ends),
      models.cubeColumnHorizontal(extend(block.modelLoc,  "_horizontal"), side, ends)
    )

  def logBlock(block: Block): Unit =
    axisBlockWithHorizontal(block,
      blockTexture(block),
      extend(blockTexture(block), "_top"))

  def woodBlock(block: Block, base: ResourceLocation): Unit =
    axisBlock(block, base, base)

  def fenceGateBlock(block: Block, texture: ResourceLocation): Unit =
    val gate = models.fenceGate(block, texture)
    val gateOpen = models.fenceGateOpen(block.modelLoc.withSuffix("_open"), texture)
    val gateWall = models.fenceGateWall(block.modelLoc.withSuffix("_wall"), texture)
    val gateWallOpen = models.fenceGateWallOpen(block.modelLoc.withSuffix("_wall_open"), texture)
    fenceGateBlock(block, gate, gateOpen, gateWall, gateWallOpen)

  def fenceGateBlock(block: Block, gate: LumoModelFile, gateOpen: LumoModelFile, gateWall: LumoModelFile, gateWallOpen: LumoModelFile): Unit =
    blockStates(block) = MultiVariantGenerator.multiVariant(block).`with` {
      PropertyDispatch.properties(FenceGateBlock.IN_WALL, FenceGateBlock.OPEN, BlockStateProperties.HORIZONTAL_FACING).generate { (inWall, isOpen, facing) =>
        val model =
          if inWall && isOpen then
            gateWallOpen
          else if inWall then
            gateWall
          else if isOpen then
            gateOpen
          else
            gate

        Variant.variant().`with`(VariantProperties.MODEL, model.location)
               .`with`(VariantProperties.UV_LOCK, true)
               .`with`(VariantProperties.Y_ROT, VariantProperties.Rotation.values()(facing.toYRot.toInt / 90))



      }
    }

  def trapdoorBlock(block: Block, orientable: Boolean = true): Unit =
    val tex = blockTexture(block)
    val top = if orientable then models.trapdoorOrientableTop(block.modelLoc.withSuffix("_top"), tex) else models.trapdoorTop(block.modelLoc.withSuffix("_top"), tex)
    val bottom = if orientable then models.trapdoorOrientableBottom(block.modelLoc.withSuffix("_bottom"), tex) else models.trapdoorTop(block.modelLoc.withSuffix("_bottom"), tex)
    val open = if orientable then models.trapdoorOrientableOpen(block.modelLoc.withSuffix("_open"), tex) else models.trapdoorOpen(block.modelLoc.withSuffix("_open"), tex)
    trapdoorBlock(block, bottom, top, open, orientable)


  def trapdoorBlock(block: Block, bottom: LumoModelFile, top: LumoModelFile, open: LumoModelFile, orientable: Boolean): Unit =
    blockStates(block) = MultiVariantGenerator.multiVariant(block).`with` {
      PropertyDispatch.properties(TrapDoorBlock.HALF, TrapDoorBlock.OPEN, BlockStateProperties.HORIZONTAL_FACING)
                      .generate { (half, isOpen, facing) =>
                        var xRot = 0
                        var yRot = facing.toYRot.toInt + 180
                        if orientable && isOpen && half == Half.TOP then
                          xRot += 180
                          yRot += 180

                        if !orientable && !isOpen then
                          yRot = 0

                        yRot %= 360

                        Variant.variant()
                               .`with`(VariantProperties.MODEL, if isOpen then open.location else if half == Half.TOP then top.location else bottom.location)
                               .`with`(VariantProperties.X_ROT, VariantProperties.Rotation.values()(xRot / 90))
                               .`with`(VariantProperties.Y_ROT, VariantProperties.Rotation.values()(yRot / 90))
                      }
    }


  def fenceBlock(block: Block, texture: ResourceLocation): Unit =
    fourWayBlock(block,
      models.fencePost(block.modelLoc.withSuffix("_post"), texture),
      models.fenceSide(block.modelLoc.withSuffix("_side"), texture))

  def fourWayBlock(block: Block, post: LumoModelFile, side: LumoModelFile): Unit =
    val builder = MultiPartGenerator.multiPart(block).`with`(VariantHelpers.ofModel(post.location))
    fourWayMultipart(builder, side)

  def fourWayMultipart(builder: MultiPartGenerator, side: LumoModelFile): Unit =
    PipeBlock.PROPERTY_BY_DIRECTION.entrySet().forEach { e =>
      val dir = e.getKey
      if dir.getAxis.isHorizontal then
        builder.`with`(Condition.condition().term(e.getValue, true), VariantHelpers.Builder(side.location).xRot(dir.toYRot.toInt + 180).uvLock(true).build())
    }
    blockStates(builder.getBlock) = builder

  def doorBlock(block: Block): Unit =
    doorBlock(block, block.modelLoc.withSuffix("_bottom"), block.modelLoc.withSuffix("_top"))

  def doorBlock(block: Block, bottom: ResourceLocation, top: ResourceLocation): Unit =
    val bottomLeft = models.doorBottomLeft(block.modelLoc.withSuffix("_bottom_left"), bottom, top)
    val bottomLeftOpen = models.doorBottomLeftOpen(block.modelLoc.withSuffix("_bottom_left_open"), bottom, top)
    val bottomRight = models.doorBottomRight(block.modelLoc.withSuffix("_bottom_right"), bottom, top)
    val bottomRightOpen = models.doorBottomRightOpen(block.modelLoc.withSuffix("_bottom_right_open"), bottom, top)
    val topLeft = models.doorTopLeft(block.modelLoc.withSuffix("_top_left"), bottom, top)
    val topLeftOpen = models.doorTopLeftOpen(block.modelLoc.withSuffix("_top_left_open"), bottom, top)
    val topRight = models.doorTopRight(block.modelLoc.withSuffix("_top_right"), bottom, top)
    val topRightOpen = models.doorTopRightOpen(block.modelLoc.withSuffix("_top_right_open"), bottom, top)

    doorBlock(block, bottomLeft, bottomLeftOpen, bottomRight, bottomRightOpen, topLeft, topLeftOpen, topRight, topRightOpen)

  def doorBlock(block: Block,
                bottomLeft: LumoModelFile,
                bottomLeftOpen: LumoModelFile,
                bottomRight: LumoModelFile,
                bottomRightOpen: LumoModelFile,
                topLeft: LumoModelFile,
                topLeftOpen: LumoModelFile,
                topRight: LumoModelFile,
                topRightOpen: LumoModelFile): Unit =
    blockStates(block) = MultiVariantGenerator.multiVariant(block).`with`:
      PropertyDispatch.properties(DoorBlock.FACING, DoorBlock.HINGE, DoorBlock.OPEN, DoorBlock.HALF).generate: (facing, hinge, open, half) =>
        var yRot = facing.toYRot.toInt + 90
        val right = hinge == DoorHingeSide.RIGHT
        val lower = half == DoubleBlockHalf.LOWER

        if open then
          yRot += 90

        if right && open then
          yRot += 180

        val model =
          if open then
            if lower then
              if right then
                bottomRightOpen.location
              else
                bottomLeftOpen.location
            else
              if right then
                topRightOpen.location
              else
                topLeftOpen.location
          else
            if lower then
              if right then
                bottomRight.location
              else
                bottomLeft.location
            else
              if right then
                topRight.location
              else
                topLeft.location

        VariantHelpers.Builder(model).yRot(yRot).build()

  private def saveBlockState(cache: CachedOutput, stateJson: JsonElement, owner: Block): CompletableFuture[?] =
    val blockName = owner.location
    val outputPath = this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                         .resolve(blockName.getNamespace).resolve("blockstates").resolve(blockName.getPath + ".json")
    DataProvider.saveStable(cache, stateJson, outputPath)

  override def run(output: CachedOutput): CompletableFuture[?] =
    registerStates()
    val freakyOne = Seq(models.generateAll(output), itemModels.generateAll(output))
    val freakies = blockStates.map { (k, v) =>
      saveBlockState(output, v.get(), k)
    }.toSeq
    CompletableFuture.allOf((freakyOne ++ freakies)*)

  override def getName: String =
    "Lumomancy block state generator"

  def registerStates(): Unit