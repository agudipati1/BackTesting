/**
 * 
 */
package com.williamoneil.backtesting.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.TimeZone;

import com.williamoneil.backtesting.dao.InstrumentPriceModel;
import com.williamoneil.backtesting.dao.TradeDateType;

/**
 * @author Gudipati
 *
 */
public abstract class Helpers {
  final public static TimeZone timeZone_NY = TimeZone.getTimeZone("America/New_York");
  final public static TimeZone timeZone_LOCAL = TimeZone.getDefault();
  
  final public static int MIN_OF_DAY_MARKET_OPEN_NY = (9*60) + 30; // 9:30 am
  final public static int MIN_OF_DAY_MARKET_CLOSE_NY = (16*60); // 4:00 am
  
  final public static int FEED_DELAY_MINS = 30;
  
  final public static MathContext mc  = new MathContext(3, RoundingMode.HALF_UP);
  
  final public static BigDecimal HUNDRED = new BigDecimal(100);
  
  final public static int PRICE_HISTORY_LOOKUP_DAYSBACK = 400;
  
  public static BigDecimal getPercetChange(BigDecimal ofValue, BigDecimal toValue) {
	  if(ofValue == null || toValue == null) {
		  return null;
	  }
	  
	  return ofValue.subtract(toValue).divide(toValue, mc).multiply(HUNDRED);
  }
  
  public static Long getProjectedVolume(InstrumentPriceModel latestTick) {
    if(latestTick == null || latestTick.getDateType() == null || latestTick.getVolume() == null || latestTick.getPriceDate() == null 
        || TradeDateType.TRADING_DATE != latestTick.getDateType() || latestTick.getVolume() <= 0) {
      return null;
    }
    
    final long vol = latestTick.getVolume().longValue();
    
    final Calendar tickCal = Calendar.getInstance();
    tickCal.setTime(latestTick.getPriceDate());
    
    /*
    Date currentTime = new Date();
    final int milliSecOffSet_LOCAL = timeZone_LOCAL.getOffset(currentTime.getTime());
    final int milliSecOffSet_NY = timeZone_NY.getOffset(currentTime.getTime());
    */
    
    final Calendar today_NY = Calendar.getInstance(timeZone_NY);
    //cal.setTime(currentTime);
    
    // if today is weekend, then no need to adjust volume
    int todaysDayOfWeek = today_NY.get(Calendar.DAY_OF_WEEK);
    if(todaysDayOfWeek == Calendar.SATURDAY || todaysDayOfWeek == Calendar.SUNDAY) {
      return vol;
    }
    
    // if today does not match the tick-date, then no need to adjust
    if(today_NY.get(Calendar.DAY_OF_YEAR) != tickCal.get(Calendar.DAY_OF_YEAR) || today_NY.get(Calendar.YEAR) != tickCal.get(Calendar.YEAR)) {
      return vol;
    }
    
    final int totalTradingMins = MIN_OF_DAY_MARKET_CLOSE_NY - MIN_OF_DAY_MARKET_OPEN_NY;
    // we adjust time only if it is during market hours

    // get the minute of the day 
    final int todaysMinuteOfDay_NY = today_NY.get(Calendar.HOUR_OF_DAY) * 60 + today_NY.get(Calendar.MINUTE);
    final int todaysTradingMins = todaysMinuteOfDay_NY - MIN_OF_DAY_MARKET_OPEN_NY - FEED_DELAY_MINS;
    
    if(todaysTradingMins > 0 && todaysMinuteOfDay_NY < MIN_OF_DAY_MARKET_CLOSE_NY + FEED_DELAY_MINS ) {
      return (vol / todaysTradingMins) * totalTradingMins; 
    } else {
      return vol;
    }
  }
  
  public static boolean isDistributionDay(InstrumentPriceModel currentTick, InstrumentPriceModel lastTick) {
    return currentTick.getClose().doubleValue() < lastTick.getClose().doubleValue() &&
        currentTick.getVolume() >= lastTick.getVolume() &&
        currentTick.getClose().doubleValue() <= 0.998 * lastTick.getClose().doubleValue();
  }
}