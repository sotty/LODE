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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LodeComponentTest {

	@Test
	public void testLodeWithFileOntology() throws URISyntaxException {
		URL ontologyUrl = Thread.currentThread().getContextClassLoader().getResource( "prov-o.rdf" );
		assertNotNull( ontologyUrl );

		URL xsltURL = Thread.currentThread().getContextClassLoader().getResource( "extraction.xsl" );
		assertNotNull( xsltURL );

		String xsltLocation = xsltURL.toURI().toString();
		String cssLocation = xsltLocation.substring( 0, xsltLocation.lastIndexOf( "extraction.xsl" ) );

		LodeTransformer transformer = new LodeTransformer( xsltLocation, cssLocation );
		try {
			String output = transformer.transform( ontologyUrl );

			Document html = Jsoup.parse( output );

			String title = html.title();
			System.out.println( title );
			assertTrue( title.contains( "PROV-O" ) );
		} catch ( Exception e ) {
			fail( e.getMessage() );
		}
	}

	@Test
	public void testLodeWithFileOntologyOWLAPI() throws URISyntaxException {
		URL ontologyUrl = Thread.currentThread().getContextClassLoader().getResource( "prov-o.owl" );
		assertNotNull( ontologyUrl );

		LodeTransformer transformer = new LodeTransformer( LodeTransformer.resolveDefaultXLS(), LodeTransformer.resolveDefaultCSS() );
		try {
			String output = transformer.transform( ontologyUrl, "en", true, false, false, false  );

			Document html = Jsoup.parse( output );

			String title = html.title();
			System.out.println( title );
			assertTrue( title.contains( "PROV-O" ) );
		} catch ( Exception e ) {
			fail( e.getMessage() );
		}
	}

}
