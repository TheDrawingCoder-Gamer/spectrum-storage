package gay.menkissing.spectrumstorage.util.registry.helper

import gay.menkissing.spectrumstorage.util.registry.InfoCollector

import collection.mutable

class GuidebookLangHelper(owner: InfoCollector, entryName: String):
  private val langs = mutable.HashMap[String, String]()
  private var curPage = 0

  private def escapeBody(body: String): String =
    body.trim.replace("\r", "").replace("\n", "\\\n")

  def addPage(title: String, body: String): this.type =
    val bodyId = s"book.${owner.modid}.guidebook.$entryName.page$curPage.text"
    val titleId = s"book.${owner.modid}.guidebook.$entryName.page$curPage.title"
    curPage += 1
    langs(bodyId) = escapeBody(body)
    langs(titleId) = title
    this

  def addPage(body: String): this.type =
    val bodyId = s"book.${owner.modid}.guidebook.$entryName.page$curPage.text"
    curPage += 1
    langs(bodyId) = escapeBody(body)
    this

  def save(): Unit =
    langs.foreach: (k, v) =>
      owner.addRawLang(k, v)
