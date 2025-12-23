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

import com.google.gson.{JsonElement, JsonObject}
import net.minecraft.resources.ResourceLocation

import java.util.function.Supplier
import scala.collection.mutable

class LumoModelBuilder(val location: ResourceLocation) extends LumoModelGeneratedFile, Supplier[JsonElement]:
  protected var curParent: ResourceLocation = null

  protected val curTextures: mutable.Map[String, String] = mutable.HashMap()

  def parent(parent: ResourceLocation): this.type =
    this.curParent = parent
    this

  def texture(key: String, texture: String): this.type =
    this.curTextures(key) = texture
    this

  def texture(key: String, texture: ResourceLocation): this.type =
    this.texture(key, texture.toString)

  def toJson: JsonObject =
    val root = JsonObject()

    if this.curParent != null then
      root.addProperty("parent", this.curParent.toString)

    if this.curTextures.nonEmpty then
      val textures = JsonObject()
      this.curTextures.foreach { (k, v) =>
        textures.addProperty(k, v)
      }
      root.add("textures", textures)

    root

  override def get(): JsonElement = toJson



