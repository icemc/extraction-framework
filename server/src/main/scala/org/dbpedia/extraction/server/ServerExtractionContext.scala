package org.dbpedia.extraction.server

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.sources.Source


class ServerExtractionContext(private val lang : Language, extractionManager : ExtractionManager)
{
    def language : Language = lang

    def ontology : Ontology = extractionManager.ontology

    def redirects : Redirects = extractionManager.redirects

    def mappingsSource : Source = extractionManager.mappingSource(lang)

    // not needed in server: commonsSource
    // not needed in server: articlesSource
}