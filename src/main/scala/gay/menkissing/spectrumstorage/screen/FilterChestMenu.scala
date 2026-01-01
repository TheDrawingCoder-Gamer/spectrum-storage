package gay.menkissing.spectrumstorage.screen

import de.dafuqs.spectrum.api.block.FilterConfigurable
import de.dafuqs.spectrum.inventories.slots.ShadowSlot
import gay.menkissing.spectrumstorage.content.block.entity.FilterChestBlockEntity
import gay.menkissing.spectrumstorage.registries.LumoScreens
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.{Container, SimpleContainer}
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.inventory.{AbstractContainerMenu, ClickAction, Slot}
import net.minecraft.world.item.ItemStack

class FilterChestMenu(windowId: Int, playerInv: Inventory, val container: Container, val blockEntity: FilterChestBlockEntity | Null, filterContainerFactory: AbstractContainerMenu => Container) extends AbstractContainerMenu(LumoScreens.filterChest, windowId):
  val level = playerInv.player.level()
  val filterInventory = filterContainerFactory(this)

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
        this.addSlot(new FilterChestFilterSlot(filterInventory, x + y * FilterChestMenu.filterColumns, 8 + x * 18, 17 + y * 18))



    for y <- 0 until ScreenCommon.playerRows do
      for x <- 0 until ScreenCommon.playerColumns do
        this.addSlot(new Slot(playerInv, x + y * ScreenCommon.playerColumns + 9, 8 + x * 18, 84 + y * 18))

    for x <- 0 until ScreenCommon.playerColumns do
      this.addSlot(new Slot(playerInv, x, 8 + x * 18, 142))

  override def quickMoveStack(player: Player, i: Int): ItemStack =
    var stack = ItemStack.EMPTY
    val slot = this.slots.get(i)
    if slot.hasItem then
      val stack2 = slot.getItem
      stack = stack2.copy()
      if i < FilterChestBlockEntity.inventorySize then
        if !this.moveItemStackTo(stack2, 9, 45, true) then
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

  protected class FilterChestFilterSlot(container: Container, index: Int, x: Int, y: Int) extends ShadowSlot(container, index, x, y):

    override def onClicked(heldStack: ItemStack, kind: ClickAction, player: Player): Boolean =
      if blockEntity != null then
        blockEntity.setFilterItem(getContainerSlot, ItemVariant.of(heldStack))
      super.onClicked(heldStack, kind, player)

object FilterChestMenu:
  val rows = 3
  val filterColumns = 6
  val containerColumns = 3


  def apply(windowId: Int, playerInv: Inventory, blockEntity: FilterChestBlockEntity): FilterChestMenu =
    new FilterChestMenu(windowId, playerInv, blockEntity, blockEntity, (menu: AbstractContainerMenu) => FilterConfigurable.getFilterInventoryFromItemsHandler(windowId, playerInv, blockEntity.getItemFilters, menu))


  def fromNetwork(windowId: Int, playerInv: Inventory, buf: FriendlyByteBuf): FilterChestMenu =
    val pos = buf.readBlockPos()
    val container = new SimpleContainer(FilterChestBlockEntity.inventorySize)
    val blockEntity =
      playerInv.player.level().getBlockEntity(pos) match
        case filterChestBlockEntity: FilterChestBlockEntity => filterChestBlockEntity
        case _ => null
    new FilterChestMenu(windowId, playerInv, container, blockEntity, (menu: AbstractContainerMenu) => FilterConfigurable.getFilterInventoryFromPacketHandler(windowId, playerInv, buf, menu))