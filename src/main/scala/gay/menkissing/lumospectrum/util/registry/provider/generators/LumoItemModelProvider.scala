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

import gay.menkissing.lumospectrum.util.resources.{*, given}
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.{CachedOutput, DataProvider}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item

import java.util.concurrent.CompletableFuture

class LumoItemModelProvider(output: FabricDataOutput) extends LumoModelProvider(output):

  def generated(item: ResourceLocation): LumoModelBuilder =
    flatItem(item, item)

  def generated(item: Item): LumoModelBuilder =
    generated(item.modelLoc)

  def blockItem(block: ResourceLocation): LumoModelBuilder =
    val newLoc = ResourceLocationExt
      .fromNamespaceAndPath(block.getNamespace, "item/" + block.getPath.stripPrefix("block/"))
    getBuilder(newLoc)
      .parent(block)
    


object LumoItemModelProvider:
  def provider(output: FabricDataOutput): LumoItemModelProvider & DataProvider =
    new LumoItemModelProvider(output) with DataProvider:
      override def run(output: CachedOutput): CompletableFuture[?] =
        this.generateAll(output)

      override def getName: String =
        "Lumo Item Models"