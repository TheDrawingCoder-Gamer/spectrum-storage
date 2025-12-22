package gay.menkissing.spectrumstorage.screen

import de.dafuqs.spectrum.registries.SpectrumItems
import gay.menkissing.spectrumstorage.content.SpectrumStorageItems
import gay.menkissing.spectrumstorage.content.block.entity.BottomlessStorageBlockEntity
import gay.menkissing.spectrumstorage.item.ItemBackedInventory
import gay.menkissing.spectrumstorage.registries.{LumoScreens, LumoTags}
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.inventory.{AbstractContainerMenu, MenuType, Slot}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.{Container, InteractionHand, SimpleContainer}
import org.jetbrains.annotations.Nullable

class BottomlessStorageMenu(menuType: MenuType[BottomlessStorageMenu], val rows: Int, windowId: Int, playerInv: Inventory, val container: Container) extends AbstractContainerMenu(menuType, windowId):
  locally:
    AbstractContainerMenu.checkContainerSize(container, rows * BottomlessStorageMenu.columns)
    container.startOpen(playerInv.player)

    val k = (rows - 4) * 18
    for
      i <- 0 until rows
      j <- 0 until BottomlessStorageMenu.columns
    do
      addSlot(new Slot(container, j + i * BottomlessStorageMenu.columns, 8 + j * 18, 18 + i * 18) {
        override def mayPlace(itemStack: ItemStack): Boolean =
          isValidItem(itemStack)
      })

    for
      i <- 0 until ScreenCommon.playerRows
      j <- 0 until ScreenCommon.playerColumns
    do
      addSlot(new Slot(playerInv, j + i * ScreenCommon.playerColumns + 9, 8 + j * 18, 103 + i * 18 + k))

    for
      i <- 0 until ScreenCommon.playerColumns
    do
      addSlot(new Slot(playerInv, i, 8 + i * 18, 161 + k))
  
  override def quickMoveStack(player: Player, i: Int): ItemStack =
    // ported from scala 1.21 code that was
    // ported from java code that was ported from scala code that was ported from java code
    var transferredItemStack = ItemStack.EMPTY
    val slot = this.slots.get(i)
    if slot.hasItem then
      val slotStack = slot.getItem
      transferredItemStack = slotStack.copy()
      val boxStart = 0
      val boxEnd = boxStart + BottomlessStorageMenu.columns * rows
      val invEnd = boxEnd + 36
      if i < boxEnd then
        if !moveItemStackTo(slotStack, boxEnd, invEnd, true) then
          return ItemStack.EMPTY
        else
          if !slotStack.isEmpty && isValidItem(slotStack) && !moveItemStackTo(slotStack, boxStart, boxEnd, false) then
            return ItemStack.EMPTY

      if slotStack.isEmpty then
        slot.setByPlayer(ItemStack.EMPTY)
      else
        slot.setChanged()

      if slotStack.getCount == transferredItemStack.getCount then
        return ItemStack.EMPTY

      slot.onTake(player, slotStack)
    transferredItemStack

  def isValidItem(item: ItemStack): Boolean =
    item.is(SpectrumItems.BOTTOMLESS_BUNDLE) || item.is(SpectrumStorageItems.bottomlessBottle)
  override def stillValid(player: Player): Boolean =
    container.stillValid(player)

object BottomlessStorageMenu:
  val amphoraRows = 6
  val barrelRows = 3
  val columns = 9
  val amphoraContainerSize = amphoraRows * columns
  val barrelContainerSize = barrelRows * columns
  
  private def bindConstructor(screen: MenuType[BottomlessStorageMenu], rows: Int)(windowId: Int, playerInv: Inventory): BottomlessStorageMenu =
    new BottomlessStorageMenu(screen, rows, windowId, playerInv, new SimpleContainer(rows * 9))
  
  def barrel(windowId: Int, playerInv: Inventory): BottomlessStorageMenu =
    bindConstructor(LumoScreens.bottomlessBarrel, barrelRows)(windowId, playerInv)
    
  def barrelServer(windowId: Int, playerInv: Inventory, container: Container): BottomlessStorageMenu =
    new BottomlessStorageMenu(LumoScreens.bottomlessBarrel, barrelRows, windowId, playerInv, container)
    
  def amphora(windowId: Int, playerInv: Inventory): BottomlessStorageMenu =
    bindConstructor(LumoScreens.bottomlessAmphora, amphoraRows)(windowId, playerInv)
  
  def amphoraServer(windowId: Int, playerInv: Inventory, container: Container): BottomlessStorageMenu =
    new BottomlessStorageMenu(LumoScreens.bottomlessAmphora, amphoraRows, windowId, playerInv, container)
