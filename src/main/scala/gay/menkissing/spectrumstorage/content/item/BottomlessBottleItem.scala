package gay.menkissing.spectrumstorage.content.item

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.item.{Item, ItemDisplayContext, ItemStack, TooltipFlag}
import de.dafuqs.spectrum.api.item.ExtendedEnchantable
import de.dafuqs.spectrum.api.render.DynamicItemRenderer
import gay.menkissing.spectrumstorage.SpectrumStorage
import gay.menkissing.spectrumstorage.content.SpectrumStorageItems
import gay.menkissing.spectrumstorage.content.item.BottomlessBottleItem.BottomlessBottleContents
import gay.menkissing.spectrumstorage.registries.LumoTranslationKeys
import gay.menkissing.spectrumstorage.util.resources.ResourceLocationExt
import net.fabricmc.api.{EnvType, Environment}
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.{FluidConstants, FluidVariant, FluidVariantAttributes}
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.impl.client.indigo.renderer.helper.GeometryHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.{BakedQuad, ItemOverrides, ItemTransforms}
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.{BakedModel, Material, ModelBaker, ModelState, UnbakedModel}
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.core.cauldron.CauldronInteraction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.{SoundEvents, SoundSource}
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.{InteractionHand, InteractionResult, InteractionResultHolder}
import net.minecraft.world.item.enchantment.{Enchantment, EnchantmentHelper, Enchantments}
import net.minecraft.world.level.{BlockAndTintGetter, ClipContext, Level}
import net.minecraft.world.level.block.{Blocks, BucketPickup, LayeredCauldronBlock, LiquidBlockContainer}
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.{Fluid, Fluids}
import net.minecraft.world.phys.{BlockHitResult, HitResult}

import java.util
import java.util.function
import java.util.function.Supplier
import scala.annotation.nowarn

class BottomlessBottleItem(props: Item.Properties) extends Item(props), ExtendedEnchantable:
  override def isEnchantable(stack: ItemStack): Boolean = true

  override def getEnchantmentValue: Int = 5

  override def acceptsEnchantment(enchantment: Enchantment): Boolean =
    enchantment == Enchantments.POWER_ARROWS


  override def appendHoverText(stack: ItemStack, level: Level, tooltipComponents: util.List[Component], tooltipFlag: TooltipFlag): Unit =
    val contents = BottomlessBottleItem.BottomlessBottleContents.getFromStack(stack)
    if contents.isEmpty then
      tooltipComponents.add(LumoTranslationKeys.bottomlessBottle.tooltip.empty)
      tooltipComponents.add(LumoTranslationKeys.bottomlessBottle.tooltip.usagePickup)
    else
      val containedFluid = contents.variant
      val max = BottomlessBottleItem.getMaxStackExpensive(stack)
      tooltipComponents.add(LumoTranslationKeys.bottomlessBottle.tooltip.countMB(contents.amount, max))
      tooltipComponents.add(FluidVariantAttributes.getName(containedFluid))
      tooltipComponents.add(LumoTranslationKeys.bottomlessBottle.tooltip.usagePickup)
      tooltipComponents.add(LumoTranslationKeys.bottomlessBottle.tooltip.usagePlace)

  def playEmptyingSound(player: Player, level: Level, pos: BlockPos, variant: FluidVariant): Unit =
    val event = FluidVariantAttributes.getEmptySound(variant)
    level.playSound(player, pos, event, SoundSource.BLOCKS, 1f, 1f)

  def placeFluid(player: Player | Null, level: Level, pos: BlockPos, hitResult: BlockHitResult, thisStack: ItemStack): Boolean =
    val contents = BottomlessBottleItem.BottomlessBottleContents.getFromStack(thisStack)
    if contents.isEmpty || contents.amount < FluidConstants.BUCKET then
      return false

    val blockState = level.getBlockState(pos)
    val canPlace = blockState.canBeReplaced(contents.variant.getFluid)

    if
      !blockState.isAir && !canPlace && (
        blockState.getBlock match
          case liquidBlock: LiquidBlockContainer => !liquidBlock.canPlaceLiquid(level, pos, blockState, contents.variant.getFluid)
          case _ => true
      )
    then
      hitResult != null && this.placeFluid(player, level, hitResult.getBlockPos.relative(hitResult.getDirection), null, thisStack)
    else
      if level.dimensionType().ultraWarm() && contents.variant.getFluid.is(FluidTags.WATER) then
        val i = pos.getX
        val j = pos.getY
        val k = pos.getZ
        level.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f + (level.random.nextFloat() - level.random.nextFloat()) * 0.8f)
        (0 until 8).foreach: _ =>
          level.addParticle(ParticleTypes.LARGE_SMOKE, i.toDouble + math.random(), j.toDouble + math.random(), k.toDouble + math.random(), 0d, 0d, 0d)
      else if blockState.getBlock.isInstanceOf[LiquidBlockContainer] && contents.variant.getFluid == Fluids.WATER then
        if blockState.getBlock.asInstanceOf[LiquidBlockContainer].placeLiquid(level, pos, blockState, Fluids.WATER.getSource(false)) then
          this.playEmptyingSound(player, level, pos, contents.variant)
      else
        if !level.isClientSide && canPlace && !blockState.liquid() then
          level.removeBlock(pos, true)

        this.playEmptyingSound(player, level, pos, contents.variant)
        level.setBlock(pos, contents.variant.getFluid.defaultFluidState().createLegacyBlock(), 11)
      true

  override def use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder[ItemStack] =
    val stack = player.getItemInHand(usedHand)
    val contents = BottomlessBottleItem.BottomlessBottleContents.getFromStack(stack)
    val blockHitResult = Item.getPlayerPOVHitResult(level, player, if player.isShiftKeyDown then ClipContext.Fluid.NONE else ClipContext.Fluid.SOURCE_ONLY)
    blockHitResult.getType match
      case HitResult.Type.MISS | HitResult.Type.ENTITY =>
        InteractionResultHolder.pass(stack)
      case HitResult.Type.BLOCK =>
        val hitPos = blockHitResult.getBlockPos
        val direction = blockHitResult.getDirection
        val placePos = hitPos.relative(direction)
        if !level.mayInteract(player, hitPos) || !player.mayUseItemAt(placePos, direction, stack) then
          InteractionResultHolder.fail(stack)
        else
          val hitState = level.getBlockState(hitPos)
          val builder = BottomlessBottleItem.BottomlessBottleContents.Builder.fromStack(stack)
          if player.isShiftKeyDown then
            // placing
            if builder.extract(builder.template, FluidConstants.BUCKET) != FluidConstants.BUCKET then
              return InteractionResultHolder.fail(stack)

            val targetPos = if hitState.getBlock.isInstanceOf[LiquidBlockContainer] then hitPos else placePos
            if this.placeFluid(player, level, targetPos, blockHitResult, stack) then
              if player.getAbilities.instabuild then
                return InteractionResultHolder.success(stack)

              val newStack = stack.copy()
              BottomlessBottleItem.BottomlessBottleContents.replaceInStack(newStack, builder.build)
              return InteractionResultHolder.success(newStack)
          else
            // pickup
            if builder.max - builder.amount >= FluidConstants.BUCKET then
              val fluid = level.getFluidState(hitPos)

              if fluid != null && (builder.isEmpty || builder.template.getFluid == fluid.getType) then
                if builder.insert(FluidVariant.of(fluid.getType), FluidConstants.BUCKET) == FluidConstants.BUCKET then
                  hitState.getBlock match
                    case bucketPickup: BucketPickup =>
                      if !bucketPickup.pickupBlock(level, hitPos, hitState).isEmpty then
                        val sound = FluidVariantAttributes.getFillSound(FluidVariant.of(fluid.getType))
                        level.playSound(player, hitPos, sound, SoundSource.BLOCKS, 1f, 1f)

                        val newStack = stack.copy()
                        BottomlessBottleItem.BottomlessBottleContents.replaceInStack(newStack, builder.build)
                        return InteractionResultHolder.success(newStack)
                    case _ => ()
        InteractionResultHolder.fail(stack)

object BottomlessBottleItem:
  val baseMax: Long = FluidConstants.BUCKET * 256

  @nowarn("msg=eta")
  def registerCauldronInteractions(): Unit =
    CauldronInteraction.EMPTY.put(SpectrumStorageItems.bottomlessBottle, emptyBottleInteraction)
    CauldronInteraction.LAVA.put(SpectrumStorageItems.bottomlessBottle, fillBottleInteraction(Fluids.LAVA))
    CauldronInteraction.WATER.put(SpectrumStorageItems.bottomlessBottle, fillBottleInteraction(Fluids.WATER))

  def maxAllowed(level: Int): Long =
    baseMax * math.pow(8, math.min(level, 5)).toLong
  def getMaxStackExpensive(stack: ItemStack): Long =
    maxAllowed(EnchantmentHelper.getEnchantments(stack).getOrDefault(Enchantments.POWER_ARROWS, 0))

  def emptyBottleInteraction(blockState: BlockState, level: Level, blockPos: BlockPos, player: Player, usedHand: InteractionHand, stack: ItemStack): InteractionResult =
    val contents = BottomlessBottleContents.getFromStack(stack)

    if contents.variant.getFluid == Fluids.WATER then
      val builder = BottomlessBottleContents.Builder.fromStack(stack)
      if builder.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET) == FluidConstants.BUCKET then
        level.setBlockAndUpdate(blockPos, Blocks.WATER_CAULDRON.defaultBlockState()
          .setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL))
        level.playSound(player, blockPos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1f, 1f)
        BottomlessBottleContents.replaceInStack(stack, builder.build)
        return InteractionResult.SUCCESS
    else if contents.variant.getFluid == Fluids.LAVA then
      val builder = BottomlessBottleContents.Builder.fromStack(stack)
      if builder.extract(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET) == FluidConstants.BUCKET then
        level.setBlockAndUpdate(blockPos, Blocks.LAVA_CAULDRON.defaultBlockState())
        level.playSound(player, blockPos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1f, 1f)
        BottomlessBottleContents.replaceInStack(stack, builder.build)
        return InteractionResult.SUCCESS
    InteractionResult.PASS

  def fillBottleInteraction(fluid: Fluid)(blockState: BlockState, level: Level, blockPos: BlockPos, player: Player, usedHand: InteractionHand, stack: ItemStack): InteractionResult =
    val contents = BottomlessBottleContents.getFromStack(stack)
    if contents.isEmpty || contents.variant.getFluid == fluid then
      val amount = blockState.getOptionalValue(LayeredCauldronBlock.LEVEL).orElse(3) * FluidConstants.BOTTLE
      val builder = BottomlessBottleContents.Builder.fromStack(stack)
      if builder.insert(FluidVariant.of(fluid), amount) == amount then
        BottomlessBottleContents.replaceInStack(stack, builder.build)
        level.setBlockAndUpdate(blockPos, Blocks.CAULDRON.defaultBlockState())
        level.playSound(player, blockPos, FluidVariantAttributes.getFillSound(builder.template), SoundSource
          .BLOCKS, 1f, 1f)
        return InteractionResult.SUCCESS

    InteractionResult.PASS

  case class BottomlessBottleContents(variant: FluidVariant, amount: Long):
    def isEmpty: Boolean = variant.isBlank || amount == 0

    def toNbt: CompoundTag =
      val tag = CompoundTag()
      tag.put("Fluid", variant.toNbt)
      tag.putLong("Amount", amount)
      tag
  object BottomlessBottleContents:
    val EMPTY: BottomlessBottleContents =
      BottomlessBottleContents(FluidVariant.blank(), 0)

    def fromNbt(nbt: CompoundTag): BottomlessBottleContents =
      val variant = FluidVariant.fromNbt(nbt.getCompound("Fluid"))
      val amount = nbt.getLong("Amount")
      BottomlessBottleContents(variant, amount)


    def getFromStack(stack: ItemStack): BottomlessBottleContents =
      val compound = stack.getOrCreateTag()
      if compound.contains("StoredFluid") then
        val storedCompound = compound.getCompound("StoredFluid")
        fromNbt(storedCompound)
      else
        BottomlessBottleContents.EMPTY

    def replaceInStack(stack: ItemStack, contents: BottomlessBottleContents): Unit =
      val compound = stack.getOrCreateTag()
      compound.put("StoredFluid", contents.toNbt)

    class Builder(var template: FluidVariant, var amount: Long, val max: Long):
      def isEmpty: Boolean =
        template.isBlank || amount == 0

      // TODO: storing

      def copied: Builder =
        Builder(template, amount, max)

      def build: BottomlessBottleContents =
        BottomlessBottleContents(template, amount)

      def getMaxAllowed(variant: FluidVariant, amount: Long): Long =
        if variant.isBlank || amount <= 0 || (!this.isEmpty && template != variant) then
          0
        else
          this.max - this.amount

      def insert(variant: FluidVariant, amount: Long): Long =
        val added = math.min(amount, getMaxAllowed(variant, amount))
        if added == 0 then
          return 0
        if this.isEmpty then
          this.template = variant

        this.amount += math.min(this.max - this.amount, added)
        added

      def extract(variant: FluidVariant, amount: Long): Long =
        if variant != template then
          0
        else
          val toRemove = math.min(this.amount, amount)
          this.amount -= toRemove
          if this.amount == 0 then
            this.template = FluidVariant.blank()

          toRemove
    object Builder:
      def fromStack(stack: ItemStack): BottomlessBottleContents.Builder =
        val prev = getFromStack(stack)
        val max = getMaxStackExpensive(stack)
        BottomlessBottleContents.Builder(prev.variant, prev.amount, max)
    class BottomlessBottleStorage(val context: ContainerItemContext) extends SingleSlotStorage[FluidVariant]:
      override def getCapacity: Long =
        if !context.getItemVariant.isOf(SpectrumStorageItems.bottomlessBottle) then
          0
        else
          getMaxStackExpensive(context.getItemVariant.toStack)

      override def extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)

        if !context.getItemVariant.isOf(SpectrumStorageItems.bottomlessBottle) then
          return 0

        val builder = Builder.fromStack(context.getItemVariant.toStack)

        if !builder.isEmpty && resource == builder.template then
          val extracted = builder.extract(resource, maxAmount)
          val newStack = context.getItemVariant.toStack
          replaceInStack(newStack, builder.build)
          val newVariant = ItemVariant.of(newStack)

          if context.exchange(newVariant, 1, transaction) == 1 then
            return extracted

        0

      override def insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long =
        StoragePreconditions.notBlankNotNegative(resource, maxAmount)

        if !context.getItemVariant.isOf(SpectrumStorageItems.bottomlessBottle) then
          return 0

        val builder = Builder.fromStack(context.getItemVariant.toStack)

        if builder.isEmpty || resource == builder.template then
          val inserted = builder.insert(resource, maxAmount)
          val newStack = context.getItemVariant.toStack
          replaceInStack(newStack, builder.build)
          val newVariant = ItemVariant.of(newStack)

          if context.exchange(newVariant, 1, transaction) == 1 then
            return inserted

        0

      override def isResourceBlank: Boolean =
        !context.getItemVariant.isOf(SpectrumStorageItems.bottomlessBottle)
        || getFromStack(context.getItemVariant.toStack).variant.isBlank

      override def getResource: FluidVariant =
        if !context.getItemVariant.isOf(SpectrumStorageItems.bottomlessBottle) then
          FluidVariant.blank()
        else
          getFromStack(context.getItemVariant.toStack).variant

      override def getAmount: Long =
        if !context.getItemVariant.isOf(SpectrumStorageItems.bottomlessBottle) then
          0
        else
          getFromStack(context.getItemVariant.toStack).amount


// Top level renderer, because fabric datagen doesn't
// respect nested environment annnotations
object BottomlessBottleRenderer:
  val bottomlessBottleID: ResourceLocation = SpectrumStorage.locate("item/bottomless_bottle_base")
  val fluidModelID: ResourceLocation = SpectrumStorage.locate("item/bottomless_bottle_fluid")

@Environment(EnvType.CLIENT)
class BottomlessBottleItemModel extends UnbakedModel, BakedModel, FabricBakedModel:
  private var baseModel: BakedModel = null
  private var fluidModel: BakedModel = null
  private var sprite: TextureAtlasSprite = null

  override def getDependencies: util.Collection[ResourceLocation] =
    util.List.of(BottomlessBottleRenderer.bottomlessBottleID, BottomlessBottleRenderer.fluidModelID)

  override def resolveParents(resolver: function.Function[ResourceLocation, UnbakedModel]): Unit = ()

  override def getQuads(blockState: BlockState, direction: Direction, randomSource: RandomSource): util.List[BakedQuad] =
    util.List.of()

  override def emitBlockQuads(blockView: BlockAndTintGetter, state: BlockState, pos: BlockPos, randomSupplier: Supplier[RandomSource], context: RenderContext): Unit =
    ()

  override def emitItemQuads(stack: ItemStack, randomSupplier: Supplier[RandomSource], context: RenderContext): Unit =
    baseModel.emitItemQuads(stack, randomSupplier, context)

    val contents = BottomlessBottleItem.BottomlessBottleContents.getFromStack(stack)

    if contents.isEmpty || context.itemTransformationMode() != ItemDisplayContext.GUI then
      return

    val variant = contents.variant

    if variant.isBlank then
      return

    val variantRenderHandler = FluidVariantRendering.getHandlerOrDefault(variant.getFluid)

    if variantRenderHandler == null then
      return

    val fluidSprite = variantRenderHandler.getSprites(variant)(0)
    // force full alpha
    val fluidColor = variantRenderHandler.getColor(variant, null, null) | 0xff000000

    context.pushTransform: quad =>
      quad.nominalFace(GeometryHelper.lightFace(quad))
      quad.color(fluidColor, fluidColor, fluidColor, fluidColor)
      (0 until 4).foreach: i =>
        val pos = quad.copyPos(i, null)
        pos.add(0.5f, 0.5f, 1f)
        pos.mul(0.5f)
        pos.add(0.25f, 0.25f, 0.25f)
        quad.pos(i, pos)

      if fluidSprite == null then
        quad.spriteBake(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocationExt.withDefaultNamespace("missingno")), MutableQuadView.BAKE_LOCK_UV)
      else
        quad.spriteBake(fluidSprite, MutableQuadView.BAKE_LOCK_UV)

      true

    val emitter = context.getEmitter

    fluidModel.getQuads(null, null, randomSupplier.get()).forEach: q =>
      emitter.fromVanilla(q.getVertices, 0)
      emitter.emit()

    context.popTransform()

  override def isCustomRenderer: Boolean = false

  override def isVanillaAdapter: Boolean = false

  override def bake(baker: ModelBaker, spriteGetter: function.Function[Material, TextureAtlasSprite], state: ModelState, resourceLocation: ResourceLocation): BakedModel =
    baseModel = baker.bake(BottomlessBottleRenderer.bottomlessBottleID, state)
    fluidModel = baker.bake(BottomlessBottleRenderer.fluidModelID, state)
    sprite = spriteGetter.apply(Material(InventoryMenu.BLOCK_ATLAS, SpectrumStorage.locate("item/bottomless_bottle")))
    this

  override def useAmbientOcclusion(): Boolean = false

  override def isGui3d: Boolean = false

  override def usesBlockLight(): Boolean = false

  override def getParticleIcon: TextureAtlasSprite = sprite

  override def getTransforms: ItemTransforms = baseModel.getTransforms

  override def getOverrides: ItemOverrides = ItemOverrides.EMPTY

@Environment(EnvType.CLIENT)
class BottomlessBottleModelLoader extends ModelLoadingPlugin:
  override def onInitializeModelLoader(context: ModelLoadingPlugin.Context): Unit =
    context.addModels(BottomlessBottleRenderer.fluidModelID, BottomlessBottleRenderer.bottomlessBottleID)
    context.resolveModel().register: ctx =>
      if ctx.id() == SpectrumStorage.locate("item/bottomless_bottle") then
        BottomlessBottleItemModel()
      else
        null