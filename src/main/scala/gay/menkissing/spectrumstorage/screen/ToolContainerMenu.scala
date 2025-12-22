package gay.menkissing.spectrumstorage.screen

import gay.menkissing.spectrumstorage.item.ItemBackedInventory
import gay.menkissing.spectrumstorage.registries.{LumoScreens, LumoTags}
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.{InteractionHand, SimpleContainer}
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.inventory.{AbstractContainerMenu, MenuType, Slot}
import net.minecraft.world.item.ItemStack
import org.jetbrains.annotations.Nullable

class ToolContainerMenu(windowId: Int, playerInv: Inventory, val box: ItemStack) extends AbstractContainerMenu(LumoScreens.toolContainer, windowId):
  locally:
    val boxInv =
      if !playerInv.player.level().isClientSide then
        new ItemBackedInventory(box, ToolContainerMenu.containerSize)
      else
        new SimpleContainer(ToolContainerMenu.containerSize)

    boxInv.startOpen(playerInv.player)


    val k = (ToolContainerMenu.rows - 4) * 18
    for
      i <- 0 until ToolContainerMenu.rows
      j <- 0 until ToolContainerMenu.columns
    do
      addSlot(new Slot(boxInv, j + i * ToolContainerMenu.columns, 8 + j * 18, 18 + i * 18) {
        override def mayPlace(itemStack: ItemStack): Boolean =
          isValidItem(itemStack)
      })

    for
      i <- 0 until ScreenCommon.playerRows
      j <- 0 until ScreenCommon.playerColumns
    do
      addSlot(new Slot(playerInv, j + i * ScreenCommon.playerColumns + 9, 8 + j * 18, 103 + i * 18 + k))

    for
      i <- 0 until 9
    do
      if playerInv.getItem(i) == box then
        addSlot(new Slot(playerInv, i, 8 + i * 18, 161 + k) {
          override def mayPickup(player: Player): Boolean = false

          override def mayPlace(itemStack: ItemStack): Boolean = false
        })
      else
        addSlot(new Slot(playerInv, i, 8 + i * 18, 161 + k))

  override def quickMoveStack(player: Player, i: Int): ItemStack =
    // ported from java code that was ported from scala code that was ported from java code
    var transferredItemStack = ItemStack.EMPTY
    val slot = this.slots.get(i)
    if slot.hasItem then
      val slotStack = slot.getItem
      transferredItemStack = slotStack.copy()
      val boxStart = 0
      val boxEnd = boxStart + ToolContainerMenu.containerSize
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

  def isValidItem(item: ItemStack): Boolean = item.is(LumoTags.item.validToolTag)
  override def stillValid(player: Player): Boolean =
    player.getItemInHand(InteractionHand.MAIN_HAND) == box || player.getItemInHand(InteractionHand.OFF_HAND) == box

object ToolContainerMenu:
  val rows = 5
  val columns = 9
  val containerSize = rows * columns

  def fromNetwork(windowId: Int, inv: Inventory, buf: FriendlyByteBuf): ToolContainerMenu =
    val mainHand = buf.readBoolean()
    val hand = if mainHand then InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND
    new ToolContainerMenu(windowId, inv, inv.player.getItemInHand(hand))
