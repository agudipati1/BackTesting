package com.williamoneil.backtesting.dao;

import java.math.BigDecimal;
import java.util.Date;


public class InstrumentPriceModel {
	private Date priceDate = null;
	private BigDecimal high = null;
	private BigDecimal low = null;
	private BigDecimal open = null;
	private BigDecimal close = null;
	private Long volume = null;
	private TradeDateType dateType = null;
	/**
	 * @return the priceDate
	 */
	public Date getPriceDate() {
		return priceDate;
	}
	/**
	 * @param priceDate the priceDate to set
	 */
	public void setPriceDate(Date priceDate) {
		this.priceDate = priceDate;
	}
	/**
	 * @return the high
	 */
	public BigDecimal getHigh() {
		return high;
	}
	/**
	 * @param high the high to set
	 */
	public void setHigh(BigDecimal high) {
		this.high = high;
	}
	/**
	 * @return the low
	 */
	public BigDecimal getLow() {
		return low;
	}
	/**
	 * @param low the low to set
	 */
	public void setLow(BigDecimal low) {
		this.low = low;
	}
	/**
	 * @return the open
	 */
	public BigDecimal getOpen() {
		return open;
	}
	/**
	 * @param open the open to set
	 */
	public void setOpen(BigDecimal open) {
		this.open = open;
	}
	/**
	 * @return the close
	 */
	public BigDecimal getClose() {
		return close;
	}
	/**
	 * @param close the close to set
	 */
	public void setClose(BigDecimal close) {
		this.close = close;
	}
	/**
	 * @return the volume
	 */
	public Long getVolume() {
		return volume;
	}
	/**
	 * @param volume the volume to set
	 */
	public void setVolume(Long volume) {
		this.volume = volume;
	}
	/**
	 * @return the dateType
	 */
	public TradeDateType getDateType() {
		return dateType;
	}
	/**
	 * @param dateType the dateType to set
	 */
	public void setDateType(TradeDateType dateType) {
		this.dateType = dateType;
	}

}
