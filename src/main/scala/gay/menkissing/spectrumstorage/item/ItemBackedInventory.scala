package gay.menkissing.spectrumstorage.item

import net.minecraft.nbt.{CompoundTag, ListTag, Tag}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.{Containers, SimpleContainer}
import net.minecraft.world.item.ItemStack

class ItemBackedInventory(val stack: ItemStack, expectedSize: Int) extends SimpleContainer(expectedSize):
  locally:
    val tag = stack.getOrCreateTag()
  
    val lst = tag.getList(ItemBackedInventory.ItemsTag, Tag.TAG_COMPOUND)
    (0 until math.min(lst.size(), expectedSize)).foreach: i =>
      setItem(i, ItemStack.of(lst.getCompound(i)))

  override def stillValid(player: Player): Boolean =
    !stack.isEmpty

  override def setChanged(): Unit =
    super.setChanged()
    val lst = new ListTag()
    0.until(getContainerSize).foreach: i =>
      lst.add(getItem(i).save(new CompoundTag()))
    val tag = stack.getOrCreateTag()
    tag.put(ItemBackedInventory.ItemsTag, lst)
    
object ItemBackedInventory:
  val ItemsTag = "Items"
  
