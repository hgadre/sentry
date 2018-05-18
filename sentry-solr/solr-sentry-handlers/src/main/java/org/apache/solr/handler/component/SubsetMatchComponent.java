/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.filter.component;

import java.io.IOException;
import java.util.Set;

import org.apache.sentry.binding.solr.authz.SentrySolrPluginImpl;
import org.apache.sentry.core.common.exception.SentryUserException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.security.AuthorizationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

/**
 * This custom {@linkplain SearchComponent} is responsible to introduce
 * a document level security filter based on user roles (AND clause).
 */
public class SubsetMatchComponent extends SearchComponent {
  private static Logger log = LoggerFactory.getLogger(SubsetMatchComponent.class);

  public static String AUTH_FIELD_PROP = "sentryAuthField";
  public static String COUNT_FIELD_PROP = "sentryAuthCountField";
  public static String DEFAULT_AUTH_FIELD = "sentry_auth";
  public static String DEFAULT_COUNT_FIELD = "sentry_auth_count";
  public static String ALL_ROLES_TOKEN_PROP = "allRolesToken";
  public static String ENABLED_PROP = "enabled";
  public static String QPARSER_NAME = "qParser";
  private String authField;
  private String authCountField;
  private String allRolesToken;
  private boolean enabled;
  private String qParserName;

  @SuppressWarnings("rawtypes")
  @Override
  public void init(NamedList args) {
    SolrParams params = SolrParams.toSolrParams(args);
    this.authField = params.get(AUTH_FIELD_PROP, DEFAULT_AUTH_FIELD);
    log.info("SubsetMatchComponent authField: " + this.authField);
    this.authCountField = params.get(COUNT_FIELD_PROP, DEFAULT_COUNT_FIELD);
    log.info("SubsetMatchComponent authCountField: " + this.authCountField);
    this.allRolesToken = params.get(ALL_ROLES_TOKEN_PROP);
    log.info("SubsetMatchComponent allRolesToken: " + this.allRolesToken);
    this.enabled = params.getBool(ENABLED_PROP, false);
    log.info("SubsetMatchComponent enabled: " + this.enabled);
    this.qParserName = params.get(QPARSER_NAME, "subset").trim();
    log.info("SubsetMatchComponent qParserName: " + this.qParserName);
  }

  public void prepare(ResponseBuilder rb) throws IOException {
    if (!enabled) {
       return;
    }

    String userName = SentrySolrPluginImpl.getShortUserName(rb.req.getUserPrincipal());
    String superUser = (System.getProperty("solr.authorization.superuser", "solr"));
    if (superUser.equals(userName)) {
      return;
    }

    Set<String> roles = getRoles(rb.req, userName);
    if (roles != null && roles.size() > 0) {
      log.debug("User {} is associated with roles {}", userName, roles);

      StringBuilder filterQuery = new StringBuilder();
      filterQuery.append(" {!").append(qParserName).append(" set_field=\"").append(authField).append("\"")
                 .append(" count_field=\'").append(authCountField).append("\'")
                 .append(" set_value=\"").append(Joiner.on(',').join(roles.iterator())).append("\"");
      if (allRolesToken != null) {
        filterQuery.append(" wildcard_token=\"").append(allRolesToken).append("\"");
      }
      filterQuery.append(" }");

      if (log.isDebugEnabled()) {
        log.debug("Adding filter clause : {}", filterQuery.toString());
      }

      ModifiableSolrParams newParams = new ModifiableSolrParams(rb.req.getParams());
      newParams.add("fq", filterQuery.toString());
      rb.req.setParams(newParams);

    } else {
      throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED,
        "Request from user: " + userName +
        " rejected because user is not associated with any roles");
    }
  }

  /**
   * This method returns the roles associated with the specified <code>userName</code>
   */
  private Set<String> getRoles (SolrQueryRequest req, String userName) {
    SolrCore solrCore = req.getCore();

    AuthorizationPlugin plugin = solrCore.getCoreContainer().getAuthorizationPlugin();
    if (!(plugin instanceof SentrySolrPluginImpl)) {
      throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED, getClass().getSimpleName() +
          " can only be used with Sentry authorization plugin for Solr");
    }

    try {
      return ((SentrySolrPluginImpl)plugin).getRoles(userName);
    } catch (SentryUserException e) {
      throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED,
        "Request from user: " + userName +
        " rejected due to SentryUserException: ", e);
    }
  }

  @Override
  public void process(ResponseBuilder arg0) throws IOException {
  }

  @Override
  public String getDescription() {
    return "Handle Query Document Authorization";
  }
}
