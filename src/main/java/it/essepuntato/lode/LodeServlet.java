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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Servlet implementation class LodeServlet
 */
public class LodeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String xsltURL = "http://lode.sourceforge.net/xslt";
	private String cssLocation = "http://lode.sourceforge.net/css/";
	private int maxTentative = 3;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LodeServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");

		resolvePaths(request); /* Used instead of the SourceForge repo */
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();

		SourceExtractor extractor = new SourceExtractor();
		extractor.addMimeTypes(MimeType.mimeTypes);

		for (int i = 0; i < maxTentative; i++) {
			try {
				String stringURL = request.getParameter("url");

				URL ontologyURL = new URL(stringURL);
				HttpURLConnection.setFollowRedirects(true);

				String content = "";

				boolean useOWLAPI = new Boolean(request.getParameter("owlapi"));
				boolean considerImportedOntologies = new Boolean(request.getParameter("imported"));
				boolean considerImportedClosure = new Boolean(request.getParameter("closure"));
				boolean useReasoner = new Boolean(request.getParameter("reasoner"));

				if (considerImportedOntologies || considerImportedClosure || useReasoner) {
					useOWLAPI = true;
				}

				String lang = request.getParameter("lang");
				if (lang == null) {
					lang = "en";
				}

				content = new LodeTransformer( xsltURL, cssLocation ).transform( ontologyURL,
				                                                                 lang,
				                                                                 useOWLAPI,
				                                                                 considerImportedOntologies,
				                                                                 considerImportedClosure,
				                                                                 useReasoner );

				out.println(content);
				i = maxTentative;
			} catch (Exception e) {
				e.printStackTrace();
				if (i + 1 == maxTentative) {
					out.println(getErrorPage(e));
				}
			}
		}
	}

	private void resolvePaths(HttpServletRequest request) {
		xsltURL = getServletContext().getRealPath("extraction.xsl");
		String requestURL = request.getRequestURL().toString();
		int index = requestURL.lastIndexOf("/");
		cssLocation = requestURL.substring(0, index) + File.separator;
	}



	private String getErrorPage(Exception e) {
		return "<html>" + "<head><title>LODE error</title></head>" + "<body>" + "<h2>" + "LODE error" + "</h2>" + "<p><strong>Reason: </strong>" + e.getMessage() + "</p>" + "</body>" + "</html>";
	}

}
