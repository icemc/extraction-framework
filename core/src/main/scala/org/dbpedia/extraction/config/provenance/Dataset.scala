package org.dbpedia.extraction.config.provenance

import java.net.URI
import java.util.MissingFormatArgumentException

import org.dbpedia.extraction.ontology.{DBpediaNamespace, RdfNamespace}
import org.dbpedia.extraction.util.{ConfigUtils, Language, WikiUtil}
import org.dbpedia.extraction.wikiparser.WikiParserException

import scala.util.{Failure, Success, Try}

/**
 * The quads generated by the DBpedia framework are organized in a number of datasets.
 */
class Dataset private[provenance](
   naturalName: String, //The title of the dataset
   descr: String = null, //The description used for documentation
   lang: Language = null, //The language (when dealing with a language specific version of the dataset)
   versionEntry: String = null, //The DBpedia version of the Dataset (e.g. 2016-04). If supplied this dataset is specific to (language and) DBpedia version.
   fileName: String = null, //The name of this Dataset used in file names and uris (if null this will generated form the naturalName)
   var sources: Seq[Dataset] = Seq.empty[Dataset], //The Datasets out of which this dataset was extracted/generated...
   result: String = null, //Uri of the Extractor/script which produced this dataset (see class DBpediaAnnotations)
   input: Seq[String] = Seq.empty[String], //Uris of the Extractors/scripts which use this dataset as in input (see class DBpediaAnnotations)
   depr: String = null, //last DBpedia version the dataset was in use (and now deprecated): e.g. 2015-10
   var traits: DatasetTrait.ValueSet = DatasetTrait.ValueSet.empty, //The set of traits the dataset has. According to these traits a dataset will be post-processed.
   defaultGraf: String = null, //The default graph name is usually the language specific dbpedia namespace IRI (if this is null). With this extension you can add to the path of this IRI to define secondary graphs.
   var keywords: Seq[String] = Seq.empty[String] //also for documentation
   )
{

  val description = Option(descr)
  val language = Option(lang)

  val inputFor: Seq[Try[URI]] = input.map(x => Try{new URI(x)})

  val resultOf: Try[URI] = Option(result) match{
    case Some(u) => Try{new URI(u)}
    case None => Failure(new IllegalArgumentException("No target class for inputFor provided."))
  }

  val name = WikiUtil.wikiDecode(naturalName.trim)
  if (name.isEmpty) throw new WikiParserException("dataset name must not be empty")

  /** Wiki-encoded dataset name */
  val encoded = WikiUtil.wikiEncode((if(Option(fileName).nonEmpty && fileName.trim.nonEmpty) fileName.trim else name).replace("-", "_")).toLowerCase

  val canonicalUri = RdfNamespace.fullUri(DBpediaNamespace.DATASET, encoded)

  val version = ConfigUtils.parseVersionString(versionEntry)

  val deprecatedSince = ConfigUtils.parseVersionString(depr)

  lazy val canonicalVersion = if(isCanonical) this else copyDataset(lang = null, versionEntry = null)

  def getLanguageVersion(language: Language, version: String = null): Dataset ={
    ConfigUtils.parseVersionString(version) match
    {
      case Success(v) => this.copyDataset(lang = language, versionEntry = v)
      case Failure(e) => this.copyDataset(lang = language)
    }
  }

  def languageUri = this.language match{
    case Some(lang) => canonicalUri + "?lang=" + lang.wikiCode
    case None => canonicalUri
  }

  def versionUri = this.language match {
    case Some(l) => version match
    {
      case Success(v) => this.languageUri + "&dbpv=" + v
      case Failure(e) => this.languageUri
    }
    case None => canonicalUri
  }

  def isCanonical = language match {
    case Some(lang) => false
    case None => true
  }

  def getDistributionUri(distType: String, extension: String) ={
    val lang = this.language match{
      case Some(l) => l
      case None => throw new MissingFormatArgumentException("Dataset is language unspecific and can therefore not provide distribution URIs. Please specify a language first!.")
    }
    this.versionUri + "&" + distType  + "=" + this.encoded +"_" + lang.wikiCode + extension
  }

  def getRelationUri(role: String, target: Dataset) ={
    val lang = this.language match{
      case Some(l) => l
      case None => throw new MissingFormatArgumentException("Dataset is language unspecific and can therefore not provide dataset relation URIs. Please specify a language first!.")
    }
    if(role.matches("\\w+") && target.language.nonEmpty)
      this.versionUri + "&relation=" + role + "&target=" + target.encoded
    else
      throw new MissingFormatArgumentException("Target dataset is language unspecific and can therefore not provide dataset relation URIs. Please specify a language first!.")
  }

  def defaultGraph: Option[String] = {
    def appendNamespaceByTrait(ns: String): String = {
      var ret = ns
      this.traits.foreach( trai => trai match{
        case DatasetTrait.Unredirected => ret += "/unredirected"
        case DatasetTrait.EnglishUris => ret += "/en_uris"
        case DatasetTrait.WikidataUris => ret += "/wkd_uris"
        case _=>
      })
      ret
    }
    this.language match {
      case Some(l) => Option(this.defaultGraf) match{
        case Some(dg) => dg.trim match{
          case str if str == "namespace" => Some(appendNamespaceByTrait(l.dbpediaUri))
          case str if str == "dataset" => Some(appendNamespaceByTrait(l.dbpediaUri) + "/" + this.encoded)
          case str if str.trim.matches("(\\w|-|_)+") => Some(appendNamespaceByTrait(l.dbpediaUri) + "/" + str.trim.toLowerCase())
          case _ => throw new IllegalArgumentException("Default graph entry is missing or malformed for dataset: " + this.encoded)
        }
        case None => throw new IllegalArgumentException("Default graph entry is missing or malformed for dataset: " + this.encoded)
      }
      case None => None
    }
  }

  def isLinkedDataDataset = traits.contains(DatasetTrait.LinkedData)

  override def toString = name

  override def hashCode = encoded.hashCode

  override def equals(other : Any) = other match {
    case that: Dataset => this.encoded == that.encoded
    case _ => false
  }

  //maybe extend scope but keep this as closed as possible
  private[provenance] def copyDataset(
     naturalName: String = this.naturalName,
     descr: String = this.descr,
     lang: Language = this.lang,
     versionEntry: String = this.versionEntry,
     fileName: String = this.fileName,
     sources: Seq[Dataset] = this.sources,
     result: String = this.result,
     input: Seq[String] = this.input,
     depr: String = this.depr,
     traits: DatasetTrait.ValueSet = this.traits,
     defaultGraphExt: String = this.defaultGraf,
     keywords: Seq[String] = this.keywords
  ) : Dataset = new Dataset(
      naturalName,
      descr,
      lang,
      versionEntry,
      fileName,
      sources,
      result,
      input,
      depr,
      traits,
      defaultGraphExt,
      keywords
  )
}
