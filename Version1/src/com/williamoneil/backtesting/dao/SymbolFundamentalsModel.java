/**
 * 
 */
package com.williamoneil.backtesting.dao;

import java.util.Date;
import java.util.List;

/**
 * @author Gudipati
 *
 */
public class SymbolFundamentalsModel {

	private Date asOfDate = null;

	private SymbolFundamentalsInfoModel info = null;

	private List<FundsHoldingsModel> fundsHoldings = null;

	private List<ReportedFundamentalsModel> fundamentals = null;

	/**
	 * @return the asOfDate
	 */
	public Date getAsOfDate() {
		return asOfDate;
	}

	/**
	 * @param asOfDate
	 *            the asOfDate to set
	 */
	public void setAsOfDate(Date asOfDate) {
		this.asOfDate = asOfDate;
	}

	/**
	 * @return the fundHoldings
	 */
	public List<FundsHoldingsModel> getFundsHoldings() {
		return fundsHoldings;
	}

	/**
	 * @param fundHoldings
	 *            the fundHoldings to set
	 */
	public void setFundsHoldings(List<FundsHoldingsModel> fundHoldings) {
		this.fundsHoldings = fundHoldings;
	}

	/**
	 * @return the fundamentals
	 */
	public List<ReportedFundamentalsModel> getFundamentals() {
		return fundamentals;
	}

	/**
	 * @param fundamentals
	 *            the fundamentals to set
	 */
	public void setFundamentals(List<ReportedFundamentalsModel> fundamentals) {
		this.fundamentals = fundamentals;
	}

	/**
	 * @return the info
	 */
	public SymbolFundamentalsInfoModel getInfo() {
		return info;
	}

	/**
	 * @param info
	 *            the info to set
	 */
	public void setInfo(SymbolFundamentalsInfoModel info) {
		this.info = info;
	}
}