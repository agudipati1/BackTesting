/**
 * 
 */
package com.williamoneil.backtesting.markettimer;

import java.io.Serializable;
import java.math.BigDecimal;

import com.williamoneil.backtesting.dao.InstrumentPriceModel;

/**
 * @author Gudipati
 *
 */
public class TimingSignalData implements Serializable, ITimingSignalData {

	private static final long serialVersionUID = 1L;

	private MarketTimingModel marketTiming = null;
	
	private InstrumentPriceModel priceVolumeData = null;
	
	private BigDecimal priceMAShort = null;
	private BigDecimal priceMAMedium = null;
	private BigDecimal volMA = null;
	
	private boolean priceMarkedLow = false;
	private boolean priceMarkedHigh = false;
	
	private static boolean POWER_TREND_ON = false; 	
	
	public TimingSignalData(InstrumentPriceModel _priceModel, MarketTimingModel _timingModel) {
		this.priceVolumeData = _priceModel;
		this.marketTiming = _timingModel;
	}
	
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#getMarketTiming()
	 */
	public MarketTimingModel getMarketTiming() {
		return marketTiming;
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#getPriceMAMedium()
	 */
	@Override
	public BigDecimal getPriceMAMedium() {
		return priceMAMedium;
	}
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#setPriceMAMedium(java.math.BigDecimal)
	 */
	@Override
	public void setPriceMAMedium(BigDecimal priceMAMedium) {
		this.priceMAMedium = priceMAMedium;
	}
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#getPriceMAShort()
	 */
	@Override
	public BigDecimal getPriceMAShort() {
		return priceMAShort;
	}
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#setPriceMAShort(java.math.BigDecimal)
	 */
	@Override
	public void setPriceMAShort(BigDecimal priceMAShort) {
		this.priceMAShort = priceMAShort;
	}
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#getVolMA()
	 */
	@Override
	public BigDecimal getVolMA() {
		return volMA;
	}
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#setVolMA(java.math.BigDecimal)
	 */
	@Override
	public void setVolMA(BigDecimal volMA) {
		this.volMA = volMA;
	}
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#isPriceMarkedLow()
	 */
	@Override
	public boolean isPriceMarkedLow() {
		return priceMarkedLow;
	}
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#setPriceMarkedLow(boolean)
	 */
	@Override
	public void setPriceMarkedLow(boolean priceMarkedLow) {
		this.priceMarkedLow = priceMarkedLow;
	}
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#isPriceMarkedHigh()
	 */
	@Override
	public boolean isPriceMarkedHigh() {
		return priceMarkedHigh;
	}
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#setPriceMarkedHigh(boolean)
	 */
	@Override
	public void setPriceMarkedHigh(boolean priceMarkedHigh) {
		this.priceMarkedHigh = priceMarkedHigh;
	}
	
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.markettimer.ITimingSignalData#getPriceVolumeData()
	 */
	@Override
	public InstrumentPriceModel getPriceVolumeData() {
		return priceVolumeData;
	}

	public String toString() {
		return this.priceVolumeData.getPriceDate().toString();
	}
	
	public void addSignal(ITimingSignal signal) {
		if(signal == null) {
			return;
		} else if (signal.isBuySignal() == null) {
			if(signal instanceof RallySignalModel) {
				resetSignalCount();
			} else if (signal instanceof PowerTrendSignalModel){
				POWER_TREND_ON = ((PowerTrendSignalModel) signal).isOn();
				
				if(POWER_TREND_ON) {
					if(this.marketTiming.getSignalCount() == null || this.marketTiming.getSignalCount() == 0) {
						// set the floor to 1 when power-trend is on
						this.marketTiming.setSignalCount(1);
					}
				} else {
					if(this.marketTiming.getSignalCount() != null && this.marketTiming.getSignalCount() > 5) {
						// set the max count to 5 when power-trend is off
						this.marketTiming.setSignalCount(5);
					}
				}
			}
		} else if (signal.isBuySignal() == true) {
				incrementTimingCount();
		} else {
			decrementTimingCount();
		}
		
		this.marketTiming.getSignals().add(signal);
	}

	/**
	 * @param signalCount the signalCount to set
	 */
	public void resetSignalCount() {
		marketTiming.setSignalCount(0);
	}
	
	private void incrementTimingCount(){
		Integer signalCount = marketTiming.getSignalCount();
		if(signalCount == null) {
			signalCount = 0;
		}
		final int MAX_COUNT = POWER_TREND_ON ? 7 : 5;
		if(signalCount < MAX_COUNT) {
			signalCount++;
		}
		
		this.marketTiming.setSignalCount(signalCount);
	}
	
	private void decrementTimingCount(){
		Integer signalCount = marketTiming.getSignalCount();
		if(signalCount == null) {
			signalCount = 0;
		}
		final int MIN_COUNT = POWER_TREND_ON ? 1 : 0;
		if(signalCount > MIN_COUNT) {
			signalCount--;
		}
		this.marketTiming.setSignalCount(signalCount);
	}
}
