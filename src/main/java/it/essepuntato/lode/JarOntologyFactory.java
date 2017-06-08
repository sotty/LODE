package it.essepuntato.lode;

import org.semanticweb.owlapi.annotations.HasPriority;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import sun.net.www.protocol.jar.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Optional;

@HasPriority( Double.MAX_VALUE )
public class JarOntologyFactory implements OWLOntologyFactory {

	OWLOntologyFactory defaultFactory;
	Optional<URL> catalogURL;

	public JarOntologyFactory( OWLOntologyFactory defaultFactory, Optional<URL> catalogURL ) {
		this.defaultFactory = defaultFactory;
		this.catalogURL = catalogURL;
	}

	@Override
	public OWLOntology createOWLOntology( OWLOntologyManager manager, OWLOntologyID ontologyID, IRI documentIRI, OWLOntologyFactory.OWLOntologyCreationHandler handler ) throws OWLOntologyCreationException {
		return defaultFactory.createOWLOntology( manager, ontologyID, documentIRI, handler );
	}

	@Override
	public OWLOntology loadOWLOntology( OWLOntologyManager manager, OWLOntologyDocumentSource documentSource, OWLOntologyFactory.OWLOntologyCreationHandler handler, OWLOntologyLoaderConfiguration configuration ) throws OWLOntologyCreationException {
		try {
			String source = documentSource.getDocumentIRI().getIRIString();
			if ( isRelativeURL( source ) && catalogURL.isPresent() ) {
				source = normalizeRelativeURL( source, catalogURL.get().toString() );
			}
			URL sourceUrl = new URL( source );

			JarURLConnection jarl = new sun.net.www.protocol.jar.JarURLConnection( sourceUrl, new Handler() );
			InputStream is = jarl.getJarFile().getInputStream( jarl.getJarEntry() );
			return manager.loadOntologyFromOntologyDocument( is );
		} catch ( IOException ioe ) {
			ioe.printStackTrace();
		}
		return null;
	}

	private String normalizeRelativeURL( String relative, String absolute ) {
		String normalized = absolute.substring( 0, absolute.lastIndexOf( '/' ) + 1 ) +  relative;
		return normalized;
	}

	@Override
	public boolean canCreateFromDocumentIRI( IRI documentIRI ) {
		return defaultFactory.canCreateFromDocumentIRI( documentIRI );
	}

	@Override
	public boolean canAttemptLoading( OWLOntologyDocumentSource documentSource ) {
		String iriString = documentSource.getDocumentIRI().getIRIString();

		if ( iriString.startsWith( "jar:" ) ) {
			// the main purpose of this class is to support ontologies packaged within a jar,
			// with URLs pointing to them
			return true;
		}

		if ( documentSource.hasAlredyFailedOnStreams() && isRelativeURL( iriString ) && catalogURL.isPresent() ) {
			// relative path -> will be resolved against the base of a catalog entry within a jar
			// assumes the catalog is in the same folder as the ontology being loaded
			return true;
		}

		return false;
	}

	private boolean isRelativeURL( String url ) {
		return url.indexOf( ":" ) < 0;
	}

}
