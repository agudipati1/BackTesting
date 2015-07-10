/**
 * 
 */
package com.williamoneil.backtesting.dao;

/**
 * @author Gudipati
 *
 */
public class FundsHoldingsModel {
	int calendarYear = 0;
	int calendarQtr = 0;

	Integer numOfFunds = null;
	Long numOfShares = null;

	/**
	 * @return the calendarYear
	 */
	public int getCalendarYear() {
		return calendarYear;
	}

	/**
	 * @param calendarYear
	 *            the calendarYear to set
	 */
	public void setCalendarYear(int calendarYear) {
		this.calendarYear = calendarYear;
	}

	/**
	 * @return the calendarQtr
	 */
	public int getCalendarQtr() {
		return calendarQtr;
	}

	/**
	 * @param calendarQtr
	 *            the calendarQtr to set
	 */
	public void setCalendarQtr(int calendarQtr) {
		this.calendarQtr = calendarQtr;
	}

	/**
	 * @return the numOfFunds
	 */
	public Integer getNumOfFunds() {
		return numOfFunds;
	}

	/**
	 * @param numOfFunds
	 *            the numOfFunds to set
	 */
	public void setNumOfFunds(Integer numOfFunds) {
		this.numOfFunds = numOfFunds;
	}

	/**
	 * @return the numOfShares
	 */
	public Long getNumOfShares() {
		return numOfShares;
	}

	/**
	 * @param numOfShares
	 *            the numOfShares to set
	 */
	public void setNumOfShares(Long numOfShares) {
		this.numOfShares = numOfShares;
	}
}