
package com.graphaware.module.tests;

import com.graphaware.module.batchinsert.GABatchInserter;
import com.graphaware.module.batchinsert.GACSVFileSource;
import org.junit.Test;
import org.neo4j.graphdb.*;

import java.io.IOException;

import com.graphaware.test.data.DatabasePopulator;
import com.graphaware.test.data.GraphgenPopulator;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Before;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class Stress4Test
         //extends GraphAwareApiTest
{

  private static final Logger LOG = LoggerFactory.getLogger(Stress4Test.class);
  private GraphDatabaseService database;

  //@Override
  protected String neo4jConfigFile()
  {
    return "neo4j.properties";
  }

  protected String propertiesFile()
  {
    return "src/test/resources/" + neo4jConfigFile();
  }

  protected String neo4jServerConfigFile()
  {
    return "neo4j-server.properties";
  }

  @Before
  public void setUp()
  {
  }

  @Test
  public void test() throws IOException
  {

    String path = System.getProperty("databaseCSVPath");


    GACSVFileSource source = new GACSVFileSource(path, "\t", "id");
    final String databasePath = "/tmp/graph_" + System.currentTimeMillis() + ".db";
    GABatchInserter inserter = new GABatchInserter(source, databasePath);
    long startTime = System.currentTimeMillis();
    inserter.load("User", "id");
    long endTime = System.currentTimeMillis();
    LOG.warn("Time to import: " + (endTime - startTime) + "ms");

    /*if[NEO4J_2_3]
      database = new TestGraphDatabaseFactory()
              .newEmbeddedDatabaseBuilder(new File(databasePath))
              .loadPropertiesFromFile(this.getClass().getClassLoader().getResource("neo4j.properties").getPath())
              .newGraphDatabase();
    end[NEO4J_2_3]*/
      
    /*if[NEO4J_2_2_5]
      database = new TestGraphDatabaseFactory()
              .newEmbeddedDatabaseBuilder(databasePath)
              .loadPropertiesFromFile(this.getClass().getClassLoader().getResource("neo4j.properties").getPath())
              .newGraphDatabase();
    end[NEO4J_2_2_5]*/
    
    LOG.warn("Creating indices");
    database.execute("CREATE INDEX ON :User(id)");
    
    int maxPeople = 100000;
    List<String> people = new ArrayList<>();
    LOG.warn("Creating list");
    try (Transaction tx = database.beginTx())
    {
      Result result = database.execute("MATCH (n:User) return n.id LIMIT " + maxPeople);
      while (result.hasNext())
      {
        Map<String, Object> row = result.next();
        people.add((String) row.get("n.id"));
      }
      tx.success();
    }

    SummaryStatistics timeStatistics = new SummaryStatistics();
    SummaryStatistics resultStatistics = new SummaryStatistics();
    for (String name : people)
    {
      try (Transaction tx = database.beginTx())
      {
        long start = System.currentTimeMillis();
        Result execute = database.execute("MATCH (a:User {id:\"" + name + "\"})-[:KNOWS]->(b:User)-[:KNOWS]->(c:User) \n"
                + "WHERE NOT (a)-[:KNOWS]->(c) \n"
                + "RETURN count(c)");
        if (execute.hasNext())
        {
          Long res = (Long) execute.next().get("count(c)");
          resultStatistics.addValue(res);
        }
        tx.success();
        long end = System.currentTimeMillis();
        timeStatistics.addValue(end - start);
      }
      if (resultStatistics.getN() % 10000 == 0)
        LOG.warn("Processed: " + resultStatistics.getN());
    }
    LOG.warn("\nTotal time: " + timeStatistics.getSum() + "ms\n"
            + "Queries: " + timeStatistics.getN() + "\n"
            + "Mean: " + timeStatistics.getMean() + "ms\n"
            + "Min: " + timeStatistics.getMin() + "ms\n"
            + "Max: " + timeStatistics.getMax() + "ms\n"
            + "Variance: " + timeStatistics.getVariance() + "ms\n"
            + "Results Min: " + resultStatistics.getMin() + "\n"
            + "Results Max: " + resultStatistics.getMax() + "\n"
            + "Results Mean: " + resultStatistics.getMean() + "\n");

  }
}