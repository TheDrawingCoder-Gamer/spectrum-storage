package gay.menkissing.spectrumstorage.screen

import de.dafuqs.spectrum.api.block.FilterConfigurable
import de.dafuqs.spectrum.inventories.slots.{FilterSlot, ShadowSlot}
import gay.menkissing.spectrumstorage.content.block.entity.FilterChestBlockEntity
import gay.menkissing.spectrumstorage.registries.SpectrumStorageScreens
import io.netty.buffer.ByteBuf
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.{ByteBufCodecs, StreamCodec}
import net.minecraft.world.{Container, SimpleContainer}
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.inventory.{AbstractContainerMenu, ClickAction, Slot}
import net.minecraft.world.item.ItemStack

class FilterChestMenu(windowId: Int, playerInv: Inventory, val container: Container, val blockEntity: FilterChestBlockEntity | Null, val data: FilterConfigurable.ExtendedDataWithPos) extends AbstractContainerMenu(SpectrumStorageScreens.filterChest.get(), windowId):
  val level = playerInv.player.level()
  val filterInventory = FilterConfigurable.getFilterInventoryFromExtendedData(windowId, playerInv, data.data(), this)

  locally:
    AbstractContainerMenu.checkContainerSize(container, FilterChestBlockEntity.inventorySize)
    container.startOpen(playerInv.player)

    for y <- 0 until FilterChestMenu.rows do
      for x <- 0 until FilterChestMenu.containerColumns do
        this.addSlot(new Slot(container, x + y * FilterChestMenu.containerColumns, 8 + (18 * FilterChestMenu
          .filterColumns) + x * 18, 17 + y * 18))

    // Y starts at  17, size is 18
    for y <- 0 until FilterChestMenu.rows do
      for x <- 0 until FilterChestMenu.filterColumns do
        this.addSlot(new FilterSlot(blockEntity, filterInventory, x + y * FilterChestMenu.filterColumns, 8 + x * 18, 17 + y * 18))


    for y <- 0 until ScreenCommon.playerRows do
      for x <- 0 until ScreenCommon.playerColumns do
        this.addSlot(new Slot(playerInv, x + y * ScreenCommon.playerColumns + 9, 8 + x * 18, 84 + y * 18))

    for x <- 0 until ScreenCommon.playerColumns do
      this.addSlot(new Slot(playerInv, x, 8 + x * 18, 142))

  override def quickMoveStack(player: Player, i: Int): ItemStack =
    var stack = ItemStack.EMPTY
    val slot = this.slots.get(i)
    if !slot.isInstanceOf[FilterSlot] && slot.hasItem then
      val stack2 = slot.getItem
      stack = stack2.copy()
      if i < FilterChestBlockEntity.inventorySize then
        if !this.moveItemStackTo(stack2, FilterChestMenu.playerSlotStart, FilterChestMenu.playerSlotEnd, true) then
          return ItemStack.EMPTY
      else if !this.moveItemStackTo(stack2, 0, 9, false) then
        return ItemStack.EMPTY

      if stack2.isEmpty then
        slot.set(ItemStack.EMPTY)
      else
        slot.setChanged()

      if stack2.getCount == stack.getCount then
        return ItemStack.EMPTY

      slot.onTake(player, stack2)

    stack


  override def stillValid(player: Player): Boolean =
    this.playerInv.stillValid(player)


object FilterChestMenu:
  val rows = 3
  val filterColumns = 6
  val containerColumns = 3
  val playerSlotStart = FilterChestBlockEntity.inventorySize + FilterChestBlockEntity.filterCount
  val playerSlotEnd = playerSlotStart + 9 * 4

  def isShadowSlot(slot: Int): Boolean =
    slot >= FilterChestBlockEntity.inventorySize && slot < FilterChestBlockEntity.inventorySize + FilterChestBlockEntity.filterCount

  
  

  def fromNetwork(windowId: Int, playerInv: Inventory, data: FilterConfigurable.ExtendedDataWithPos): FilterChestMenu =
    val pos = data.pos()
    val container = new SimpleContainer(FilterChestBlockEntity.inventorySize)
    val blockEntity =
      playerInv.player.level().getBlockEntity(pos) match
        case filterChestBlockEntity: FilterChestBlockEntity => filterChestBlockEntity
        case _ => null
    new FilterChestMenu(windowId, playerInv, container, blockEntity, data)