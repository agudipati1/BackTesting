/**
 * 
 */
package com.williamoneil.backtesting.markettimer;

import java.util.Date;

/**
 * @author Gudipati
 *
 */
public class DistributionDayModel {

	private Date tradeDate = null;
	private boolean stall = false;
	
	
	public DistributionDayModel(){
	}
	
	public DistributionDayModel(Date tradeDate, boolean stall) {
		this.tradeDate = tradeDate;
		this.stall = stall;
	}
	
	/**
	 * @return the tradeDate
	 */
	public Date getTradeDate() {
		return tradeDate;
	}
	/**
	 * @param tradeDate the tradeDate to set
	 */
	public void setTradeDate(Date tradeDate) {
		this.tradeDate = tradeDate;
	}
	/**
	 * @return the stall
	 */
	public boolean isStall() {
		return stall;
	}
	/**
	 * @param stall the stall to set
	 */
	public void setStall(boolean stall) {
		this.stall = stall;
	}
}
