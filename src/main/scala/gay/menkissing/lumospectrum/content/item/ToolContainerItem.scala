package gay.menkissing.lumospectrum.content.item

import gay.menkissing.lumospectrum.item.ItemBackedInventory
import gay.menkissing.lumospectrum.registries.{LumoScreens, LumoTags}
import gay.menkissing.lumospectrum.screen.ToolContainerMenu
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.{InteractionHand, InteractionResultHolder, SimpleContainer}
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.inventory.{AbstractContainerMenu, ClickAction, Slot}
import net.minecraft.world.item.{Item, ItemStack, ItemUtils}
import net.minecraft.world.level.Level

class ToolContainerItem(props: Item.Properties) extends Item(props):
  override def use(level: Level, player: Player, interactionHand: InteractionHand): InteractionResultHolder[ItemStack] =
    if !level.isClientSide then
      val stack = player.getItemInHand(interactionHand)
      val provider = new ExtendedScreenHandlerFactory {
        override def writeScreenOpeningData(player: ServerPlayer, buf: FriendlyByteBuf): Unit =
          buf.writeBoolean(interactionHand == InteractionHand.MAIN_HAND)

        override def getDisplayName: Component = stack.getHoverName

        override def createMenu(i: Int, inventory: Inventory, player: Player): AbstractContainerMenu =
          new ToolContainerMenu(i, inventory, stack)
      }
      player.openMenu(provider)
    InteractionResultHolder.sidedSuccess(player.getItemInHand(interactionHand), level.isClientSide)

  override def onDestroyed(itemEntity: ItemEntity): Unit =
    ItemUtils.onContainerDestroyed(itemEntity, ToolContainerItem.getRawInventory(itemEntity.getItem).items.stream())
    itemEntity.getItem.removeTagKey(ItemBackedInventory.ItemsTag)

  override def overrideOtherStackedOnMe(thisStack: ItemStack, thatStack: ItemStack, slot: Slot, clickAction: ClickAction, player: Player, slotAccess: SlotAccess): Boolean =
    if clickAction == ClickAction.SECONDARY && slot.allowModification(player) && !thatStack.isEmpty && thatStack.is(LumoTags.item.validToolTag) then
      val container = ToolContainerItem.getRawInventory(thisStack)
      if container.canAddItem(thatStack) then
        val res = container.addItem(thatStack)
        slotAccess.set(res)
        return true
    false

  override def overrideStackedOnOther(thisStack: ItemStack, slot: Slot, clickAction: ClickAction, player: Player): Boolean =
    if clickAction != ClickAction.SECONDARY then
      false
    else
      val container = ToolContainerItem.getRawInventory(thisStack)
      val thatStack = slot.getItem
      if !thatStack.isEmpty && thatStack.is(LumoTags.item.validToolTag) then
        if container.canAddItem(thatStack) then
          val res = container.addItem(thatStack)
          slot.set(res)
          true
        else
          false
      else
        false

object ToolContainerItem:
  def getRawInventory(stack: ItemStack): SimpleContainer =
    new ItemBackedInventory(stack, ToolContainerMenu.containerSize)
