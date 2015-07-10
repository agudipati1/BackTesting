/**
 * 
 */
package com.williamoneil.backtesting.markettimer;

import java.math.BigDecimal;

import com.williamoneil.backtesting.dao.InstrumentPriceModel;

/**
 * @author Gudipati
 *
 */
public class SimpleTimingSignalData implements ITimingSignalData {

	private SimpleMarketTimingModel simpleMarketTiming = null;
	
	private InstrumentPriceModel priceVolumeData = null;
	
	private BigDecimal priceMAShort = null;
	private BigDecimal priceMAMedium = null;
	private BigDecimal volMA = null;
	
	private boolean priceMarkedLow = false;
	private boolean priceMarkedHigh = false;
	
	public SimpleTimingSignalData(InstrumentPriceModel _priceModel, SimpleMarketTimingModel _timingModel) {
		this.priceVolumeData = _priceModel;
		this.simpleMarketTiming = _timingModel;
	}
	
	/**
	 * @return the simpleMarketTiming
	 */
	public SimpleMarketTimingModel getSimpleMarketTiming() {
		return simpleMarketTiming;
	}

	/**
	 * @return the priceMAMedium
	 */
	public BigDecimal getPriceMAMedium() {
		return priceMAMedium;
	}
	/**
	 * @param priceMAMedium the priceMAMedium to set
	 */
	public void setPriceMAMedium(BigDecimal priceMAMedium) {
		this.priceMAMedium = priceMAMedium;
	}
	/**
	 * @return the priceMAShort
	 */
	public BigDecimal getPriceMAShort() {
		return priceMAShort;
	}
	/**
	 * @param priceMAShort the priceMAShort to set
	 */
	public void setPriceMAShort(BigDecimal priceMAShort) {
		this.priceMAShort = priceMAShort;
	}
	/**
	 * @return the volMA
	 */
	public BigDecimal getVolMA() {
		return volMA;
	}
	/**
	 * @param volMA the volMA to set
	 */
	public void setVolMA(BigDecimal volMA) {
		this.volMA = volMA;
	}
	
	/**
	 * @return the priceVolumeData
	 */
	public InstrumentPriceModel getPriceVolumeData() {
		return priceVolumeData;
	}

	public String toString() {
		return this.priceVolumeData.getPriceDate().toString();
	}

	/**
	 * @return the priceMarkedLow
	 */
	public boolean isPriceMarkedLow() {
		return priceMarkedLow;
	}

	/**
	 * @param priceMarkedLow the priceMarkedLow to set
	 */
	public void setPriceMarkedLow(boolean priceMarkedLow) {
		this.priceMarkedLow = priceMarkedLow;
	}

	/**
	 * @return the priceMarkedHigh
	 */
	public boolean isPriceMarkedHigh() {
		return priceMarkedHigh;
	}

	/**
	 * @param priceMarkedHigh the priceMarkedHigh to set
	 */
	public void setPriceMarkedHigh(boolean priceMarkedHigh) {
		this.priceMarkedHigh = priceMarkedHigh;
	}
}
