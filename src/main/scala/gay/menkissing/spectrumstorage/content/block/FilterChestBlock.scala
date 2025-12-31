package gay.menkissing.spectrumstorage.content.block

import de.dafuqs.spectrum.blocks.chests.SpectrumChestBlock
import gay.menkissing.spectrumstorage.content.block.entity.FilterChestBlockEntity
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.{Containers, InteractionHand, InteractionResult}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BarrelBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState}
import net.minecraft.world.phys.BlockHitResult

class FilterChestBlock(props: BlockBehaviour.Properties) extends BarrelBlock(props):
  locally:
    this.registerDefaultState:
      this.stateDefinition.any()
          .setValue(BottomlessStorageBlock.FACING, Direction.NORTH)
          .setValue(BottomlessStorageBlock.OPEN, false)

  override def newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity =
    FilterChestBlockEntity(blockPos, blockState)

  override def use(blockState: BlockState, level: Level, blockPos: BlockPos, player: Player, interactionHand: InteractionHand, blockHitResult: BlockHitResult): InteractionResult =
    if level.isClientSide then
      InteractionResult.SUCCESS
    else
      val blockEntity = level.getBlockEntity(blockPos)
      blockEntity match
        case be: FilterChestBlockEntity =>
          player.openMenu(be)
        case _ => ()
      InteractionResult.CONSUME

  override def onRemove(replaced: BlockState, level: Level, blockPos: BlockPos, newState: BlockState, movedByPiston: Boolean): Unit =
    if !replaced.is(newState.getBlock) then
      val be = level.getBlockEntity(blockPos)
      val update =
        be match
          case bse: FilterChestBlockEntity =>
            Containers.dropContents(level, blockPos, bse)
            true
          case _ => false
      super.onRemove(replaced, level, blockPos, newState, movedByPiston)
      if update then
        level.updateNeighbourForOutputSignal(blockPos, this)

  override def tick(blockState: BlockState, serverLevel: ServerLevel, blockPos: BlockPos, randomSource: RandomSource): Unit =
    val be = serverLevel.getBlockEntity(blockPos)
    be match
      case fcbe: FilterChestBlockEntity =>
        fcbe.recheckOpen()
      case _ => ()

  override def setPlacedBy(level: Level, blockPos: BlockPos, blockState: BlockState, livingEntity: LivingEntity, itemStack: ItemStack): Unit =
    if itemStack.hasCustomHoverName then
      level.getBlockEntity(blockPos) match
        case fcbe: FilterChestBlockEntity =>
          fcbe.setCustomName(itemStack.getHoverName)
        case _ => ()
    
