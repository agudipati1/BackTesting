/**
 * 
 */
package com.williamoneil.backtesting.markettimer;

import java.util.List;

import com.williamoneil.backtesting.dao.InstrumentPriceModel;
import com.williamoneil.backtesting.dao.TradeDateType;

/**
 * @author Gudipati
 *
 */
public class PriceMarkedLabelHelper {

	private static final int LOOKAHEAD = 9;
	
	public PriceMarkedLabelHelper(){
	}
	
	public void processTick(final List<InstrumentPriceModel> pvList, final ITimingSignalData aTick, final int index) {
		final InstrumentPriceModel aData = pvList.get(index);
		if (aData.getDateType() == TradeDateType.TRADING_DATE) {
			boolean isHigh = true;
			boolean isLow = true;
			int i = 0;
			
			int forwardLookingCount = 0;
			int backwardLookingCount = 0;
			while(forwardLookingCount <= LOOKAHEAD || backwardLookingCount <= LOOKAHEAD) {
			    i++;
				//lookahead first
				int lookNextIndex = index + i;
				if(lookNextIndex >= pvList.size() || lookNextIndex < 0) {
					isHigh = false;
					isLow = false;
					break;
				}
				
				InstrumentPriceModel nextTick = pvList.get(lookNextIndex);
				if(nextTick.getDateType() == TradeDateType.TRADING_DATE) {
				  forwardLookingCount++;
				  if(forwardLookingCount <= LOOKAHEAD && nextTick.getHigh().compareTo(aTick.getPriceVolumeData().getHigh()) >= 0) {
				    isHigh = false;
				  }
				  if(forwardLookingCount <= LOOKAHEAD && nextTick.getLow().compareTo(aTick.getPriceVolumeData().getLow()) <= 0) {
					isLow = false;
				  }
				  
	              if(!isHigh && !isLow) {
                    break;
                  }
				}

				lookNextIndex = index - i;
				if(lookNextIndex < 0) {
					isHigh = false;
					isLow = false;
					break;
				}
				
				// lookback now
				nextTick = pvList.get(lookNextIndex);
				if(nextTick.getDateType() == TradeDateType.TRADING_DATE) {
				  backwardLookingCount++;
				  if(backwardLookingCount <= LOOKAHEAD && nextTick.getLow().compareTo(aTick.getPriceVolumeData().getLow()) < 0) {
					isLow = false;
				  }
				  if(backwardLookingCount <= LOOKAHEAD && nextTick.getHigh().compareTo(aTick.getPriceVolumeData().getHigh()) > 0) {
					isHigh = false;
				  }

				  if(!isHigh && !isLow) {
				      break;
				  }
				}
			}
			if(isLow) {
				aTick.setPriceMarkedLow(isLow);
			}
			if(isHigh) {
				aTick.setPriceMarkedHigh(isHigh);
			}
		}
	}


}
