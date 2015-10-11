
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
import static com.graphaware.module.tests.TestTriadicPatterns.*;

public class StressGitHubTest
         //extends GraphAwareApiTest
{

  private static final Logger LOG = LoggerFactory.getLogger(StressGitHubTest.class);
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
    String databasePath = System.getProperty("databasePath");
    if (databasePath == null || databasePath.length() == 0)
      databasePath = "/tmp/graph_" + System.currentTimeMillis() + ".db";
    
    LOG.warn("Using database: " + databasePath);
    
    /*if[NEO4J_2_3]
      database = new TestGraphDatabaseFactory()
              .newEmbeddedDatabaseBuilder(new File(databasePath))
              .loadPropertiesFromFile(this.getClass().getClassLoader().getResource("neo4j-noga.properties").getPath())
              .newGraphDatabase();
    end[NEO4J_2_3]*/
      
    /*if[NEO4J_2_2_5]
      database = new TestGraphDatabaseFactory()
              .newEmbeddedDatabaseBuilder(databasePath)
              .loadPropertiesFromFile(this.getClass().getClassLoader().getResource("neo4j-noga.properties").getPath())
              .newGraphDatabase();
    end[NEO4J_2_2_5]*/

  }

  
  @Test
  public void test() throws IOException
  {

    int maxPeople = 100000;
    List<Integer> people = new ArrayList<>();
    try (Transaction tx = database.beginTx())
    {
      Result result = database.execute("MATCH (n:User) return n.id LIMIT " + maxPeople);
      while (result.hasNext())
      {
        Map<String, Object> row = result.next();
        people.add((Integer) row.get("n.id"));
      }
      tx.success();
    }
    
    // Negative Predicate Expression

    {    
      String pattern = "MATCH (a:X)-->(b)-->(c) WHERE NOT (a)-->(c)";
      String query = "MATCH (a:User {id:{param}})-[r1]->(b)-[r2]->(c) \n"
                + "WHERE NOT (a)-->(c) \n"
                + "RETURN count(c)";
      testPattern(pattern, database, query, people);
    }
    
    {    
      String pattern = "MATCH (a:X)-[:A]->(b)-[:B]->(c) WHERE NOT (a)-->(c) passes through";
      String query = "MATCH (a:User {id:{param}})-[:FOLLOWS]->(b)-[:WATCH]->(c) \n"
                + "WHERE NOT (a)-->(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    {    
      String pattern = "MATCH (a:X)-[:A]->(b)-[:A]->(c) WHERE NOT (a)<-[:A]-(c) passes through";
      String query = "MATCH (a:User {id:{param}})-[:FOLLOWS]->(b)-[:FOLLOWS]->(c) \n"
                + "WHERE NOT (a)<-[:FOLLOWS]-(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    {    
      String pattern = "MATCH (a:X)-[:A]->(b)-[:A]->(c) WHERE NOT (a:X)-[:A]->(c)";
      String query = "MATCH (a:User {id:{param}})-[:FOLLOWS]->(b)-[:FOLLOWS]->(c) \n"
                + "WHERE NOT (a)-[:FOLLOWS]->(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    {    
      String pattern = "MATCH (a:X)-[:A]->(b)-[:B]->(c) WHERE NOT (a:X)-[:A]->(c)";
      String query = "MATCH (a:User {id:{param}})-[:WATCH]->(b)-[:FORK_OF]->(c) \n"
                + "WHERE NOT (a)-[:WATCH]->(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    {    
      String pattern = "MATCH (a:X)-[:A]->(b)<-[:B]-(c) WHERE NOT (a:X)-[:A]->(c)";
      String query = "MATCH (a:User {id:{param}})-[:WATCH]->(b)<-[:FORK_OF]-(c) \n"
                + "WHERE NOT (a)-[:WATCH]->(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    // Positive Predicate Expression
    {    
      String pattern = "MATCH (a:X)-->(b)-[:A]->(c) WHERE (a:X)-[:A]->(c) passes through";
      String query = "MATCH (a:User {id:{param}})-[r1]->(b)-[:WATCH]->(c) \n"
                + "WHERE (a)-[:WATCH]->(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    {    
      String pattern = "MATCH (a:X)-->(b)-->(c) WHERE (a)-->(c)";
      String query = "MATCH (a:User {id:{param}})-->(b)-->(c) \n"
                + "WHERE (a)-->(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    {    
      String pattern = "MATCH (a:X)-[:A]->(b)-[:B]->(c) WHERE (a)-->(c) passes through";
      String query = "MATCH (a:User {id:{param}})-[:FOLLOWS]->(b)-[:WATCH]->(c) \n"
                + "WHERE (a)-->(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    {    
      String pattern = "MATCH (a:X)-[:A]->(b)-[:A]->(c) WHERE (a)<-[:A]-(c) passes through";
      String query = "MATCH (a:User {id:{param}})-[:FOLLOWS]->(b)-[:FOLLOWS]->(c) \n"
                + "WHERE (a)<-[:FOLLOWS]-(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    {    
      String pattern = "MATCH (a:X)-[:A]->(b)-[:A]->(c) WHERE (a:X)-[:A]->(c)";
      String query = "MATCH (a:User {id:{param}})-[:FOLLOWS]->(b)-[:FOLLOWS]->(c) \n"
                + "WHERE (a)-[:FOLLOWS]->(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    {    
      String pattern = "MATCH (a:X)-[:A]->(b)-[:B]->(c) WHERE (a:X)-[:A]->(c)";
      String query = "MATCH (a:User {id:{param}})-[:WATCH]->(b)-[:FORK_OF]->(c) \n"
                + "WHERE (a)-[:WATCH]->(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    {    
      String pattern = "MATCH (a:X)-[:A]->(b)<-[:B]-(c) WHERE (a:X)-[:A]->(c)";
      String query = "MATCH (a:User {id:{param}})-[:WATCH]->(b)<-[:FORK_OF]-(c) \n"
                + "WHERE (a)-[:WATCH]->(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
    // Negative Predicate Expression and matching labels

    {    
      String pattern = "MATCH (a:X)-->(b:Y)-->(c:Y) WHERE NOT (a)-->(c)";
      String query = "MATCH (a:User {id:{param}})-->(b:Repository)-->(c:Repository) \n"
                + "WHERE NOT (a)-->(c) \n"
                + "RETURN count(c)";

      testPattern(pattern, database, query, people);
    }
    
//    {    
//      String pattern = "MATCH (a:X)-->(b:Y)-->(c:Z) WHERE NOT (a)-->(c) passes through";
//      String query = "MATCH (a:PushEvent {id:{param}})-->(b:User)-->(c:Branch) \n"
//                + "WHERE NOT (a)-->(c) \n"
//                + "RETURN count(c)";
//
//      testPattern(pattern, database, query, people);
//    }
//    
//    {    
//      String pattern = "MATCH (a:X)-->(b:Y)-->(c) WHERE NOT (a)-->(c) passes through";
//      String query = "MATCH (a:Person {id:{param}})-->(b:Person)-->(c:Skill) \n"
//                + "WHERE (a)-->(c) \n"
//                + "RETURN count(c)";
//
//      testPattern(pattern, database, query, people);
//    }
  }
}
