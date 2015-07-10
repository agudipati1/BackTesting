package com.williamoneil.backtesting.markettimer;

import java.math.BigDecimal;

import com.williamoneil.backtesting.dao.InstrumentPriceModel;

public interface ITimingSignalData {

	/**
	 * @return the priceMAMedium
	 */
	public abstract BigDecimal getPriceMAMedium();

	/**
	 * @param priceMAMedium the priceMAMedium to set
	 */
	public abstract void setPriceMAMedium(BigDecimal priceMAMedium);

	/**
	 * @return the priceMAShort
	 */
	public abstract BigDecimal getPriceMAShort();

	/**
	 * @param priceMAShort the priceMAShort to set
	 */
	public abstract void setPriceMAShort(BigDecimal priceMAShort);

	/**
	 * @return the volMA
	 */
	public abstract BigDecimal getVolMA();

	/**
	 * @param volMA the volMA to set
	 */
	public abstract void setVolMA(BigDecimal volMA);

	/**
	 * @return the priceMarkedLow
	 */
	public abstract boolean isPriceMarkedLow();

	/**
	 * @param priceMarkedLow the priceMarkedLow to set
	 */
	public abstract void setPriceMarkedLow(boolean priceMarkedLow);

	/**
	 * @return the priceMarkedHigh
	 */
	public abstract boolean isPriceMarkedHigh();

	/**
	 * @param priceMarkedHigh the priceMarkedHigh to set
	 */
	public abstract void setPriceMarkedHigh(boolean priceMarkedHigh);

	/**
	 * @return the priceVolumeData
	 */
	public abstract InstrumentPriceModel getPriceVolumeData();

}