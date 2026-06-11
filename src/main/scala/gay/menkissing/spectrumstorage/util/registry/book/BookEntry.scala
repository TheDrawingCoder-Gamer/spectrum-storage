package gay.menkissing.spectrumstorage.util.registry.book

import com.google.gson.{JsonArray, JsonObject}
import com.klikli_dev.modonomicon.api.datagen.book.condition.BookConditionModel
import com.klikli_dev.modonomicon.api.datagen.book.page.BookPageModel
import com.klikli_dev.modonomicon.api.datagen.book.{BookCategoryModel, BookEntryParentModel, BookIconModel}
import gay.menkissing.spectrumstorage.util.registry.provider.generators.SpectrumStorageBaseBookProvider
import net.minecraft.core.HolderLookup
import net.minecraft.data.CachedOutput
import net.minecraft.data.PackOutput.Target
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.ItemLike

import java.util.concurrent.CompletableFuture
import scala.collection.mutable

class BookEntry(val location: EntryLocation, var name: String, var translationId: ResourceLocation):
  var category: BookCategoryModel = null
  val parents: mutable.ListBuffer[BookEntryParentModel] = mutable.ListBuffer.empty
  var description: String = ""
  var icon: BookIconModel = null
  var x: Int = 0
  var y: Int = 0
  var entryBackgroundUIndex: Int = 0
  var entryBackgroundVIndex: Int = 0

  var hideWhileLocked: Boolean = false
  var showWhenAnyParentUnlocked: Boolean = false
  private val pages: mutable.ListBuffer[BookPageModel[?]] = mutable.ListBuffer.empty
  var condition: Option[BookConditionModel[?]] = None
  
  private val langEntries = mutable.HashMap[String, String]()
  
  def withCategory(category: BookCategoryModel): this.type =
    this.category = category
    this
  def withName(name: String): this.type =
    this.name = name
    this
  def withDescription(desc: String): this.type =
    this.description = desc
    this
  def withIcon(icon: BookIconModel): this.type =
    this.icon = icon
    this
  def withIcon(item: ItemLike): this.type =
    withIcon(BookIconModel.create(item))
  def withLocation(x: Int, y: Int): this.type =
    this.x = x
    this.y = y
    this
  def withEntryBackground(u: Int, v: Int): this.type =
    this.entryBackgroundUIndex = u
    this.entryBackgroundVIndex = v
    this
  def withHideWhileLocked(hideWhileLocked: Boolean): this.type =
    this.hideWhileLocked = hideWhileLocked
    this
  def withCondition(condition: BookConditionModel[?]): this.type =
    this.condition = Some(condition)
    this
  def noCondition: this.type =
    this.condition = None
    this

  private def escapeBody(body: String): String =
    body.trim.replace("\r", "").replace("\n", "\\\n")

  def addPage(pageMaker: BookEntry.PageMaker): this.type =
    val pageN = pages.size + 1
    val transGetter: BookEntry.TransGetter =
      (index, text) =>
        val txt = s"book.${translationId.getNamespace}.${location.book.getPath}.${translationId.getPath}.page$pageN.$index"
        langEntries(txt) = escapeBody(text)
        txt
    pages += pageMaker(transGetter)
    this
  
  def toJson(provider: HolderLookup.Provider): JsonObject =
    val json = new JsonObject()
    require(icon != null && category != null)
    json.addProperty("category", this.category.getId.toString)
    json.addProperty("name", this.name)
    json.addProperty("description", this.description)
    json.add("icon", icon.toJson(provider))
    json.addProperty("x", this.x)
    json.addProperty("y", this.y)
    json.addProperty("background_u_index", this.entryBackgroundUIndex)
    json.addProperty("background_v_index", this.entryBackgroundVIndex)
    json.addProperty("hide_while_locked", this.hideWhileLocked)
    json.addProperty("show_when_any_parent_unlocked", this.showWhenAnyParentUnlocked)
    
    if this.pages.nonEmpty then
      val pagesArray = new JsonArray()
      this.pages.foreach: page =>
        pagesArray.add(page.toJson(location.id, provider))
      json.add("pages", pagesArray)
    
    this.condition.foreach: cond =>
      json.add("condition", cond.toJson(location.id, provider))
    
    
    
    json

  def submit(provider: SpectrumStorageBaseBookProvider): Unit =
    langEntries.foreach: (k, v) =>
      provider.add(k, v)
      
  
object BookEntry:
  trait TransGetter:
    def apply(index: String, text: String): String
    
    final def text(txt: String): String = apply("text", txt)
    final def title(txt: String): String = apply("title", txt)
  
  trait PageMaker:
    def apply(transGetter: TransGetter): BookPageModel[?]
    
  
    
    
  
  

