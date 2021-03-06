/*
 * $HeadURL: https://svn.apache.org/repos/asf/httpcomponents/httpcore/tags/4.0.1/httpcore/src/main/java/org/apache/http/impl/io/HttpResponseWriter.java $
 * $Revision: 744526 $
 * $Date: 2009-02-14 18:04:18 +0100 (Sat, 14 Feb 2009) $
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.impl.io;

import java.io.IOException;

import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.io.SessionOutputBuffer;
import org.apache.http.message.LineFormatter;
import org.apache.http.params.HttpParams;

/**
 * HTTP response writer that serializes its output to an instance 
 * of {@link SessionOutputBuffer}. 
 * 
 * @since 4.0
 */
public class HttpResponseWriter extends AbstractMessageWriter {

    public HttpResponseWriter(final SessionOutputBuffer buffer,
                              final LineFormatter formatter,
                              final HttpParams params) {
        super(buffer, formatter, params);
    }
    
    protected void writeHeadLine(final HttpMessage message)
        throws IOException {

        lineFormatter.formatStatusLine(this.lineBuf, 
                ((HttpResponse) message).getStatusLine());
        this.sessionBuffer.writeLine(this.lineBuf);
    }

}
