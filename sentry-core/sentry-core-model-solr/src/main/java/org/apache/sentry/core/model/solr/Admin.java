/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sentry.core.model.solr;

import java.util.Arrays;
import java.util.Collection;

/**
 * This class represents Solr admin operations which includes,
 * - Collection admin operations
 * - Core admin operations
 * - Security configuration management.
 */
public class Admin implements SolrModelAuthorizable {
  public static Collection<String> ENTITY_NAMES =
      Arrays.asList(SolrConstants.ALL, "collections", "cores", "security", "metrics", "autoscaling");

  public static Admin ALL = new Admin(SolrConstants.ALL);
  public static Admin COLLECTIONS = new Admin("collections");
  public static Admin CORES = new Admin("cores");
  public static Admin SECURITY = new Admin("security");
  public static Admin METRICS = new Admin("metrics");
  public static Admin AUTOSCALING = new Admin("autoscaling");

  private final String name;

  public Admin (String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getTypeName() {
    return getAuthzType().name();
  }

  @Override
  public AuthorizableType getAuthzType() {
    return AuthorizableType.Admin;
  }

  @Override
  public String toString() {
    return name;
  }
}
