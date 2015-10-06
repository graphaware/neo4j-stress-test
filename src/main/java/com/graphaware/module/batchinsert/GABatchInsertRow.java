/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.graphaware.module.batchinsert;

import java.util.Map;

/**
 *
 * @author ale
 */
public class GABatchInsertRow
{
  private Map<String, Object> sourceNodeProperties;
  private Map<String, Object> destinationNodeProperties;
  
  public Map<String, Object> getSourceNodeProperties()
  {
    return sourceNodeProperties;
  }
  public Map<String, Object> getDestinationNodeProperties()
  {
    return destinationNodeProperties;
  }
  public void setSourceNodeProperties(Map<String, Object> sourceNodeProperties)
  {
    this.sourceNodeProperties = sourceNodeProperties;
  }
  public void setDestinationNodeProperties(Map<String, Object> destinationNodeProperties)
  {
    this.destinationNodeProperties = destinationNodeProperties;
  }  
}
