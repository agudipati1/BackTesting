/**
 * 
 */
package com.williamoneil.backtesting.util;

import java.util.ArrayList;
import java.util.List;

import com.williamoneil.backtesting.dao.InstrumentPriceModel;

/**
 * @author Gudipati
 *
 */
public class HighLowTickInTradingDaysHelper {

	  private int numDays  = 1; 
	  public HighLowTickInTradingDaysHelper(int numDays) {
	      this.numDays = numDays;
	  }
	  
	  private List<InstrumentPriceModel> pricess = new ArrayList<InstrumentPriceModel>();
	  public List<InstrumentPriceModel> getPrices() {
	      if(pricess.size() >= numDays) {
	          return pricess;
	      } else {
	          return null;
	      }
	      
	  }

	  private InstrumentPriceModel highTick = null;
	  private InstrumentPriceModel previousHighTick = null;
	  public InstrumentPriceModel getHighTick() {
	      if(pricess.size() >= numDays) {
	          return highTick;
	      } else {
	          return null;
	      }
	  }

	  public InstrumentPriceModel getPreviousHighTick() {
	      if(pricess.size() >= numDays) {
	        return previousHighTick;
	      } else {
	        return null;
	      }
	  }
	  
	  private InstrumentPriceModel lowTick = null;
	  private InstrumentPriceModel previousLowTick = null;
	  
	  public InstrumentPriceModel getLowTick() {
	      if(pricess.size() >= numDays) {
	          return lowTick;
	      } else {
	          return null;
	      }
	  }

	  public InstrumentPriceModel getPreviousLowTick() {
	    if(pricess.size() >= numDays) {
	        return previousLowTick;
	    } else {
	        return null;
	    }
	  }
	  public void processTick(InstrumentPriceModel signal) {
	      if(signal == null) {
	          return;
	      }
	      
	      pricess.add(signal);
	      if(pricess.size() == 1) {
	          return;
	      }
	      
	      InstrumentPriceModel removedSignal = null;
	      if(pricess.size() > numDays) {
	          removedSignal = pricess.remove(0);
	      }
	      
	      if(highTick == null) {
	          highTick = signal;
	      } else if (removedSignal != null && highTick.getPriceDate().equals(removedSignal.getPriceDate())){
	          highTick = null;
	      }
	      
	      if(lowTick == null) {
	          lowTick = signal;
	      } else if (removedSignal != null && lowTick.getPriceDate().equals(removedSignal.getPriceDate())){
	          lowTick = null;
	      }
	      
	      if(pricess.size() > 1) {
	          if(highTick == null) {
	              // use brute force to find the high tick
	              highTick = pricess.get(0);
	              for(int i=1;i<pricess.size();i++) {
	                InstrumentPriceModel aSignal = pricess.get(i);
	                  
	                  if(aSignal.getHigh().doubleValue() >= highTick.getHigh().doubleValue()) {
	                      highTick = aSignal;
	                  }
	              }
	          } 
	          
	          if (highTick.getHigh().doubleValue() <= signal.getHigh().doubleValue()) {
	              previousHighTick = highTick;
	              highTick = signal;
	          }
	          
	          if(lowTick == null) {
	              // use brute force to find the low tick
	              lowTick = pricess.get(0);
	              for(int i=1;i<pricess.size();i++) {
	                InstrumentPriceModel aSignal = pricess.get(i);
	                  
	                  if(aSignal.getLow().doubleValue() <= lowTick.getLow().doubleValue()) {
	                      lowTick = aSignal;
	                  }
	              }
	          } 
	          
	          if (lowTick.getLow().doubleValue() >= signal.getLow().doubleValue()) {
	              previousLowTick = lowTick;
	              lowTick = signal;
	          }
	      }
	  }
}
