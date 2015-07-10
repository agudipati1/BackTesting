/**
 * 
 */
package com.williamoneil.backtesting.util;

import java.math.BigDecimal;

import com.williamoneil.backtesting.dao.InstrumentPriceModel;
import com.williamoneil.backtesting.dao.TradeDateType;

/**
 * Average True Range helper
 * @author Gudipati
 *
 */
public class ATRHelper {

  private int length = 1;
  public ATRHelper(int length) {
    this.length = length;
  }
  
  private int count = 0;
  private double rollingATR = 0;
  private BigDecimal lastATR = null;
  
  private InstrumentPriceModel previousTick = null;
  
  /*
   * Use this for IPOs because they may not have full length of prices
   */
  public double getRollingATR(){
	  return rollingATR / count;
  }
  
  public BigDecimal processTick(final InstrumentPriceModel aTick) {
    if (aTick.getDateType() != TradeDateType.TRADING_DATE) {
      return null;
    }
    
    BigDecimal atr = null;
    
    final double tr = getTrueRange(aTick, previousTick);
    
    count++;
    if(count > length) {
      atr = new BigDecimal((lastATR.doubleValue() * (length - 1) + tr) / length);
    } else if(count <= length) {
      // we just continue add to the rolling-ATR
      rollingATR += tr;
      if (count == length) {
        atr = new BigDecimal(rollingATR / length);
      }
    }
    
    previousTick = aTick;
    lastATR = atr;
    return atr;
  }
  
  private static double getTrueRange(final InstrumentPriceModel currTick, final InstrumentPriceModel prevTick) {
    final double currHighCurrClose = Math.abs(currTick.getHigh().doubleValue() - currTick.getLow().doubleValue());
    
    if(prevTick == null) {
      return currHighCurrClose;
    } else {
      final double currHighPrevClose = Math.abs(currTick.getHigh().doubleValue() - prevTick.getClose().doubleValue());
      final double currLowPrevClose = Math.abs(currTick.getLow().doubleValue() - prevTick.getClose().doubleValue());
      
      if(currHighPrevClose > currLowPrevClose) {
        if(currHighPrevClose > currHighCurrClose) {
          return currHighPrevClose;
        } else {
          return currHighCurrClose;
        }
      } else if(currLowPrevClose > currHighCurrClose) {
        return currLowPrevClose;
      } else {
        return currHighCurrClose;
      }
      
    }
  }
}
