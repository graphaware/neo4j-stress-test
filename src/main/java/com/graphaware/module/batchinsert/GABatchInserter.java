/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.graphaware.module.batchinsert;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neo4j.cypher.internal.compiler.v1_9.symbols.RelationshipType;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserters;

/**
 *
 * @author ale
 */
public class GABatchInserter
{
  private final IGABatchInserterSource source;
  private final String dbPath;
  private static final int BATCH_INSERT = 100000;
  private Map<String, Long> nodesCache;
  private static Logger logger = LoggerFactory.getLogger(GABatchInserter.class);

  public GABatchInserter(IGABatchInserterSource source, String dbPath)
  {
    this.source = source;
    this.dbPath = dbPath;
    nodesCache = new HashMap<>();
  }

  public void load(String label, String key) throws IOException
  {
    BatchInserter inserter = null;
    BatchInserterIndexProvider indexProvider = null;
    try
    {
      Map<String, String> config = new HashMap<>();
      config.put("dbms.pagecache.memory", "512m");
      inserter = BatchInserters.inserter(new File(dbPath), config);

      indexProvider = new LuceneBatchInserterIndexProvider(inserter);
      BatchInserterIndex users
              = indexProvider.nodeIndex(label, MapUtil.stringMap("type", "exact"));
      users.setCacheCapacity(key, BATCH_INSERT);

      Label userLabel = DynamicLabel.label(label);
      DynamicRelationshipType knows = DynamicRelationshipType.withName("KNOWS");

      int k = 1;
      GABatchInsertRow row;
      while ((row = source.getNext()) != null)
      {
        long nodeSource = getOrCreateNode(inserter, users, row.getSourceNodeProperties(), key, userLabel);
        long nodeDestination = getOrCreateNode(inserter, users, row.getDestinationNodeProperties(), key, userLabel);
        inserter.createRelationship(nodeSource, nodeDestination, knows, null);

        if (k++ % BATCH_INSERT == 0)
        {
          users.flush();
          nodesCache.clear();
          //k = 0;
          logger.warn("Flushing at " + k);
        }
      }
      logger.warn("Flushing at the end -> " + k);
      users.flush();
      nodesCache.clear();
    }
    finally
    {
      if (source != null)
        source.close();
      if (indexProvider != null)
      {
        indexProvider.shutdown();
      }

      if (inserter != null)
      {
        inserter.shutdown();
      }

    }
  }
  private long getOrCreateNode(BatchInserter inserter, BatchInserterIndex users, Map<String, Object> sourceNodeProperties, String key, Label ... label)
  {
    String keyValue = (String) sourceNodeProperties.get(key);
    Long node = nodesCache.get(keyValue);
    if (node != null)
      return node;
    IndexHits<Long> nodeHits = users.get(key, keyValue);
    if (nodeHits != null && nodeHits.hasNext())
      return nodeHits.getSingle();
    
    node = inserter.createNode( sourceNodeProperties, label);
    users.add( node, sourceNodeProperties );
    nodesCache.put(keyValue, node);
    return node;
  }
}
