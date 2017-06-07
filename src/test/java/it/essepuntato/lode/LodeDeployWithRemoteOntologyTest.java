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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.InputStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LodeDeployWithRemoteOntologyTest {

	private Server server;

	@Before
	public void startServer() throws Exception {
		server = new Server(8080);
		server.setStopAtShutdown(true);
		WebAppContext webAppContext = new WebAppContext();
		webAppContext.setContextPath("/lode");
		webAppContext.setResourceBase("src/main/webapp");
		webAppContext.setClassLoader(getClass().getClassLoader());
		server.addHandler(webAppContext);
		server.start();
	}


	@Test
	public void testLodeWithRemoteOntology() throws Exception {
		CloseableHttpClient client = HttpClientBuilder.create().build();
		try {
			HttpGet mockRequest = new HttpGet( "http://localhost:8080/lode/extract?url=https://www.w3.org/ns/prov-o" );
			HttpResponse mockResponse = client.execute( mockRequest );

			assertEquals( 200, mockResponse.getStatusLine().getStatusCode() );
			InputStream is = mockResponse.getEntity().getContent();

			Document html = Jsoup.parse( is, Charset.defaultCharset().name(), "http://localhost:8080/lode" );

			String title = html.title();
			System.out.println( title );
			assertTrue( title.contains( "PROV-O" ) );
		} catch ( Exception e ) {
			fail( e.getMessage() );
		} finally {
			client.close();
		}
	}

	@After
	public void shutdownServer() throws Exception {
		server.stop();
	}

}
