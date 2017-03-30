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

package org.apache.solr.recipe;

import java.util.Map;

import org.apache.solr.common.util.Utils;
import org.apache.solr.recipe.Policy.BaseSuggester;
import org.apache.solr.recipe.Policy.Session;

import static org.apache.solr.common.cloud.ZkStateReader.COLLECTION_PROP;
import static org.apache.solr.common.cloud.ZkStateReader.SHARD_ID_PROP;
import static org.apache.solr.common.params.CollectionParams.CollectionAction.ADDREPLICA;
import static org.apache.solr.common.params.CoreAdminParams.NODE;

class AddReplicaSuggester extends BaseSuggester {

  AddReplicaSuggester(String coll, String shard, Session session) {
    super(coll, shard, session);
  }

  Map get() {
    Map operation = tryEachNode(true);
    if (operation == null) operation = tryEachNode(false);
    return operation;
  }

  Map tryEachNode(boolean strict) {
    //iterate through elements and identify the least loaded
    for (int i = matrix.size() - 1; i >= 0; i--) {
      Row row = matrix.get(i);
      row = row.addReplica(coll, shard);
      row.violations.clear();
      for (Clause clause : session.getRuleSorter().clauses) {
        if (strict || clause.strict) clause.test(row);
      }
      if (row.violations.isEmpty()) {
        return Utils.makeMap("operation", ADDREPLICA,
            COLLECTION_PROP, coll,
            SHARD_ID_PROP, shard,
            NODE, row.node);
      }
    }
    return null;
  }

}