package org.obolibrary.robot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.vocab.PROVVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Merge multiple ontologies into a single ontology.
 *
 * @author <a href="mailto:james@overton.ca">James A. Overton</a>
 */
public class MergeOperation {
  /** Logger. */
  private static final Logger logger = LoggerFactory.getLogger(MergeOperation.class);

  /**
   * Given a single ontology with zero or more imports, add all the imported axioms into the
   * ontology itself, return the modified ontology.
   *
   * @param ontology the ontology to merge
   * @return the new ontology
   */
  public static OWLOntology merge(OWLOntology ontology) {
    List<OWLOntology> ontologies = new ArrayList<>();
    ontologies.add(ontology);
    // One ontology will always collapse the import closure
    mergeInto(ontologies, ontology, false, true);
    return ontology;
  }

  /**
   * Merge one or more ontologies with the default merge options (do not include annotations, do not
   * collapse import closure).
   *
   * @param ontologies the list of ontologies to merge
   * @return the first ontology
   */
  public static OWLOntology merge(List<OWLOntology> ontologies) {
    return merge(ontologies, false, false);
  }

  /**
   * Given one or more ontologies, add all their axioms first ontology, and return the first
   * ontology. Option to include ontology annotations and collapse import closure.
   *
   * <p>We use a list instead of a set because OWLAPI judges identity simply by the ontology IRI,
   * even if two ontologies have different axioms.
   *
   * @param ontologies the list of ontologies to merge
   * @param includeAnnotations if true, ontology annotations should be merged; annotations on
   *     imports are not merged
   * @param collapseImportsClosure if true, imports closure from all ontologies included
   * @return the first ontology
   */
  public static OWLOntology merge(
      List<OWLOntology> ontologies, boolean includeAnnotations, boolean collapseImportsClosure) {
    return merge(ontologies, includeAnnotations, collapseImportsClosure, false, false);
  }

  /**
   * Given one or more ontologies, add all their axioms first ontology, and return the first
   * ontology. Option to include ontology annotations and collapse import closure.
   *
   * <p>We use a list instead of a set because OWLAPI judges identity simply by the ontology IRI,
   * even if two ontologies have different axioms.
   *
   * @param ontologies the list of ontologies to merge
   * @param includeAnnotations if true, ontology annotations should be merged; annotations on
   *     imports are not merged
   * @param collapseImportsClosure if true, imports closure from all ontologies included
   * @param definedBy if true, annotate all entities in the ontology with the ontology IRI
   * @param derivedFrom if true, annotate all axioms in the ontology with the ontology version IRI
   * @return the first ontology
   */
  public static OWLOntology merge(
      List<OWLOntology> ontologies,
      boolean includeAnnotations,
      boolean collapseImportsClosure,
      boolean definedBy,
      boolean derivedFrom) {
    OWLOntology ontology = ontologies.get(0);
    mergeInto(
        ontologies, ontology, includeAnnotations, collapseImportsClosure, definedBy, derivedFrom);
    return ontology;
  }

  /**
   * Given one or more ontologies, add all their axioms (including their imports closures) into the
   * first ontology, and return the first ontology. Replaced by with method using explicit options;
   * the mergeOptions map is not used by the MergeCommand.
   *
   * @param ontologies the list of ontologies to merge
   * @param mergeOptions a map of option strings, or null
   * @return the first ontology
   */
  public static OWLOntology merge(List<OWLOntology> ontologies, Map<String, String> mergeOptions) {
    OWLOntology ontology = ontologies.get(0);

    // Use collapseImportsClosure and includeAnnotations instead
    mergeInto(ontologies, ontology, false, true);
    return ontology;
  }

  /**
   * Given a source ontology and a target ontology, add all the axioms from the source ontology and
   * its import closure into the target ontology. The target ontology is not itself merged, so any
   * of its imports remain distinct.
   *
   * @param ontology the source ontology to merge
   * @param targetOntology the ontology to merge axioms into
   */
  public static void mergeInto(OWLOntology ontology, OWLOntology targetOntology) {
    List<OWLOntology> ontologies = new ArrayList<>();
    ontologies.add(ontology);
    // By default, do not include annotations and do not collapse import closure
    mergeInto(ontologies, targetOntology, false, false);
  }

  /**
   * Given one or more ontologies and a target ontology, add all the axioms from the listed
   * ontologies to the target ontology. Optionally, include annotations from the ontologies in the
   * list. It is recommended to use mergeInto with both includeAnnotations and
   * collapseImportsClosure options.
   *
   * @param ontologies the ontologies to merge
   * @param targetOntology the ontology to merge axioms into
   * @param includeAnnotations if true, ontology annotations should be merged; annotations on
   *     imports are not merged
   */
  public static void mergeInto(
      List<OWLOntology> ontologies, OWLOntology targetOntology, boolean includeAnnotations) {
    // By default, do not collapse the imports closure
    mergeInto(ontologies, targetOntology, includeAnnotations, false);
  }

  /**
   * Given a source ontology and a target ontology, add all the axioms from the source ontology and
   * its import closure into the target ontology. The target ontology is not itself merged, so any
   * of its imports remain distinct.
   *
   * @param ontology the source ontology to merge
   * @param targetOntology the ontology to merge axioms into
   * @param includeAnnotations true if ontology annotations should be merged; annotations on imports
   *     are not merged
   */
  public static void mergeInto(
      OWLOntology ontology, OWLOntology targetOntology, boolean includeAnnotations) {
    List<OWLOntology> ontologies = new ArrayList<>();
    ontologies.add(ontology);
    mergeInto(ontologies, targetOntology, includeAnnotations);
  }

  /**
   * Given a source ontology and a target ontology, add all the axioms from the source ontology into
   * the target ontology. Optionally, include annotations from the source and/or collapse the
   * imports closures.
   *
   * @param ontology the source ontology to merge
   * @param targetOntology the ontology to merge axioms into
   * @param includeAnnotations if true, ontology annotations should be merged; annotations on
   *     imports are not merged
   * @param collapseImportsClosure if true, imports closure from all ontologies included
   */
  public static void mergeInto(
      OWLOntology ontology,
      OWLOntology targetOntology,
      boolean includeAnnotations,
      boolean collapseImportsClosure) {
    List<OWLOntology> ontologies = new ArrayList<>();
    ontologies.add(ontology);
    mergeInto(ontologies, targetOntology, includeAnnotations, collapseImportsClosure);
  }

  /**
   * Given a list of ontologies and a target ontology, add all the axioms from the listed ontologies
   * and their import closure into the target ontology. The target ontology is not itself merged, so
   * any of its imports remain distinct.
   *
   * @param ontologies the list of ontologies to merge
   * @param targetOntology the ontology to merge axioms into
   */
  public static void mergeInto(List<OWLOntology> ontologies, OWLOntology targetOntology) {
    // By default, do not include annotations and do not collapse import closure
    mergeInto(ontologies, targetOntology, false, false);
  }

  /**
   * Given a list of ontologies and a target ontology, add all the axioms from the listed ontologies
   * and their import closure into the target ontology. The target ontology is not itself merged, so
   * any of its imports remain distinct, unless collapsing imports closure.
   *
   * @param ontologies the list of ontologies to merge
   * @param targetOntology the ontology to merge axioms into
   * @param includeAnnotations true if ontology annotations should be merged; annotations on imports
   *     are not merged
   * @param collapseImportsClosure true if imports closure from all ontologies included
   */
  public static void mergeInto(
      List<OWLOntology> ontologies,
      OWLOntology targetOntology,
      boolean includeAnnotations,
      boolean collapseImportsClosure) {
    mergeInto(ontologies, targetOntology, includeAnnotations, collapseImportsClosure, false, false);
  }

  /**
   * Given a list of ontologies and a target ontology, add all the axioms from the listed ontologies
   * and their import closure into the target ontology. The target ontology is not itself merged, so
   * any of its imports remain distinct, unless collapsing imports closure.
   *
   * @param ontologies the list of ontologies to merge
   * @param targetOntology the ontology to merge axioms into
   * @param includeAnnotations true if ontology annotations should be merged; annotations on imports
   *     are not merged
   * @param collapseImportsClosure true if imports closure from all ontologies included
   * @param definedBy if true, annotate all entities in the ontology with the ontology IRI
   * @param derivedFrom if true, annotate all axioms in the ontology with the ontology version IRI
   */
  public static void mergeInto(
      List<OWLOntology> ontologies,
      OWLOntology targetOntology,
      boolean includeAnnotations,
      boolean collapseImportsClosure,
      boolean definedBy,
      boolean derivedFrom) {
    for (OWLOntology ontology : ontologies) {
      if (collapseImportsClosure) {
        annotateProvenanceOfImports(ontology, targetOntology, definedBy, derivedFrom);
        // Merge the ontologies with imports included
        targetOntology
            .getOWLOntologyManager()
            .addAxioms(targetOntology, ontology.getAxioms(Imports.INCLUDED));
      } else {
        // Merge the ontologies with imports excluded
        Set<OWLImportsDeclaration> imports = targetOntology.getImportsDeclarations();
        try {
          OntologyHelper.removeImports(targetOntology);
        } catch (Exception e) {
          // Continue without removing imports
          continue;
        }
        if (definedBy) {
          annotateWithOntologyIRI(ontology, targetOntology, Imports.EXCLUDED);
        }
        if (derivedFrom) {
          annotateWithVersionIRI(ontology, targetOntology, Imports.EXCLUDED);
        }
        targetOntology
            .getOWLOntologyManager()
            .addAxioms(targetOntology, ontology.getAxioms(Imports.EXCLUDED));
        OWLOntologyManager manager = targetOntology.getOWLOntologyManager();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        // Re-add the imports
        for (OWLImportsDeclaration dec : imports) {
          manager.applyChange(new AddImport(targetOntology, dec));
        }
      }
      if (includeAnnotations) {
        for (OWLAnnotation annotation : ontology.getAnnotations()) {
          // Add each set of ontology annotations to the target ontology
          OntologyHelper.addOntologyAnnotation(targetOntology, annotation);
        }
      }
    }
    if (collapseImportsClosure) {
      // Remove import statements, as they've been merged in
      removeImports(targetOntology);
    }
  }

  /**
   * Annotates provenance of all axioms through considering imports.
   *
   * @param ontology the ontology to merge
   * @param targetOntology the ontology to merge axioms into
   * @param definedBy if true, annotate all entities in the ontology with the ontology IRI
   * @param derivedFrom if true, annotate all axioms in the ontology with the ontology version IRI
   */
  private static void annotateProvenanceOfImports(
      OWLOntology ontology, OWLOntology targetOntology, boolean definedBy, boolean derivedFrom) {
    if (definedBy) {
      for (OWLOntology importedOnt : ontology.getImports()) {
        annotateWithOntologyIRI(importedOnt, ontology, Imports.EXCLUDED);
      }
      annotateWithOntologyIRI(ontology, targetOntology, Imports.EXCLUDED);
    }
    if (derivedFrom) {
      for (OWLOntology importedOnt : ontology.getImports()) {
        annotateWithVersionIRI(importedOnt, ontology, Imports.EXCLUDED);
      }
      annotateWithVersionIRI(ontology, targetOntology, Imports.EXCLUDED);
    }
  }

  /**
   * Annotates all axioms in the ontology with the ontology version IRI if defined,annotates with
   * the ontology IRI otherwise.
   *
   * @param sourceOntology the ontology to annotate its all axioms.
   * @param targetOntology the ontology to add annotation
   * @param includeImportsClosure if INCLUDED, the imports closure is included.
   */
  private static void annotateWithVersionIRI(
      OWLOntology sourceOntology, OWLOntology targetOntology, Imports includeImportsClosure) {
    IRI provenanceIRI =
        sourceOntology
            .getOntologyID()
            .getVersionIRI()
            .or(sourceOntology.getOntologyID().getOntologyIRI().orNull());
    if (provenanceIRI != null) {
      OWLOntologyManager manager = targetOntology.getOWLOntologyManager();
      OWLAnnotationProperty annotationProp =
          manager
              .getOWLDataFactory()
              .getOWLAnnotationProperty(PROVVocabulary.WAS_DERIVED_FROM.getIRI());
      OWLDeclarationAxiom provenancePropDeclaration =
          manager.getOWLDataFactory().getOWLDeclarationAxiom(annotationProp);

      Set<OWLAxiom> sourceAxioms = sourceOntology.getAxioms(includeImportsClosure);
      for (OWLAxiom axiom : sourceAxioms) {
        if (!axiom.equals(provenancePropDeclaration)) {
          OntologyHelper.addAxiomAnnotation(
              targetOntology, axiom, annotationProp, provenanceIRI, false);
        }
      }

      manager.addAxiom(targetOntology, provenancePropDeclaration);
      if (sourceOntology != targetOntology) {
        sourceOntology.getOWLOntologyManager().removeAxioms(sourceOntology, sourceAxioms);
      }
    }
  }

  /**
   * Annotate all entities in the ontology with the ontology IRI.
   *
   * @param sourceOntology the ontology to annotate its entities
   * @param targetOntology the ontology to add annotation
   * @param includeImportsClosure if INCLUDED, the imports closure is included.
   */
  private static void annotateWithOntologyIRI(
      OWLOntology sourceOntology, OWLOntology targetOntology, Imports includeImportsClosure) {
    IRI ontIRI = sourceOntology.getOntologyID().getOntologyIRI().orNull();
    if (ontIRI != null) {
      OWLAnnotationProperty rdfsIsDefinedBy =
          targetOntology.getOWLOntologyManager().getOWLDataFactory().getRDFSIsDefinedBy();
      for (OWLEntity owlEntity : sourceOntology.getSignature(includeImportsClosure)) {
        OntologyHelper.addEntityAnnotation(
            targetOntology, owlEntity, rdfsIsDefinedBy, ontIRI, false);
      }
    }
  }

  /**
   * Given an ontology, remove the import statements.
   *
   * @param ontology the ontology to remove import statements from
   */
  private static void removeImports(OWLOntology ontology) {
    Set<OWLImportsDeclaration> oids = ontology.getImportsDeclarations();
    for (OWLImportsDeclaration oid : oids) {
      RemoveImport ri = new RemoveImport(ontology, oid);
      ontology.getOWLOntologyManager().applyChange(ri);
    }
  }

  /**
   * Return a map from option name to default option value, for all the available merge options.
   *
   * @return a map with default values for all available options
   */
  public static Map<String, String> getDefaultOptions() {
    Map<String, String> options = new HashMap<>();
    options.put("collapse-imports-closure", "false");

    return options;
  }
}
