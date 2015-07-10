/**
 * 
 */
package com.williamoneil.backtesting.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.williamoneil.backtesting.dao.InstrumentPriceModel;

/**
 * @author Gudipati
 *
 */
public class HighTickInCalendarDaysHelper {

	  private int numDays  = 1; 
	  public HighTickInCalendarDaysHelper(int numDays) {
	      this.numDays = numDays;
	  }
	  
	  private InstrumentPriceModel firstPrice = null; 
	  private List<InstrumentPriceModel> prices = new ArrayList<InstrumentPriceModel>();
	  private InstrumentPriceModel highTick = null;
	  private InstrumentPriceModel previousHighTick = null;
	  public InstrumentPriceModel getHighTick() {
	      return highTick;
	  }
	  
	  public InstrumentPriceModel getPreviousHighTick() {
	    return previousHighTick;
	  }
	  
	  public boolean processTick(InstrumentPriceModel signal) {
	      if(signal == null) {
	          return false;
	      }
	      
	      if(firstPrice == null) {
	        firstPrice = signal;
	      }
	      
	      if(prices.size() == 0) {
	          prices.add(signal);
	          
	          return false;
	      }
	      
	      prices.add(signal);
	      
	      while(true) {
	          if(prices.size() == 1) {
	              break;
	          }
	          InstrumentPriceModel lastTick = prices.get(0);
	          if(numDaysBetween(signal.getPriceDate(), lastTick.getPriceDate()) > numDays) {
	              prices.remove(0);
	          } else {
	              break;
	          }
	      }
	      
	      if(highTick == null) {
	          highTick = signal;
	      } else if (numDaysBetween(signal.getPriceDate(), highTick.getPriceDate()) > numDays){
	          highTick = null;
	      }
	      
	      if(prices.size() > 1) {
	          if(highTick == null) {
	              // use brute force to find the high tick
	              highTick = prices.get(0);
	              for(int i=1;i<prices.size();i++) {
	                InstrumentPriceModel aSignal = prices.get(i);
	                  
	                  if(aSignal.getHigh().doubleValue() >= highTick.getHigh().doubleValue()) {
	                      highTick = aSignal;
	                  }
	              }
	          } 

	          if (isNewHigh(signal)) {
	            previousHighTick = highTick;
	            highTick = signal;
	            if(numDaysBetween(signal.getPriceDate(), firstPrice.getPriceDate()) >= numDays) {
	              return true;
	            }
	          }
	      }
	      
	      return false;
	  }
	  
	  private boolean isNewHigh(InstrumentPriceModel signal) {
	      return (highTick.getHigh().doubleValue() <= signal.getHigh().doubleValue());
	  }
	  
	  private static final long MILLIS_IN_DAY = 24 * 60 * 60 * 1000;
	  private static int numDaysBetween(Date first, Date second) {
	      return (int) Math.ceil(Math.abs((second.getTime() - first.getTime()) / MILLIS_IN_DAY));
	  }
}
