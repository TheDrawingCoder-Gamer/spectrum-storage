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

package gay.menkissing.lumospectrum.util.resources

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block

trait ResourceLike[T]:
  def location(self: T): ResourceLocation

extension[T] (self: T)(using rl: ResourceLike[T])
  def location: ResourceLocation = rl.location(self)

given ResourceLike[ResourceLocation] = self => self

given ResourceLike[Item] = self => BuiltInRegistries.ITEM.getKey(self)

given ResourceLike[Block] = self => BuiltInRegistries.BLOCK.getKey(self)

// things that have models (items + blocks)
trait ModelResourceLike[T]:
  def modelLoc(self: T): ResourceLocation

extension[T] (self: T)(using rl: ModelResourceLike[T])
  def modelLoc: ResourceLocation = rl.modelLoc(self)

given ModelResourceLike[Item] = self =>
  val key = self.location
  new ResourceLocation(key.getNamespace, "item/" + key.getPath)

given ModelResourceLike[Block] = self =>
  val key = self.location
  new ResourceLocation(key.getNamespace, "block/" + key.getPath)

given ModelResourceLike[ResourceLocation] = self => self

extension (self: ResourceLocation)
  def extend(suffix: String): ResourceLocation =
    new ResourceLocation(self.getNamespace, self.getPath + suffix)
  
object ResourceLocationExt:
  def withDefaultNamespace(id: String): ResourceLocation =
    new ResourceLocation("minecraft", id)
  def fromNamespaceAndPath(namespace: String, path: String): ResourceLocation =
    new ResourceLocation(namespace, path)