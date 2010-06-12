/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.cas.integration.restlet;

import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import com.github.inspektr.common.web.ClientInfoHolder;
import org.jasig.cas.server.CentralAuthenticationService;
import org.jasig.cas.server.authentication.Credential;
import org.jasig.cas.server.authentication.DefaultUserNamePasswordCredential;
import org.jasig.cas.server.authentication.UserNamePasswordCredential;
import org.jasig.cas.server.login.DefaultLoginRequestImpl;
import org.jasig.cas.server.login.LoginRequest;
import org.jasig.cas.server.login.LoginResponse;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.WebRequest;

/**
 * Handles the creation of Ticket Granting Tickets.
 * 
 * @author Scott Battaglia
 * @version $Revision$ $Date$
 * @since 3.3
 * 
 */
public class TicketResource extends Resource {
    
    private static final Logger log = LoggerFactory.getLogger(TicketResource.class);

    @Autowired
    private CentralAuthenticationService centralAuthenticationService;
    
    public final boolean allowGet() {
        return false;
    }

    public final boolean allowPost() {
        return true;
    }

    public final void acceptRepresentation(final Representation entity)
        throws ResourceException {
        if (log.isDebugEnabled()) {
            log.debug("Obtaining credentials...");
            log.debug(getRequest().getEntityAsForm().toString());
        }
     
        final Credential c = obtainCredentials();

        final LoginRequest loginRequest = new DefaultLoginRequestImpl(null, ClientInfoHolder.getClientInfo().getClientIpAddress(), false, false, null);
        loginRequest.getCredentials().add(c);
        final LoginResponse loginResponse = this.centralAuthenticationService.login(loginRequest);
        getResponse().setStatus(determineStatus());
        if (loginResponse.getSession() != null) {
            final Reference ticket_ref = getRequest().getResourceRef().addSegment(loginResponse.getSession().getId());
            getResponse().setLocationRef(ticket_ref);
            getResponse().setEntity("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\"><html><head><title>" + getResponse().getStatus().getCode() + " " + getResponse().getStatus().getDescription() + "</title></head><body><h1>TGT Created</h1><form action=\"" + ticket_ref + "\" method=\"POST\">Service:<input type=\"text\" name=\"service\" value=\"\"><br><input type=\"submit\" value=\"Submit\"></form></body></html>", MediaType.TEXT_HTML);
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, loginResponse.getGeneralSecurityExceptions().toString());                
        }
    }
    
    /**
     * Template method for determining which status to return on a successful ticket creation. 
     * This method exists for compatibility reasons with bad clients (i.e. Flash) that can't
     * process 201 with a Location header.
     * 
     * @return the status to return.
     */
    protected Status determineStatus() {
        return Status.SUCCESS_CREATED;
    }
    
    protected Credential obtainCredentials() {
        final UserNamePasswordCredential c = new DefaultUserNamePasswordCredential();
        final WebRequestDataBinder binder = new WebRequestDataBinder(c);
        final RestletWebRequest webRequest = new RestletWebRequest(getRequest());
        
        if (log.isDebugEnabled()) {
            log.debug(getRequest().getEntityAsForm().toString());
            log.debug("Username from RestletWebRequest: " + webRequest.getParameter("userName"));
        }
        
        binder.bind(webRequest);
        
        return c;
    }
    
    protected class RestletWebRequest implements WebRequest {
        
        private final Form form;
        
        private final Request request;
        
        public RestletWebRequest(final Request request) {
            this.form = getRequest().getEntityAsForm();
            this.request = request;
        }

        public boolean checkNotModified(long lastModifiedTimestamp) {
            return false;
        }

        public String getContextPath() {
            return this.request.getResourceRef().getPath();
        }

        public String getDescription(boolean includeClientInfo) {
            return null;
        }

        public Locale getLocale() {
            return LocaleContextHolder.getLocale();
        }

        public String getParameter(String paramName) {
            return this.form.getFirstValue(paramName);
        }

        public Map<String, String[]> getParameterMap() {
            final Map<String, String[]> conversion = new HashMap<String,String[]>();

            for (final Map.Entry<String, String> entry : this.form.getValuesMap().entrySet()) {
                conversion.put(entry.getKey(), new String[] {entry.getValue()});
            }

            return conversion;
        }

        public String[] getParameterValues(String paramName) {
            return this.form.getValuesArray(paramName);
        }

        public String getRemoteUser() {
            return null;
        }

        public Principal getUserPrincipal() {
            return null;
        }

        public boolean isSecure() {
            return this.request.isConfidential();
        }

        public boolean isUserInRole(String role) {
            return false;
        }

        public Object getAttribute(String name, int scope) {
            return null;
        }

        public String[] getAttributeNames(int scope) {
            return null;
        }

        public String getSessionId() {
            return null;
        }

        public Object getSessionMutex() {
            return null;
        }

        public void registerDestructionCallback(String name, Runnable callback,
            int scope) {
            // nothing to do
        }

        public void removeAttribute(String name, int scope) {
            // nothing to do
        }

        public void setAttribute(String name, Object value, int scope) {
            // nothing to do
        }

        public String getHeader(final String s) {
            return null;
        }

        public String[] getHeaderValues(String s) {
            return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Iterator<String> getHeaderNames() {
            return null;
        }

        public Iterator<String> getParameterNames() {
            return null;
        }

        public Object resolveReference(String s) {
            return null;
        }
    }
}
