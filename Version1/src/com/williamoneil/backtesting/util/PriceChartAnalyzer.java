/**
 * 
 */
package com.williamoneil.backtesting.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.williamoneil.ApplicationException;
import com.williamoneil.backtesting.dao.InstrumentPriceModel;
import com.williamoneil.backtesting.dao.TradeDateType;

/**
 * @author Gudipati
 *
 */
public class PriceChartAnalyzer {

  public static List<PriceAnalysisData> analyzeDailyPrices(final List<InstrumentPriceModel> pvList) throws ApplicationException {
    if(pvList == null || pvList.size() <= 2) {
      return null;
    }
      
    if(pvList.get(0).getPriceDate().after(pvList.get(1).getPriceDate())) {
      Collections.reverse(pvList);
    }
    
      final List<PriceAnalysisData> analyzedPrices = new ArrayList<PriceAnalysisData>();
      
      final PriceMovingAverageHelper pMAHelper10dEma = new PriceMovingAverageHelper(10, false); //10 EMA
      final PriceMovingAverageHelper pMAHelper20dEma = new PriceMovingAverageHelper(20, false); //20 EMA
      final PriceMovingAverageHelper pMAHelper50dSma = new PriceMovingAverageHelper(50, true); //50 SMA
      final PriceMovingAverageHelper pMAHelper200dSma = new PriceMovingAverageHelper(200, true); //200 SMA
      
      final VolumeMovingAverageHelper vMAHelper = new VolumeMovingAverageHelper(50, true); //50 SMA
      
      final MAAnalysisHelper ma10AnalysisHelper = new MAAnalysisHelper(ChartElementType.PRICE_MA_10);
      final MAAnalysisHelper ma20AnalysisHelper = new MAAnalysisHelper(ChartElementType.PRICE_MA_20);
      final MAAnalysisHelper ma50AnalysisHelper = new MAAnalysisHelper(ChartElementType.PRICE_MA_50);
      final MAAnalysisHelper ma200AnalysisHelper = new MAAnalysisHelper(ChartElementType.PRICE_MA_200);
      final GapAnalysisHelper gapAnalysisHelper = new GapAnalysisHelper();
      
      final ATRHelper atr14Helper = new ATRHelper(14);
      
      final HighAnalysisHelper highAnalysisHelper = new HighAnalysisHelper();
      
      BigDecimal lastPriceMA10 = null;
      BigDecimal lastPriceMA20 = null;
      BigDecimal lastPriceMA50 = null;
      BigDecimal lastPriceMA200 = null;
      BigDecimal lastVolMA = null;
      
      for(final InstrumentPriceModel aPrice : pvList) {
        if(aPrice == null || aPrice.getDateType() == null || TradeDateType.TRADING_DATE != aPrice.getDateType() || aPrice.getVolume() == null || aPrice.getClose() == null || aPrice.getVolume() <= 0) {
          continue;
        }

        final PriceAnalysisData analyzedPrice = new PriceAnalysisData();
        analyzedPrice.setPrice(aPrice);
        
        // first calculate moving averages because we would need it in checking the signals
        final BigDecimal priceMA10 = pMAHelper10dEma.processTick(aPrice, lastPriceMA10);
        
        final BigDecimal priceMA20 = pMAHelper20dEma.processTick(aPrice, lastPriceMA20);
        final BigDecimal priceMA50 = pMAHelper50dSma.processTick(aPrice, lastPriceMA50);
        final BigDecimal priceMA200 = pMAHelper200dSma.processTick(aPrice, lastPriceMA200);
        final BigDecimal volMA = vMAHelper.processTick(aPrice, lastVolMA);
        
        final BigDecimal atr14 = atr14Helper.processTick(aPrice);
        
        // set the MA and other analyzed data now..  
        analyzedPrice.setPrice10dEma(priceMA10);
        analyzedPrice.setPrice20dEma(priceMA20);
        analyzedPrice.setPrice50dSma(priceMA50);
        analyzedPrice.setPrice200dSma(priceMA200);
        analyzedPrice.setVol50dSma(volMA);
        analyzedPrice.setAtr14d(atr14);
        
        lastPriceMA10 = priceMA10;
        lastPriceMA20 = priceMA20;
        lastPriceMA50 = priceMA50;
        lastPriceMA200 = priceMA200;
        lastVolMA = volMA;
        
        // analyze for signals now..
        ma200AnalysisHelper.populateChartAnalysis(analyzedPrice);
        ma50AnalysisHelper.populateChartAnalysis(analyzedPrice);
        ma20AnalysisHelper.populateChartAnalysis(analyzedPrice);
        ma10AnalysisHelper.populateChartAnalysis(analyzedPrice);
        gapAnalysisHelper.populateChartAnalysis(analyzedPrice);
        
        // analyze new-highs
        highAnalysisHelper.populateChartAnalysis(analyzedPrice);
        
        analyzedPrices.add(analyzedPrice);
        
        if(analyzedPrice.getPriceAnalysisElements().size() > 0) {
        	StringBuffer buffy = new StringBuffer(analyzedPrice.getPrice().getPriceDate() + " : ");
        	for(PriceAnalysisElementData pae : analyzedPrice.getPriceAnalysisElements()) {
        		buffy.append(pae.getElementType() + " " + pae.getAnalysisType() + ",");
        	}
        	//System.err.println(buffy.toString());
        }
      }
      
      return analyzedPrices;
  }
  
  public static double getClosingRange(InstrumentPriceModel currentPrice, InstrumentPriceModel lastPrice) {
    final BigDecimal close = currentPrice.getClose();
    final BigDecimal low = currentPrice.getLow();
    
    BigDecimal high = null;
    if(lastPrice != null && lastPrice.getLow().doubleValue() > currentPrice.getHigh().doubleValue()) {
        // this is a gap down
        high = lastPrice.getClose();
    } else {
        high = currentPrice.getHigh();   
    }
    
    return ((close.doubleValue() - low.doubleValue()) * 100) / (high.doubleValue() - low.doubleValue());
  }
}

class HighAnalysisHelper {
    final HighLowTickInTradingDaysHelper trading100DaysHighHelper = new HighLowTickInTradingDaysHelper(100);
    final HighTickInCalendarDaysHelper calendar365DaysHighHelper = new HighTickInCalendarDaysHelper(365); // 52 weeks high
    
    public void populateChartAnalysis(final PriceAnalysisData currentPrice) {
      if(currentPrice == null || currentPrice.getPrice() == null || currentPrice.getPrice().getDateType() != TradeDateType.TRADING_DATE || currentPrice.getPrice().getVolume() == null || currentPrice.getPrice().getVolume() == 0)  {
        return;
      }
      
      final boolean new52wkHigh = calendar365DaysHighHelper.processTick(currentPrice.getPrice());
      trading100DaysHighHelper.processTick(currentPrice.getPrice());
      
      
      ChartAnalysisType analysisType = null;
      if(new52wkHigh) {
        final InstrumentPriceModel previousHigh = calendar365DaysHighHelper.getPreviousHighTick();
        if(previousHigh != null && currentPrice.getPrice().getClose().doubleValue() >= previousHigh.getHigh().doubleValue()) {
          analysisType = ChartAnalysisType.NEW_HIGHS;
          final PriceAnalysisElementData analysis = new PriceAnalysisElementData();
          analysis.setElementType(ChartElementType.PRICE);
          analysis.setAnalysisType(analysisType);
          currentPrice.getPriceAnalysisElements().add(analysis);
        }
      }
      
      // add recent-high logic ONLY if 52-wk high is not added
      if(analysisType == null) {
        if(trading100DaysHighHelper.getHighTick() != null && currentPrice.getPrice() == trading100DaysHighHelper.getHighTick()) {
          final InstrumentPriceModel previousHigh = calendar365DaysHighHelper.getPreviousHighTick();
          if(previousHigh != null && currentPrice.getPrice().getClose().doubleValue() >= previousHigh.getHigh().doubleValue()) {
            analysisType = ChartAnalysisType.NEW_RECENT_HIGHS ;
            final PriceAnalysisElementData analysis = new PriceAnalysisElementData();
            analysis.setElementType(ChartElementType.PRICE);
            analysis.setAnalysisType(analysisType);
       
            currentPrice.getPriceAnalysisElements().add(analysis);
          }
        }
      }
    }
}

class GapAnalysisHelper {
  
  private ATRHelper atrHelper = new ATRHelper(14);
  
  private PriceAnalysisData prevPrice = null;
  
  static final Calendar cal  = Calendar.getInstance();
  static {
	  // for debugging only
	    cal.set(Calendar.YEAR, 2014);
	    cal.set(Calendar.MONTH, Calendar.MARCH);
	    cal.set(Calendar.DAY_OF_MONTH, 10);
  }
  
  public void populateChartAnalysis(final PriceAnalysisData currentPrice) {
    if(currentPrice == null || currentPrice.getPrice() == null || currentPrice.getPrice().getDateType() != TradeDateType.TRADING_DATE || currentPrice.getPrice().getVolume() == null || currentPrice.getPrice().getVolume() == 0)  {
      return;
    }
 
    if(currentPrice.getPrice().getPriceDate().after(cal.getTime())) {
    	System.err.print("");
    }
    
    final BigDecimal atr = atrHelper.processTick(currentPrice.getPrice());
    
    if(prevPrice == null) {
      prevPrice = currentPrice;
      return;
    }
 
    try {
      if(currentPrice.getPrice().getLow().doubleValue() <= prevPrice.getPrice().getHigh().doubleValue()) {
        return;
      }
      
      double threshold = 0.01;
      if(atr != null) {
    	  threshold = atr.doubleValue();
      } else {
    	 threshold = atrHelper.getRollingATR();
      }
      
      PriceAnalysisElementData pae = null;
      if(currentPrice.getPrice().getLow().doubleValue() >= prevPrice.getPrice().getHigh().doubleValue() + threshold) {
        pae = new PriceAnalysisElementData();
        pae.setAnalysisType(ChartAnalysisType.GAP_UP);
        pae.setElementType(ChartElementType.PRICE);
      } else if (currentPrice.getPrice().getHigh().doubleValue() <= prevPrice.getPrice().getLow().doubleValue() - threshold) {
        pae = new PriceAnalysisElementData();
        pae.setAnalysisType(ChartAnalysisType.GAP_DOWN);
        pae.setElementType(ChartElementType.PRICE);
      }
      
      if(pae != null) {
        currentPrice.getPriceAnalysisElements().add(pae);
      }
    } finally {
      prevPrice = currentPrice;
    }
  }
}

class MAAnalysisHelper {
	
    final static double LOW_THRESHOLD = 0.5;
    final static double HIGH_THRESHOLD = 0.5;
    final static double CLOSE_THRESHOLD = 0.5;
    
    private PriceAnalysisData previousPrice = null;
    
    private ChartElementType chartElementType = null;
    
    public MAAnalysisHelper(ChartElementType elmType) {
      if(elmType == ChartElementType.PRICE_MA_10 || elmType == ChartElementType.PRICE_MA_20 || elmType == ChartElementType.PRICE_MA_50 || elmType == ChartElementType.PRICE_MA_200) {
        chartElementType = elmType;
      } else {
        throw new RuntimeException("Only 10, 20, 50, 200 ma's are supported");
      }
    }
    
    private BigDecimal getMA(final PriceAnalysisData price) {
      BigDecimal ma = null;
      switch (chartElementType) { 
        case PRICE_MA_10: {
          ma = price.getPrice10dEma();
          break;
        }
        case PRICE_MA_20: {
          ma = price.getPrice20dEma();
          break;
        }
        case PRICE_MA_50: {
          ma = price.getPrice50dSma();
          break;
        }
        case PRICE_MA_200: {
          ma = price.getPrice200dSma();
          break;
        }
        default:{
          throw new RuntimeException("Only 10, 20, 50, 200 ma's are supported");
        }
      }
      
      return ma;
    }

    private ChartElementType geChartElementType() {
      return chartElementType;
    }


    private boolean isPriceAtSupport(final InstrumentPriceModel tick, BigDecimal pricePoint) {
    
      // calc % from ma
      final double lowPctFromPoint = 100 * (tick.getLow().doubleValue()-pricePoint.doubleValue()) / pricePoint.doubleValue(); 
      final double closePctFromPoint = 100 * (tick.getClose().doubleValue()-pricePoint.doubleValue()) / pricePoint.doubleValue();
      
      if(lowPctFromPoint <= LOW_THRESHOLD && lowPctFromPoint >= -LOW_THRESHOLD) {
        return true;
      } else if (closePctFromPoint < CLOSE_THRESHOLD && closePctFromPoint > -CLOSE_THRESHOLD) {
        return true;
      }
       
      return false;
    }

    private boolean isPriceAboveSupportThreshold(final InstrumentPriceModel tick, BigDecimal pricePoint) {
      // calc % from ma
      final double closePctFromPoint = 100 * (tick.getClose().doubleValue() - pricePoint.doubleValue()) / pricePoint.doubleValue();
      
      if (closePctFromPoint >= CLOSE_THRESHOLD) {
        return true;
      }
       
      return false;
    }

    private boolean isPriceBelowResistanceThreshold(final InstrumentPriceModel tick, BigDecimal pricePoint) {
      // calc % from ma
      final double closePctFromPoint = 100 * (tick.getClose().doubleValue() - pricePoint.doubleValue()) / pricePoint.doubleValue();
      
      if (closePctFromPoint <= -CLOSE_THRESHOLD) {
        return true;
      }
       
      return false;
    }

    private boolean isPriceAtResistance(final InstrumentPriceModel tick, BigDecimal pricePoint) {
      
      //  calc % from ma
      final double highPctFromPoint = 100 * (tick.getHigh().doubleValue()-pricePoint.doubleValue()) / pricePoint.doubleValue(); 
      final double closePctFromPoint = 100 * (tick.getClose().doubleValue()-pricePoint.doubleValue()) / pricePoint.doubleValue();
      
      if(highPctFromPoint <= HIGH_THRESHOLD && highPctFromPoint >= -HIGH_THRESHOLD) {
        return true;
      } else if (closePctFromPoint < CLOSE_THRESHOLD && closePctFromPoint > -CLOSE_THRESHOLD) {
        return true;
      }
       
      return false;
    }

    
    private ChartAnalysisType previousTickAnalysisType = null;
    public void populateChartAnalysis(final PriceAnalysisData currentPrice) {
      if(currentPrice == null || currentPrice.getPrice() == null || currentPrice.getPrice().getDateType() != TradeDateType.TRADING_DATE || currentPrice.getPrice().getVolume() == null || currentPrice.getPrice().getVolume() <= 0)  {
        return;
      }
      
      if(previousPrice == null) {
        previousPrice = currentPrice;
        return;
      }
      
      final BigDecimal currentMA = getMA(currentPrice);
      final BigDecimal previousMA = getMA(previousPrice);
      if(currentMA == null || previousMA == null) {
        previousPrice = currentPrice;
        return;
      }
      
      final double chgPct = 100 * (currentPrice.getPrice().getClose().doubleValue() - previousPrice.getPrice().getClose().doubleValue()) / previousPrice.getPrice().getClose().doubleValue(); 
      
      ChartAnalysisType currentTickAnalysisType = null;
      
      // first check if the tick is breaking-up or down .. we only check this condition if the previous tick is at support
      if(previousTickAnalysisType == ChartAnalysisType.SUPPORT || previousTickAnalysisType == ChartAnalysisType.RESISTANCE) {
        if(isPriceBelowResistanceThreshold(currentPrice.getPrice(), currentMA)) {
          if(chgPct >= 0) {
            currentTickAnalysisType = ChartAnalysisType.RESISTANCE;
          } else {
            currentTickAnalysisType = ChartAnalysisType.BREAKING_DOWN;
          }
        } else if(isPriceAboveSupportThreshold(currentPrice.getPrice(), currentMA)){
          if(chgPct <= 0) {
            currentTickAnalysisType = ChartAnalysisType.SUPPORT;
          } else {
            currentTickAnalysisType = ChartAnalysisType.BREAKING_UP;
          }
        }
      } else if (isPriceAtSupport(currentPrice.getPrice(), currentMA)) {
        if(isPriceBelowResistanceThreshold(currentPrice.getPrice(), currentMA)) {
          if(chgPct >= 0) {
            currentTickAnalysisType = ChartAnalysisType.RESISTANCE;
          } else {
            currentTickAnalysisType = ChartAnalysisType.BREAKING_DOWN;
          }
        } else if(isPriceAboveSupportThreshold(currentPrice.getPrice(), currentMA)){
          if(chgPct <= 0) {
            currentTickAnalysisType = ChartAnalysisType.SUPPORT;
          } else {
            currentTickAnalysisType = ChartAnalysisType.BREAKING_UP;
          }
        }
      }
      
      if(currentTickAnalysisType == null) {
        if(previousMA.doubleValue() >= currentMA.doubleValue()) {
          // trending-down.. we only check for resistance and breaking-down
          
          // we only check resistance if it is not breaking-down
          if(currentTickAnalysisType == null) {
            boolean flag = isPriceAtResistance(currentPrice.getPrice(), currentMA);
            if(flag) {
              currentTickAnalysisType = ChartAnalysisType.RESISTANCE;
            }
          }
        } else {
          // trending-up.. we only check for support
          boolean flag = isPriceAtSupport(currentPrice.getPrice(), currentMA);
          if(flag) {
            currentTickAnalysisType = ChartAnalysisType.SUPPORT;
          }          
         }
      }
      
      if(currentTickAnalysisType == null) {
        if(previousPrice.getPrice().getClose().doubleValue() > previousMA.doubleValue()
            && currentPrice.getPrice().getClose().doubleValue() < currentMA.doubleValue()) {
          currentTickAnalysisType = ChartAnalysisType.BREAKING_DOWN;
        } else if (previousPrice.getPrice().getClose().doubleValue() < previousMA.doubleValue()
            && currentPrice.getPrice().getClose().doubleValue() > currentMA.doubleValue()) {
          currentTickAnalysisType = ChartAnalysisType.BREAKING_UP;
        }
      }
      
      if(currentTickAnalysisType != null) {
        final PriceAnalysisElementData analysis = new PriceAnalysisElementData();
        analysis.setElementType(geChartElementType());
        analysis.setAnalysisType(currentTickAnalysisType);
       
        currentPrice.getPriceAnalysisElements().add(analysis);
      }
      
      previousTickAnalysisType = currentTickAnalysisType;
      previousPrice = currentPrice;
    }
}

