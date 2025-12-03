package gay.menkissing.lumospectrum.util.registry.helper

import gay.menkissing.lumospectrum.util.registry.InfoCollector

import scala.collection.mutable

class LangHelper(owner: InfoCollector, idGetter: String => String):
  private val langs = mutable.HashMap[String, String]()
  
  def lang(key: String, value: String): this.type =
    langs(idGetter(key)) = value
    this
    
  def save(): Unit =
    langs.foreach: (k, v) =>
      owner.addRawLang(k, v)
