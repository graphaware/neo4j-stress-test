/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.graphaware.module.batchinsert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author ale
 */
public class GACSVFileSource implements IGABatchInserterSource
{
  private final BufferedReader br;
  private final String cvsSplitBy;
  private final String key;
  public GACSVFileSource(String fileSource, String cvsSplitBy, String key) throws FileNotFoundException
  {
    br = new BufferedReader(new FileReader(fileSource));
    this.cvsSplitBy = cvsSplitBy;
    this.key = key;
  }

  @Override
  public GABatchInsertRow getNext()
  {
    try
    {
      String line = "";
      if ((line = br.readLine()) != null)
      {
        String[] currentNodesId = line.split(cvsSplitBy);
        //System.out.println("Lines: " + currentNodesId[0] + " " + currentNodesId[1]);
        GABatchInsertRow row = new GABatchInsertRow();
        HashMap<String, Object> sourceProperties = new HashMap<>();
        sourceProperties.put(key, currentNodesId[0]);
        row.setSourceNodeProperties(sourceProperties);
        HashMap<String, Object> destinationProperties = new HashMap<>();
        destinationProperties.put(key, currentNodesId[1]);
        row.setDestinationNodeProperties(destinationProperties);
        return row;
      }
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void close()
  {
    if (br != null)
    {
      try
      {
        br.close();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
  }

}
