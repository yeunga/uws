/******************************************************************************
 *
 *  Copyright (C) 2009                          Copyright (C) 2009
 *  National Research Council           Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6                     Ottawa, Canada, K1A 0R6
 *  All rights reserved                         Tous droits reserves
 *
 *  NRC disclaims any warranties,       Le CNRC denie toute garantie
 *  expressed, implied, or statu-       enoncee, implicite ou legale,
 *  tory, of any kind with respect      de quelque nature que se soit,
 *  to the software, including          concernant le logiciel, y com-
 *  without limitation any war-         pris sans restriction toute
 *  ranty of merchantability or         garantie de valeur marchande
 *  fitness for a particular pur-       ou de pertinence pour un usage
 *  pose.  NRC shall not be liable      particulier.  Le CNRC ne
 *  in any event for any damages,       pourra en aucun cas etre tenu
 *  whether direct or indirect,         responsable de tout dommage,
 *  special or general, consequen-      direct ou indirect, particul-
 *  tial or incidental, arising         ier ou general, accessoire ou
 *  from the use of the software.       fortuit, resultant de l'utili-
 *                                                              sation du logiciel.
 *
 *
 *  This file is part of cadcUWS.
 *
 *  cadcUWS is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  cadcUWS is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with cadcUWS.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package ca.nrc.cadc.uws;

import java.net.URI;


/**
 * The error object gives a human readable error message (if any) for the
 * underlying job. This object is intended to be a detailed error message, and
 * consequently might be a large piece of text such as a stack trace. When
 * there is an error running a job a summary of the error should be given using
 * the optional errorSummary element of the JobSummary type.
 */
public class ErrorSummary
{
    private String summaryMessage;
    private URI documentURI;


    /**
     * Public no-arg constructor.
     */
    public ErrorSummary()
    {
    }

    /**
     * Complete constructor.
     *
     * @param summaryMessage        The summary of the error.
     * @param documentURI           The URI to the actual Document.
     */
    public ErrorSummary(final String summaryMessage, final URI documentURI)
    {
        this.summaryMessage = summaryMessage;
        this.documentURI = documentURI;
    }


    public String getSummaryMessage()
    {
        return summaryMessage;
    }

    public URI getDocumentURI()
    {
        return documentURI;
    }
}