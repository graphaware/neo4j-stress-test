/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.graphaware.module.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.neo4j.graphdb.ExecutionPlanDescription;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ale
 */
public class TestTriadicPatterns
{
    private static final Logger LOG = LoggerFactory.getLogger(TestTriadicPatterns.class);

  public static void testPattern(String pattern, GraphDatabaseService database, String cypherquery, List<?> testset)
  {
    SummaryStatistics statistics = new SummaryStatistics();
    SummaryStatistics resultStatistics = new SummaryStatistics();

    boolean checkPlan = true;
    for (Object value : testset)
    {
      Map<String, Object> params = new HashMap<>();
      params.put( "param", value );
      Result testSingle = testSingle(database, cypherquery, params, statistics, resultStatistics);
      if (checkPlan && testSingle != null)
      {
        checkPlan(testSingle.getExecutionPlanDescription());
        checkPlan = false;
      }
    }
    LOG.warn("\nPartern: " + pattern + "\n"
            + "Total time: " + statistics.getSum() + "ms\n"
            + "Query Performed: " + statistics.getN() + "\n"
            + "Mean: " + statistics.getMean() + "ms\n"
            + "Min: " + statistics.getMin() + "ms\n"
            + "Max: " + statistics.getMax() + "ms\n"
            + "Variance: " + statistics.getVariance() + "ms\n"
            + "Results Min: " + resultStatistics.getMin() + "\n"
            + "Results Max: " + resultStatistics.getMax() + "\n"
            + "Results Mean: " + resultStatistics.getMean() + "\n");
  }
  public static Result testSingle(GraphDatabaseService database, String cypherquery, Map<String, Object> params, SummaryStatistics timeStatistics, SummaryStatistics resultStatistics)
  {
    try (Transaction tx = database.beginTx())
    {
      long start = System.currentTimeMillis();
      Result execute = database.execute(cypherquery, params);      
      long end = System.currentTimeMillis();
      if (execute.hasNext())
      {
        Long res = (Long) execute.next().get("count(c)");
        resultStatistics.addValue(res);
      }
      timeStatistics.addValue(end - start);
      tx.success();
      return execute;
    }
  }
  private static void checkPlan(ExecutionPlanDescription executionPlanDescription)
  {
      String execPlanName = executionPlanDescription.getName();
      //LOG.warn(">>>: " + execPlanName);
      switch (execPlanName)
      {
        case "TriadicSelection":
          LOG.warn("TriadicSelection on: " + (String) executionPlanDescription.getArguments().get("KeyNames"));
          break;
        case "NodeIndexSeek":
          LOG.warn("Index on: " + (String) executionPlanDescription.getArguments().get("Index"));
          break;
      }
      List<ExecutionPlanDescription> children = executionPlanDescription.getChildren();
      if (children != null)
      for (ExecutionPlanDescription exec : children)
        checkPlan(exec);
  }
  
}
