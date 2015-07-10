/**
 * 
 */
package com.williamoneil.backtesting.data;

import java.math.BigDecimal;

/**
 * @author Gudipati
 *
 */
public class CurrencyData {

	public static CurrencyData instantiate(long initValue) {
		return new CurrencyData(new BigDecimal(initValue));
	}
	public static CurrencyData instantiate(BigDecimal initValue) {
		return new CurrencyData(initValue);
	}
	
	private String currencyCode = null;
	private BigDecimal value = null;
	
	public CurrencyData() {
		super();
	}
	public CurrencyData(BigDecimal value) {
		super();
		this.value = value;
	}
	public CurrencyData(String currencyCode, BigDecimal value) {
		super();
		this.currencyCode = currencyCode;
		this.value = value;
	}
	/**
	 * @return the currencyCode
	 */
	public String getCurrencyCode() {
		return currencyCode;
	}
	/**
	 * @param currencyCode the currencyCode to set
	 */
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = currencyCode;
	}
	/**
	 * @return the value
	 */
	public BigDecimal getValue() {
		return value;
	}
	/**
	 * @param value the value to set
	 */
	public void setValue(BigDecimal value) {
		this.value = value;
	}
	
}
