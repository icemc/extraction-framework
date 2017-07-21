package org.dbpedia.extraction.mappings.rml.translation.model.factories

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.mappings.rml.translation.mapper.{RMLModelMapper, TemplateRMLMapper}
import org.dbpedia.extraction.mappings.rml.translation.model.rmlresources.{RMLPredicateObjectMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.translation.model.{RMLModel, RMLTemplateMapping, RMLTranslationModel}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.PageNode

/**
  * Factory that creates RML template mappings converted from DBpedia mappings using a triple store (Jena)
  */
class RMLTemplateMappingFactory extends RMLMappingFactory {



  def createMapping(page: PageNode, language: Language, mappings: Mappings): RMLTemplateMapping =
  {

    println("Loading RML Mapping: " + page.title.encodedWithNamespace)
    val rmlModel = new RMLTranslationModel(page.title, page.sourceUri)
    val rmlMapper = new RMLModelMapper(rmlModel)
    if(mappings.templateMappings.head._2.isInstanceOf[TemplateMapping]) {
      val templateMapping = mappings.templateMappings.head._2.asInstanceOf[TemplateMapping] // :|
      rmlMapper.addTemplateMapping(templateMapping)
    } else {
      val conditionalMapping = mappings.templateMappings.head._2.asInstanceOf[ConditionalMapping]
      rmlMapper.addConditionalMapping(conditionalMapping)
    }

    new RMLTemplateMapping(page.title.resourceIri, rmlModel)
  }


}
