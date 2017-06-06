/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *      
 * Copyright (c) 2010-2017, Silvio Peroni <essepuntato@gmail.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package it.essepuntato.lode;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.coode.owlapi.rdf.rdfxml.RDFXMLOntologyStorerFactory;
import org.mindswap.pellet.PelletOptions;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

/**
 * Core implementation of the LODE OWL -> HTML transformation
 */
public class LodeTransformer {

	private static final long serialVersionUID = 1L;

	private static final String defaultXsltURL = "http://lode.sourceforge.net/xslt";
	private static final String defaultCssLocation = "http://lode.sourceforge.net/css/";

	private String xsltURL = defaultXsltURL;
	private String cssLocation = defaultCssLocation;


	public LodeTransformer( String xsltURL, String cssLocation ) {
		this.xsltURL = xsltURL;
		this.cssLocation = cssLocation;
	}


	public String transform( URL ontologyURL ) throws TransformerException, OWLOntologyCreationException, OWLOntologyStorageException, URISyntaxException, IOException {
		return transform( ontologyURL, "en", false, false, false, false );
	}

	public String transform( URL ontologyURL,
	                         String lang,
	                         boolean useOWLAPI,
	                         boolean considerImportedOntologies,
	                         boolean considerImportedClosure,
	                         boolean useReasoner ) throws TransformerException, OWLOntologyCreationException, OWLOntologyStorageException, URISyntaxException, IOException {

		String content;

		if ( considerImportedOntologies || considerImportedClosure || useReasoner ) {
			useOWLAPI = true;
		}

		if ( useOWLAPI ) {
			content = parseWithOWLAPI( ontologyURL, useOWLAPI, considerImportedOntologies, considerImportedClosure, useReasoner );
		} else {
			SourceExtractor extractor = new SourceExtractor();
			extractor.addMimeTypes( MimeType.mimeTypes );

			content = extractor.exec( ontologyURL );
		}

				/*
				 * As it was before the new OWLAPI content =
				 * extractor.exec(ontologyURL); if (useOWLAPI) { content =
				 * parseWithOWLAPI(content, useOWLAPI,
				 * considerImportedOntologies, considerImportedClosure,
				 * useReasoner); }
				 */

		content = applyXSLTTransformation( content, ontologyURL.toString(), lang );

		return content;
	}


	/*
	 * Old version of the method (before upgrading OWLAPI) private String
	 * parseWithOWLAPI( String content, boolean useOWLAPI, boolean
	 * considerImportedOntologies, boolean considerImportedClosure, boolean
	 * useReasoner) throws OWLOntologyCreationException,
	 * OWLOntologyStorageException, URISyntaxException { String result =
	 * content;
	 * 
	 * if (useOWLAPI) {
	 * 
	 * List<String> removed = new ArrayList<String>(); if
	 * (!considerImportedClosure && !considerImportedOntologies) { result =
	 * removeImportedAxioms(result, removed); }
	 * 
	 * 
	 * OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	 * 
	 * OWLOntology ontology = manager.loadOntologyFromOntologyDocument( new
	 * StringDocumentSource(result));
	 * 
	 * if (considerImportedClosure || considerImportedOntologies) {
	 * Set<OWLOntology> setOfImportedOntologies = new HashSet<OWLOntology>(); if
	 * (considerImportedOntologies) {
	 * setOfImportedOntologies.addAll(ontology.getDirectImports()); } else {
	 * setOfImportedOntologies.addAll(ontology.getImportsClosure()); } for
	 * (OWLOntology importedOntology : setOfImportedOntologies) {
	 * manager.addAxioms(ontology, importedOntology.getAxioms()); } }
	 * 
	 * if (useReasoner) { ontology = parseWithReasoner(manager, ontology); }
	 * 
	 * StringDocumentTarget parsedOntology = new StringDocumentTarget();
	 * 
	 * manager.saveOntology(ontology, new RDFXMLOntologyFormat(),
	 * parsedOntology); result = parsedOntology.toString();
	 * 
	 * if (!removed.isEmpty() && !considerImportedClosure &&
	 * !considerImportedOntologies) { result = addImportedAxioms(result,
	 * removed); } }
	 * 
	 * return result; }
	 */

	private String parseWithOWLAPI( URL ontologyURL,
	                                boolean useOWLAPI,
	                                boolean considerImportedOntologies,
	                                boolean considerImportedClosure,
	                                boolean useReasoner ) throws OWLOntologyCreationException, OWLOntologyStorageException, URISyntaxException {
		String result = "";

		if (useOWLAPI) {
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			
			OWLOntology ontology = null;

			if (considerImportedClosure || considerImportedOntologies) {
				ontology = manager.loadOntology(IRI.create(ontologyURL.toString()));
				Set<OWLOntology> setOfImportedOntologies = new HashSet<OWLOntology>();
				if (considerImportedOntologies) {
					setOfImportedOntologies.addAll(ontology.getDirectImports());
				} else {
					setOfImportedOntologies.addAll(ontology.getImportsClosure());
				}
				for (OWLOntology importedOntology : setOfImportedOntologies) {
					manager.addAxioms(ontology, importedOntology.getAxioms());
				}
			} else {
				manager.setSilentMissingImportsHandling(true);
				ontology = manager.loadOntology(IRI.create(ontologyURL.toString()));
			}

			if (useReasoner) {
				ontology = parseWithReasoner(manager, ontology);
			}

			StringDocumentTarget parsedOntology = new StringDocumentTarget();

			// ensure storer is represent and registered
			manager.addOntologyStorer( new RDFXMLOntologyStorerFactory().createStorer() );
			manager.saveOntology(ontology, new RDFXMLOntologyFormat(), parsedOntology);

			result = parsedOntology.toString();
		}

		return result;
	}

	private String addImportedAxioms(String result, List<String> removed) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new ByteArrayInputStream(result.getBytes()));

			NodeList ontologies = document.getElementsByTagNameNS("http://www.w3.org/2002/07/owl#", "Ontology");
			if (ontologies.getLength() > 0) {
				Element ontology = (Element) ontologies.item(0);

				for (String toBeAdded : removed) {
					Element importElement = document.createElementNS("http://www.w3.org/2002/07/owl#", "owl:imports");
					importElement.setAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:resource", toBeAdded);
					ontology.appendChild(importElement);
				}
			}

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StreamResult output = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(document);
			transformer.transform(source, output);

			return output.getWriter().toString();
		} catch (ParserConfigurationException e) {
			return result;
		} catch (SAXException e) {
			return result;
		} catch (IOException e) {
			return result;
		} catch (TransformerConfigurationException e) {
			return result;
		} catch (TransformerFactoryConfigurationError e) {
			return result;
		} catch (TransformerException e) {
			return result;
		}
	}

	/*
	 * private String removeImportedAxioms(String result, List<String>
	 * removedImport) { DocumentBuilderFactory factory =
	 * DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
	 * try { DocumentBuilder builder = factory.newDocumentBuilder(); Document
	 * document = builder.parse(new ByteArrayInputStream(result.getBytes()));
	 * 
	 * NodeList ontologies =
	 * document.getElementsByTagNameNS("http://www.w3.org/2002/07/owl#",
	 * "Ontology"); for (int i = 0; i < ontologies.getLength() ; i++) { Element
	 * ontology = (Element) ontologies.item(i);
	 * 
	 * NodeList children = ontology.getChildNodes(); List<Element> removed = new
	 * ArrayList<Element>(); for (int j = 0; j < children.getLength(); j++) {
	 * Node child = children.item(j);
	 * 
	 * if ( child.getNodeType() == Node.ELEMENT_NODE &&
	 * child.getNamespaceURI().equals("http://www.w3.org/2002/07/owl#") &&
	 * child.getLocalName().equals("imports")) { removed.add((Element) child); }
	 * }
	 * 
	 * for (Element toBeRemoved : removed) {
	 * removedImport.add(toBeRemoved.getAttributeNS(
	 * "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource"));
	 * ontology.removeChild(toBeRemoved); } }
	 * 
	 * Transformer transformer =
	 * TransformerFactory.newInstance().newTransformer(); StreamResult output =
	 * new StreamResult(new StringWriter()); DOMSource source = new
	 * DOMSource(document); transformer.transform(source, output);
	 * 
	 * return output.getWriter().toString(); } catch
	 * (ParserConfigurationException e) { return result; } catch (SAXException
	 * e) { return result; } catch (IOException e) { return result; } catch
	 * (TransformerConfigurationException e) { return result; } catch
	 * (TransformerFactoryConfigurationError e) { return result; } catch
	 * (TransformerException e) { return result; } }
	 */

	private OWLOntology parseWithReasoner(OWLOntologyManager manager, OWLOntology ontology) {
		try {
			PelletOptions.load(new URL("http://" + cssLocation + "pellet.properties"));
			PelletReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner(ontology);
			reasoner.getKB().prepare();
			List<InferredAxiomGenerator<? extends OWLAxiom>> generators = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
			generators.add(new InferredSubClassAxiomGenerator());
			generators.add(new InferredClassAssertionAxiomGenerator());
			generators.add(new InferredDisjointClassesAxiomGenerator());
			generators.add(new InferredEquivalentClassAxiomGenerator());
			generators.add(new InferredEquivalentDataPropertiesAxiomGenerator());
			generators.add(new InferredEquivalentObjectPropertyAxiomGenerator());
			generators.add(new InferredInverseObjectPropertiesAxiomGenerator());
			generators.add(new InferredPropertyAssertionGenerator());
			generators.add(new InferredSubDataPropertyAxiomGenerator());
			generators.add(new InferredSubObjectPropertyAxiomGenerator());

			InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner, generators);

			OWLOntologyID id = ontology.getOntologyID();
			Set<OWLImportsDeclaration> declarations = ontology.getImportsDeclarations();
			Set<OWLAnnotation> annotations = ontology.getAnnotations();

			Map<OWLEntity, Set<OWLAnnotationAssertionAxiom>> entityAnnotations = new HashMap<OWLEntity, Set<OWLAnnotationAssertionAxiom>>();
			for (OWLClass aEntity : ontology.getClassesInSignature()) {
				entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
			}
			for (OWLObjectProperty aEntity : ontology.getObjectPropertiesInSignature()) {
				entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
			}
			for (OWLDataProperty aEntity : ontology.getDataPropertiesInSignature()) {
				entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
			}
			for (OWLNamedIndividual aEntity : ontology.getIndividualsInSignature()) {
				entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
			}
			for (OWLAnnotationProperty aEntity : ontology.getAnnotationPropertiesInSignature()) {
				entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
			}
			for (OWLDatatype aEntity : ontology.getDatatypesInSignature()) {
				entityAnnotations.put(aEntity, aEntity.getAnnotationAssertionAxioms(ontology));
			}

			manager.removeOntology(ontology);
			OWLOntology inferred = manager.createOntology(id);
			iog.fillOntology(manager, inferred);

			for (OWLImportsDeclaration decl : declarations) {
				manager.applyChange(new AddImport(inferred, decl));
			}
			for (OWLAnnotation ann : annotations) {
				manager.applyChange(new AddOntologyAnnotation(inferred, ann));
			}
			for (OWLClass aEntity : inferred.getClassesInSignature()) {
				applyAnnotations(aEntity, entityAnnotations, manager, inferred);
			}
			for (OWLObjectProperty aEntity : inferred.getObjectPropertiesInSignature()) {
				applyAnnotations(aEntity, entityAnnotations, manager, inferred);
			}
			for (OWLDataProperty aEntity : inferred.getDataPropertiesInSignature()) {
				applyAnnotations(aEntity, entityAnnotations, manager, inferred);
			}
			for (OWLNamedIndividual aEntity : inferred.getIndividualsInSignature()) {
				applyAnnotations(aEntity, entityAnnotations, manager, inferred);
			}
			for (OWLAnnotationProperty aEntity : inferred.getAnnotationPropertiesInSignature()) {
				applyAnnotations(aEntity, entityAnnotations, manager, inferred);
			}
			for (OWLDatatype aEntity : inferred.getDatatypesInSignature()) {
				applyAnnotations(aEntity, entityAnnotations, manager, inferred);
			}

			return inferred;
		} catch (FileNotFoundException e1) {
			return ontology;
		} catch (MalformedURLException e1) {
			return ontology;
		} catch (IOException e1) {
			return ontology;
		} catch (OWLOntologyCreationException e) {
			return ontology;
		}
	}

	private void applyAnnotations(OWLEntity aEntity, Map<OWLEntity, Set<OWLAnnotationAssertionAxiom>> entityAnnotations, OWLOntologyManager manager, OWLOntology ontology) {
		Set<OWLAnnotationAssertionAxiom> entitySet = entityAnnotations.get(aEntity);
		if (entitySet != null) {
			for (OWLAnnotationAssertionAxiom ann : entitySet) {
				manager.addAxiom(ontology, ann);
			}
		}
	}

	private String applyXSLTTransformation( String source, String ontologyUrl, String lang ) throws TransformerException, IOException {
		TransformerFactory tfactory = new net.sf.saxon.TransformerFactoryImpl();

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		Transformer transformer = tfactory.newTransformer( new StreamSource( xsltURL ) );

		transformer.setParameter("css-location", cssLocation);
		transformer.setParameter("lang", lang);
		transformer.setParameter("ontology-url", ontologyUrl);
		transformer.setParameter("source", cssLocation + "source");

		StreamSource inputSource = new StreamSource(new StringReader(source));

		transformer.transform(inputSource, new StreamResult(output));

		return output.toString();
	}

	public static String resolveDefaultCSS() {
		String xsltLocation = resolveDefaultXLS();
		return xsltLocation.substring( 0, xsltLocation.lastIndexOf( "extraction.xsl" ) );
	}

	public static String resolveDefaultXLS() {
		URL xsltURL = Thread.currentThread().getContextClassLoader().getResource( "extraction.xsl" );
		try {
			return xsltURL.toURI().toString();
		} catch ( URISyntaxException e ) {
			e.printStackTrace();
			return defaultXsltURL;
		}
	}
}
