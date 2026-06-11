package gay.menkissing.spectrumstorage.datagen.providers

import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel
import com.klikli_dev.modonomicon.api.datagen.book.condition.BookAdvancementConditionModel
import com.klikli_dev.modonomicon.api.datagen.book.page.{BookSpotlightPageModel, BookTextPageModel}
import de.dafuqs.spectrum.registries.{SpectrumBlocks, SpectrumEnchantmentKeys, SpectrumItems}
import gay.menkissing.spectrumstorage.SpectrumStorage
import gay.menkissing.spectrumstorage.content.{SpectrumStorageBlocks, SpectrumStorageItems}
import gay.menkissing.spectrumstorage.util.registry.book.{BookEntry, EntryLocation}
import gay.menkissing.spectrumstorage.util.registry.provider.generators.SpectrumStorageBaseBookProvider
import gay.menkissing.spectrumstorage.util.registry.provider.generators.book.BookPedestalPageModel
import gay.menkissing.spectrumstorage.util.resources.{*, given}
import net.minecraft.core.registries.Registries
import net.minecraft.core.{Holder, HolderLookup, HolderOwner}
import net.minecraft.data.PackOutput
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.{Item, ItemStack}
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.enchantment.{Enchantment, Enchantments, ItemEnchantments}
import net.minecraft.world.level.ItemLike

import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

final class SpectrumStorageBook(output: PackOutput, lookup: CompletableFuture[HolderLookup.Provider], clientActive: Boolean, serverActive: Boolean) extends SpectrumStorageBaseBookProvider(SpectrumStorage.ModId, output, lookup, clientActive, serverActive):
  def addTooltip(item: Supplier[? <: Item], sub: String, value: String): Unit =
    val it = item.get()
    val key = s"${it.getDescriptionId}.tooltip.$sub"
    add(key, value)

  override def addTranslations(): Unit =
    addBlock(SpectrumStorageBlocks.bottomlessBarrel, "Bottomless Barrel")
    addBlock(SpectrumStorageBlocks.bottomlessAmphora, "Bottomless Amphora")
    addBlock(SpectrumStorageBlocks.filterChest, "Filter Chest")
    addBlock(SpectrumStorageBlocks.bottomlessShelf, "Bottomless Shelf")

    addItem(SpectrumStorageItems.bottomlessBottle, "Bottomless Bottle")
    addTooltip(SpectrumStorageItems.bottomlessBottle, "empty", "Empty")
    addTooltip(SpectrumStorageItems.bottomlessBottle, "usage_pickup", "Use to pickup")
    addTooltip(SpectrumStorageItems.bottomlessBottle, "usage_place", "Sneak-use to place")
    addTooltip(SpectrumStorageItems.bottomlessBottle, "count_mb", "%1$s mB / %2$s buckets")
    addItem(SpectrumStorageItems.toolContainer, "Tool Container")

    add("book.spectrumstorage.added_by_spectrumstorage", "§oAdded by Spectrum Storage")

    List(
      "bottomless_barrel" -> "Bottomless Barrel",
      "bottomless_amphora" -> "Bottomless Amphora",
      "filter_chest" -> "Filter Chest"
    ).foreach: (k, v) =>
      add(s"container.spectrumstorage.$k", v)



  override def addEntries(lookup: HolderLookup.Provider): Unit =
    val spectrumBook = ResourceLocation.fromNamespaceAndPath("spectrum", "guidebook")
    val magicalCategory = ResourceLocation.fromNamespaceAndPath("spectrum", "magical_blocks")
    val equipCategory = ResourceLocation.fromNamespaceAndPath("spectrum", "equipment")
    val commonDesc = "book.spectrumstorage.added_by_spectrumstorage"

    def itemUnlock(id: String): BookAdvancementConditionModel =
      BookAdvancementConditionModel
        .create()
        .withAdvancementId(s"spectrumstorage:unlocks/items/$id")

    def blockUnlock(id: String): BookAdvancementConditionModel =
      BookAdvancementConditionModel
        .create()
        .withAdvancementId(s"spectrumstorage:unlocks/blocks/$id")
    def entryLoc(path: String): EntryLocation =
      EntryLocation(spectrumBook, ResourceLocation.fromNamespaceAndPath("spectrum", path))

    def firstPage(item: ItemLike, text: String): BookSpotlightPageModel =
      BookSpotlightPageModel
        .create()
        .withText(text)
        .withItem(Ingredient.of(item))
        .withTitle(item.asItem().getDescriptionId)

    def pedestalPage(recipe: String, text: String): BookPedestalPageModel =
      BookPedestalPageModel
        .create()
        .withText(text)
        .withRecipeId(recipe)
        .withTitle("container.spectrum.rei.pedestal_recipe")

    def commonEntry(item: ItemLike, entry: BookEntry)(firstTxt: String): BookEntry =
      entry
        .withName(item.asItem().getDescriptionId)
        .withDescription(commonDesc)
        .withIcon(item)
        .withHideWhileLocked(true)
        .addPage(
          trans => firstPage(item, trans.text(firstTxt))
        )
    val enchantLookup = lookup.lookupOrThrow(Registries.ENCHANTMENT)

    // HACK : (
    // dummy out this so MAYBE it works
    val powerEnchant = enchantLookup.getOrThrow(Enchantments.POWER)
    val voidingEnchant = enchantLookup.getOrThrow(SpectrumEnchantmentKeys.VOIDING)

    val powerVStack =
      ItemStack(SpectrumStorageItems.bottomlessBottle.get())
    powerVStack.enchant(powerEnchant, 5)
    val voidingBundle =
      ItemStack(SpectrumBlocks.BOTTOMLESS_BUNDLE.asItem())
    voidingBundle.enchant(voidingEnchant, 1)

    val buildEnchanter = BookAdvancementConditionModel.create().withAdvancementId("spectrum:midgame/build_enchanting_structure")

    def voidingPage(text: String): BookSpotlightPageModel =
      BookSpotlightPageModel
        .create()
        .withCondition(buildEnchanter)
        .withItem(voidingBundle)
        .withTitle("enchantment.spectrum.voiding")
        .withText(
          text
        )

    this.addEntry(entryLoc("equipment/bottomless_bottle"), equipCategory, SpectrumStorageItems.bottomlessBottle.get().location) {
      entry =>
        commonEntry(SpectrumStorageItems.bottomlessBottle, entry)(
          """
            |I find myself often carrying half a dozen buckets holding the same thing. It would be nice if I could just cram all that liquid in one item.
            |
            |The bottomless bottle does just that - it stores a large amount of liquid inside itself.
            |""".stripMargin
        )
          .withLocation(3, 4)
          .withCondition(itemUnlock("bottomless_bottle"))
          .addPage(trans =>
            pedestalPage(
              "spectrumstorage:pedestal/tier2/bottomless_bottle",
              trans.text("Right-click picks up liquid, while sneak right-click places it.")
            )
          )
          .addPage(trans =>
            BookSpotlightPageModel
              .create()
              .withTitle("enchantment.minecraft.power")
              .withCondition(buildEnchanter)
              .withItem(powerVStack)
              .withText(trans.text("Power increases its capacity fourfold each level."))
          )
    }
    this.addEntry(entryLoc("equipment/tool_container"), equipCategory, SpectrumStorageItems.toolContainer.get().location) {
        entry =>
          entry
            .withName(SpectrumStorageItems.toolContainer.get().getDescriptionId)
            .withLocation(4, 4)
            .withDescription(commonDesc)
            .withCondition(itemUnlock("tool_container"))
            .withIcon(SpectrumStorageItems.toolContainer.get())
            .withHideWhileLocked(true)
            .addPage(trans =>
              firstPage(
                SpectrumStorageItems.toolContainer.get(),
                trans.text(
                  """
                    |On my travels I find myself carrying half a tool shed with me.
                    |
                    |I can mitigate this somewhat by making a tool container to keep all my tools in.
                    |""".stripMargin
                ))
            )
            .addPage(trans =>
              pedestalPage("spectrumstorage:pedestal/tier2/tool_container",
                trans.text("*Don't ask how they all fit*"))
            )
      }
    this.addEntry(entryLoc("magical_blocks/bottomless_amphora"), magicalCategory, SpectrumStorageBlocks.bottomlessAmphora.get().location) {
        entry =>
          entry
            .withName(SpectrumStorageBlocks.bottomlessAmphora.get().getDescriptionId)
            .withLocation(1, 5)
            .withDescription(commonDesc)
            .withCondition(blockUnlock("bottomless_amphora"))
            .withIcon(SpectrumStorageBlocks.bottomlessAmphora.get())
            .withHideWhileLocked(true)
            .addPage(trans =>
              firstPage(SpectrumStorageBlocks.bottomlessAmphora.get(),
                trans.text(
                  """
                    |I had thought that barrels could store a lot, but amphoras can store double that. So if I make a bottomless amphora...
                    |
                    |The bottomless amphora can store a whopping 54 bottomless bundles or bottles inside of it!
                    |""".stripMargin
                ))
            )
            .addPage(trans =>
              pedestalPage("spectrumstorage:pedestal/tier4/bottomless_amphora",
                trans.text("*This is getting absurd*")
              )
            )
            .addPage(trans =>
              BookTextPageModel
                .create()
                .withText(trans.text(
                  """
                    |Just like the bottomless shelf and bottomless barrel before it, the bottomless amphora remembers what items and fluids were inside its items, meaning you can use it as a filter.
                    |""".stripMargin
                ))
            ).addPage(trans =>
              voidingPage(
                trans.text(
                  """
                    |Like the bottomless shelf and bottomless barrel, if I insert a bottomless bundle that has the Curse of the Void into
                    |my bottomless amphora, it will still keep its filter and won't accept any
                    |arbitrary items. Any items inserted that overflow the bundle will be voided.
                    |""".stripMargin.linesIterator.mkString(" ")
                )
              )
            )
      }
    this.addEntry(entryLoc("magical_blocks/bottomless_barrel"), magicalCategory, SpectrumStorageBlocks.bottomlessBarrel.get().location) {
        entry =>
          entry
            .withName(SpectrumStorageBlocks.bottomlessBarrel.get().getDescriptionId)
            .withLocation(0, 5)
            .withDescription(commonDesc)
            .withCondition(blockUnlock("bottomless_barrel"))
            .withIcon(SpectrumStorageBlocks.bottomlessBarrel.get())
            .withHideWhileLocked(true)
            .addPage(trans =>
              firstPage(SpectrumStorageBlocks.bottomlessBarrel.get(),
                trans.text(
                  """
                    |While bottomless shelves are great, I find myself making complicated systems to access many of them.
                    |It would be nice if I could compact them even more.
                    |
                    |The bottomless barrel does just that - it can hold up to 27 bottomless bundles or bottles.
                    |""".stripMargin
                )
              )
            ).addPage(trans =>
              pedestalPage(
                "spectrumstorage:pedestal/tier3/bottomless_barrel",
                trans.text("*That's a lot of stuff*")
              )
            ).addPage(trans =>
              BookTextPageModel
                .create()
                .withText(
                  trans.text("""
                          |Just like the bottomless shelf, the bottomless barrel will remember what items and fluids were inside its items, meaning you can use it as a filter
                          |""".stripMargin)
                )
            )
            .addPage(trans =>
              voidingPage(
                  trans.text(
                    """
                      |Like the bottomless shelf, if I insert a bottomless bundle that has the Curse of the Void into
                      |my bottomless barrel, it will still keep its filter and won't accept any
                      |arbitrary items. Any items inserted that overflow the bundle will be voided.
                      |""".stripMargin.linesIterator.mkString(" ")
                    // ^ im not having a 200 character long line
                  )
                )
            )
      }
    this.addEntry(entryLoc("magical_blocks/bottomless_shelf"), magicalCategory, SpectrumStorageBlocks.bottomlessShelf.get().location) {
        entry =>
          commonEntry(SpectrumStorageBlocks.bottomlessShelf.get(), entry)(
              """
                |My bottomless bottle seems to be too unstable to place down on its own - So why not put it on a shelf?
                |
                |The bottomless shelf can hold 6 bottomless bundles or 6 bottomless bundles, or a mix of them.
                |""".stripMargin
          )
            .withLocation(1, 4)
            .withCondition(blockUnlock("bottomless_shelf"))
            .addPage(trans =>
              pedestalPage(
                "spectrumstorage:pedestal/tier2/bottomless_shelf",
                trans.text("*Don't be shelfish!*")
              )
            )
            .addPage(trans =>
              BookTextPageModel
                .create()
                .withText(
                  trans.text(
                    """
                      |A bottomless shelf will remember what items and fluids were inside its items, meaning you can use it as a filter.
                      |""".stripMargin
                  )
                )
            )
            .addPage(trans =>
              voidingPage(
                trans.text(
                  """
                    |If I insert a bottomless bundle that has the Curse of the Void into
                    |my bottomless shelf, it will still keep its filter and won't accept any
                    |arbitrary items. Any items inserted that overflow the bundle will be voided.
                    |""".stripMargin.linesIterator.mkString(" ")
                    // ^ im not having a 200 character long line
                )
              )
            )
      }
    this.addEntry(entryLoc("magical_blocks/filter_chest"), magicalCategory, SpectrumStorageBlocks.filterChest.get().location) {
        entry =>
          commonEntry(SpectrumStorageBlocks.filterChest.get(), entry)(
            """
              |The machines I build to sort my vast catalog of items tend to get quite large. It would be nice to have something that easily filters them.
              |
              |The filter barrel can filter up to 18 unique items, but it can only hold 9 stacks of items. It will only allow insertion of items if they are in its filter.
              |""".stripMargin
          )
          .withLocation(0, 4)
          .withCondition(blockUnlock("filter_chest"))
          .addPage(trans =>
            pedestalPage(
              "spectrumstorage:pedestal/tier3/filter_chest",
              trans.text("*Filter? I hardly know her!*")
            )
          )
      }
