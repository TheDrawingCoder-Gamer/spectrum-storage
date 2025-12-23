package gay.menkissing.spectrumstorage.util.registry.provider.generators

import com.google.gson.{JsonElement, JsonObject}
import net.minecraft.resources.ResourceLocation

import java.util.function.Supplier

class WrappedLumoModel(val location: ResourceLocation, val data: Supplier[JsonElement]) extends LumoModelGeneratedFile:
  override def toJson: JsonObject =
    val r = data.get()
    r.getAsJsonObject
  
