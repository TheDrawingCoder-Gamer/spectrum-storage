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

package gay.menkissing.lumospectrum.util.registry.provider.generators

import com.google.gson.JsonElement
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.{CachedOutput, DataProvider, PackOutput}
import net.minecraft.data.models.blockstates.{BlockStateGenerator, MultiVariantGenerator, Variant, VariantProperties, VariantProperty}
import net.minecraft.data.models.BlockModelGenerators
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block

import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import scala.collection.mutable
import gay.menkissing.lumospectrum.util.resources.{given, *}

class LumoModelProvider(val output: FabricDataOutput):
  val generatedModels: mutable.Map[ResourceLocation, LumoModelBuilder] = mutable.HashMap()

  def blockModelLoc(block: Block): ResourceLocation =
    val name = BuiltInRegistries.BLOCK.getKey(block)
    new ResourceLocation(name.getNamespace, "block/" + name.getPath)

  def blockTexture(block: Block): ResourceLocation =
    val name = BuiltInRegistries.BLOCK.getKey(block)
    new ResourceLocation(name.getNamespace, "block/" + name.getPath)

  def getBuilder[T: ModelResourceLike](path: T): LumoModelBuilder =
    generatedModels.getOrElseUpdate(path.modelLoc, LumoModelBuilder(path.modelLoc))

  def flatItem[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    withExistingParent(name, new ResourceLocation("minecraft", "item/generated")).texture("layer0", texture)

  def withExistingParent[T: ModelResourceLike](name: T, parent: ResourceLocation): LumoModelBuilder =
    getBuilder(name).parent(parent)

  def withExistingParent[T: ModelResourceLike](name: T, parent: String): LumoModelBuilder =
    withExistingParent(name, new ResourceLocation("minecraft", parent))

  def singleTexture[T: ModelResourceLike](name: T, parent: String, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, new ResourceLocation("minecraft", parent), texture)

  def singleTexture[T: ModelResourceLike](name: T, parent: String, textureKey: String, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, new ResourceLocation("minecraft", parent), textureKey, texture)

  def singleTexture[T: ModelResourceLike](name: T, parent: ResourceLocation, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, parent, "texture", texture)

  def singleTexture[T: ModelResourceLike](name: T, parent: ResourceLocation, textureKey: String, texture: ResourceLocation): LumoModelBuilder =
    withExistingParent(name, parent)
      .texture(textureKey, texture)

  def cubeAll[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, new ResourceLocation("minecraft", "block/cube_all"), "all", texture)

  def sideBottomTop[T: ModelResourceLike](name: T, parent: ResourceLocation, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    withExistingParent(name, parent)
      .texture("side", side)
      .texture("bottom", bottom)
      .texture("top", top)

  def cubeColumn[T: ModelResourceLike](name: T, side: ResourceLocation, ends: ResourceLocation): LumoModelBuilder =
    withExistingParent(name, new ResourceLocation("minecraft", "block/cube_column"))
      .texture("side", side)
      .texture("end", ends)

  def cubeColumnHorizontal[T: ModelResourceLike](name: T, side: ResourceLocation, ends: ResourceLocation): LumoModelBuilder =
    withExistingParent(name, new ResourceLocation("minecraft", "block/cube_column_horizontal"))
      .texture("side", side)
      .texture("end", ends)

  def stairs[T: ModelResourceLike](name: T, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    sideBottomTop(name, ResourceLocationExt.withDefaultNamespace("block/stairs"), side, bottom, top)

  def stairsOuter[T: ModelResourceLike](name: T, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    sideBottomTop(name, ResourceLocationExt.withDefaultNamespace("block/outer_stairs"), side, bottom, top)

  def stairsInner[T: ModelResourceLike](name: T, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    sideBottomTop(name, ResourceLocationExt.withDefaultNamespace("block/inner_stairs"), side, bottom, top)

  def slab[T: ModelResourceLike](name: T, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    sideBottomTop(name, ResourceLocationExt.withDefaultNamespace("block/slab"), side, bottom, top)

  def slabTop[T: ModelResourceLike](name: T, side: ResourceLocation, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    sideBottomTop(name, ResourceLocationExt.withDefaultNamespace("block/slab_top"), side, bottom, top)

  def button[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/button"), texture)

  def buttonPressed[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/button_pressed"), texture)

  def buttonInventory[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/button_inventory"), texture)

  def pressurePlate[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/pressure_plate_up"), texture)

  def pressurePlateDown[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/pressure_plate_down"), texture)

  def sign[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    getBuilder(name).texture("particle", texture)

  def fencePost[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/fence_post"), texture)

  def fenceSide[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/fence_side"), texture)

  def fenceInventory[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/fence_inventory"), texture)

  def fenceGate[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/template_fence_gate"), texture)

  def fenceGateOpen[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/template_fence_gate_open"), texture)

  def fenceGateWall[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/template_fence_gate_wall"), texture)

  def fenceGateWallOpen[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, ResourceLocationExt.withDefaultNamespace("block/template_fence_gate_wall_open"), texture)

  private def door[T: ModelResourceLike](name: T, model: String, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    withExistingParent(name, ResourceLocationExt.withDefaultNamespace("block/" + model))
      .texture("bottom", bottom)
      .texture("top", top)

  def doorBottomLeft[T: ModelResourceLike](name: T, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    door(name, "door_bottom_left", bottom, top)

  def doorBottomLeftOpen[T: ModelResourceLike](name: T, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    door(name, "door_bottom_left_open", bottom, top)

  def doorBottomRight[T: ModelResourceLike](name: T, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    door(name, "door_bottom_right", bottom, top)

  def doorBottomRightOpen[T: ModelResourceLike](name: T, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    door(name, "door_bottom_right_open", bottom, top)

  def doorTopLeft[T: ModelResourceLike](name: T, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    door(name, "door_top_left", bottom, top)

  def doorTopLeftOpen[T: ModelResourceLike](name: T, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    door(name, "door_top_left_open", bottom, top)

  def doorTopRight[T: ModelResourceLike](name: T, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    door(name, "door_top_right", bottom, top)

  def doorTopRightOpen[T: ModelResourceLike](name: T, bottom: ResourceLocation, top: ResourceLocation): LumoModelBuilder =
    door(name, "door_top_right_open", bottom, top)


  def trapdoorBottom[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, "block/template_trapdoor_bottom", texture)

  def trapdoorTop[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, "block/template_trapdoor_top", texture)

  def trapdoorOpen[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, "block/template_trapdoor_open", texture)

  def trapdoorOrientableBottom[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, "block/template_orientable_trapdoor_bottom", texture)

  def trapdoorOrientableTop[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, "block/template_orientable_trapdoor_top", texture)

  def trapdoorOrientableOpen[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, "block/template_orientable_trapdoor_open", texture)

  def cross[T: ModelResourceLike](name: T, texture: ResourceLocation): LumoModelBuilder =
    singleTexture(name, "block/cross", "cross", texture)


  def generateAll(cache: CachedOutput): CompletableFuture[?] =
    CompletableFuture.allOf(
      this.generatedModels.values.map { model =>
        val target = getPath(model)
        DataProvider.saveStable(cache, model.toJson, target)
      }.toSeq *
    )

  protected def getPath(model: LumoModelBuilder): Path =
    val loc = model.location
    this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(loc.getNamespace).resolve("models").resolve(loc.getPath + ".json")
