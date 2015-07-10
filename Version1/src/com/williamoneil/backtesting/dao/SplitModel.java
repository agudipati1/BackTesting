/**
 * 
 */
package com.williamoneil.backtesting.dao;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Gudipati
 *
 */
public class SplitModel {
	private Date splitDate = null;
	private BigDecimal splitFactor = null;
	/**
	 * @return the splitDate
	 */
	public Date getSplitDate() {
		return splitDate;
	}
	/**
	 * @param splitDate the splitDate to set
	 */
	public void setSplitDate(Date splitDate) {
		this.splitDate = splitDate;
	}
	/**
	 * @return the splitFactor
	 */
	public BigDecimal getSplitFactor() {
		return splitFactor;
	}
	/**
	 * @param splitFactor the splitFactor to set
	 */
	public void setSplitFactor(BigDecimal splitFactor) {
		this.splitFactor = splitFactor;
	}
}
