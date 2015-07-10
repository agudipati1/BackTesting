/**
 * 
 */
package com.williamoneil.backtesting.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.williamoneil.backtesting.dao.InstrumentPriceModel;
import com.williamoneil.backtesting.dao.TradeDateType;

/**
 * @author Gudipati
 *
 */
public class VolumeMovingAverageHelper {
	private int length = 0;
	private boolean simpleMA = true;

	public VolumeMovingAverageHelper(int length, boolean simpleMA) {
		this.length = length;
		this.simpleMA = simpleMA;
	}
	
	private BigDecimal rollingSum = BigDecimal.ZERO;
	private List<InstrumentPriceModel> rollingTicks = new ArrayList<InstrumentPriceModel>();
	public BigDecimal processTick(final InstrumentPriceModel aTick, final BigDecimal lastMA) {		
		BigDecimal avg = null;
		if (aTick.getDateType() == TradeDateType.TRADING_DATE && aTick.getVolume() != null) {
			if(simpleMA || rollingTicks.size() <= length) {
				// for EMA we process it as SMA until we reach the MA-length
				
				// its a trade-date
				rollingTicks.add(aTick);
				
				rollingSum = rollingSum.add(new BigDecimal(aTick.getVolume()));
	
				if(rollingTicks.size() > length) {
					final InstrumentPriceModel lastTick = rollingTicks.remove(0);
					rollingSum = rollingSum.subtract(new BigDecimal(lastTick.getVolume()));
					
					avg = rollingSum.divide(new BigDecimal(length), 5, BigDecimal.ROUND_HALF_UP);
				} else if (rollingTicks.size() == length) {
					avg = rollingSum.divide(new BigDecimal(length), 5, BigDecimal.ROUND_HALF_UP);
					// add an extra tick if this is for EMA (this would make the logic not get into this loop)
					if(!simpleMA) {
						rollingTicks.add(aTick);
					}
				} else {
					avg = null;
				}
			} else if (!simpleMA){
				// this is EMA specific logic
				if(lastMA != null) {
					final BigDecimal multiplier = new BigDecimal(2).divide(new BigDecimal(length + 1), 5, BigDecimal.ROUND_HALF_UP);
					avg = (new BigDecimal(aTick.getVolume()).subtract(lastMA).multiply(multiplier)).add(lastMA);
				}
			}
		} else {
			// for non-trading-dates we return previous tick's MA
			avg = lastMA;
		}
		
		return avg;
	}
}
