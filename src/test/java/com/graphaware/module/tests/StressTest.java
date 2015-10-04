
package com.graphaware.module.tests;

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

public class StressTest
         //extends GraphAwareApiTest
{

  private static final Logger LOG = LoggerFactory.getLogger(StressTest.class);
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
    /*if[NEO4J_2_3]
      database = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder(new File("/tmp/graph_" + System.currentTimeMillis() + ".db"))
            .loadPropertiesFromFile(this.getClass().getClassLoader().getResource("neo4j.properties").getPath())
            .newGraphDatabase();
    end[NEO4J_2_3]*/
      
    /*if[NEO4J_2_2_5]
      database = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder("/tmp/graph_" + System.currentTimeMillis() + ".db")
            .loadPropertiesFromFile(this.getClass().getClassLoader().getResource("neo4j.properties").getPath())
            .newGraphDatabase();
    end[NEO4J_2_2_5]*/
      
    
    populateDatabase(database);

  }

  public String baseUrl()
  {
    return "http://localhost:7575";
  }

  protected DatabasePopulator databasePopulator()
  {
    return new GraphgenPopulator()
    {
      @Override
      protected String file() throws IOException
      {
        return new ClassPathResource("demo-data.cyp").getFile().getAbsolutePath();
      }
    };
  }

  protected void populateDatabase(GraphDatabaseService database)
  {
    LOG.warn("Starting populating the database ...");

    DatabasePopulator populator = databasePopulator();
    if (populator != null)
    {
      populator.populate(database);
    }
    LOG.warn("... database populated!");

  }

  @Test
  public void test() throws IOException
  {

    int maxPeople = 1000;
    List<String> people = new ArrayList<>();
    try (Transaction tx = database.beginTx())
    {
      Result result = database.execute("MATCH (n:Person) return n.name LIMIT " + maxPeople);
      while (result.hasNext())
      {
        Map<String, Object> row = result.next();
        people.add((String) row.get("n.name"));
      }
      tx.success();
    }
    try (Transaction tx = database.beginTx())
    {
      final String query = "MATCH (a:Person {name:\"" + people.get(0) + "\"})-[:KNOWS]->(b:Person)-[:KNOWS]->(c:Person) \n"
              + "WHERE NOT (a)-[:KNOWS]->(c) \n"
              + "RETURN c.name";
      LOG.warn("Query: " + query);
      Result result = database.execute("EXPLAIN " + query);
      tx.success();
    }
    SummaryStatistics statistics = new SummaryStatistics();
    for (String name : people)
    {
      try (Transaction tx = database.beginTx())
      {
        long start = System.currentTimeMillis();
        Result execute = database.execute("MATCH (a:Person {name:\"" + name + "\"})-[:KNOWS]->(b:Person)-[:KNOWS]->(c:Person) \n"
                + "WHERE NOT (a)-[:KNOWS]->(c) \n"
                + "RETURN c.name");
        long end = System.currentTimeMillis();
        statistics.addValue(end - start);
        tx.success();
      }
    }
    LOG.warn("Total time: " + statistics.getSum() + "ms\n"
            + "Occurrences: " + statistics.getN() + "\n"
            + "Mean: " + statistics.getMean() + "ms\n"
            + "Min: " + statistics.getMin() + "ms\n"
            + "Max: " + statistics.getMax() + "ms\n"
            + "Variance: " + statistics.getVariance() + "ms\n");
  }
}
