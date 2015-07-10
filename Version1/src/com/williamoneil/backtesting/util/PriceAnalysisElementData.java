/**
 * 
 */
package com.williamoneil.backtesting.util;

/**
 * @author Gudipati
 *
 */
public class PriceAnalysisElementData {
  
  private ChartAnalysisType analysisType = null;
  private ChartElementType elementType = null;
  /**
   * @return the analysisType
   */
  public ChartAnalysisType getAnalysisType() {
    return analysisType;
  }
  /**
   * @param analysisType the analysisType to set
   */
  public void setAnalysisType(ChartAnalysisType analysisType) {
    this.analysisType = analysisType;
  }
  /**
   * @return the elementType
   */
  public ChartElementType getElementType() {
    return elementType;
  }
  /**
   * @param elementType the elementType to set
   */
  public void setElementType(ChartElementType elementType) {
    this.elementType = elementType;
  }
}