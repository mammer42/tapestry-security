/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.tynamo.security.shiro.authz;

import java.io.IOException;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.lang.util.StringUtils;
import org.apache.shiro.web.util.WebUtils;
import org.tynamo.security.internal.services.LoginContextService;

/**
 * A copy of Shiro's 1.2.0 PortFilter that works with tapestry-security 0.4.x
 *
 * A Filter that requires the request to be on a specific port, and if not, redirects to the same URL on that port.
 *
 * @since 0.4.1
 */
public class PortFilter extends AuthorizationFilter {
	public PortFilter(LoginContextService loginContextService) {
		super(loginContextService);
	}

	public static final int DEFAULT_HTTP_PORT = 80;
	public static final String HTTP_SCHEME = "http";

	private int port = DEFAULT_HTTP_PORT;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	protected int toPort(Object mappedValue) {
		String[] ports = (String[]) mappedValue;
		if (ports == null || ports.length == 0) {
			return getPort();
		}
		if (ports.length > 1) {
			throw new ConfigurationException("PortFilter can only be configured with a single port.  You have " +
					"configured " + ports.length + ": " + StringUtils.toString(ports));
		}
		return Integer.parseInt(ports[0]);
	}

	protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
		int requiredPort = toPort(mappedValue);
		int requestPort = request.getServerPort();
		return requiredPort == requestPort;
	}

	protected String getScheme(String requestScheme, int port) {
		if (port == DEFAULT_HTTP_PORT) {
			return HTTP_SCHEME;
		} else if (port == SslFilter.DEFAULT_HTTPS_PORT) {
			return SslFilter.HTTPS_SCHEME;
		} else {
			return requestScheme;
		}
	}

	/**
	 * Redirects the request to the same exact incoming URL, but with the port listed in the filter's configuration.
	 *
	 * @param request     the incoming <code>ServletRequest</code>
	 * @param response    the outgoing <code>ServletResponse</code>
	 * @param mappedValue the config specified for the filter in the matching request's filter chain.
	 * @return {@code false} always to force a redirect.
	 */
	@Override
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response, Object mappedValue) throws IOException {

		//just redirect to the specified port:
		int port = toPort(mappedValue);

		String scheme = getScheme(request.getScheme(), port);

		StringBuilder sb = new StringBuilder();
		sb.append(scheme).append("://");
		sb.append(request.getServerName());
		if (port != DEFAULT_HTTP_PORT && port != SslFilter.DEFAULT_HTTPS_PORT) {
			sb.append(":");
			sb.append(port);
		}
		if (request instanceof HttpServletRequest) {
			sb.append(WebUtils.toHttp(request).getRequestURI());
			String query = WebUtils.toHttp(request).getQueryString();
			if (query != null) {
				sb.append("?").append(query);
			}
		}

		WebUtils.issueRedirect(request, response, sb.toString());

		return false;
	}
}
