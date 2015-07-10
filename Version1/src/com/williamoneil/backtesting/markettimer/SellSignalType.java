/**
 * 
 */
package com.williamoneil.backtesting.markettimer;

import java.io.Serializable;

/**
 * @author Gudipati
 *
 */
public enum SellSignalType implements Serializable {

	FOLLOWTHROUGH_UNDERCUT ("S1", "Follow-Through Undercut"),
	RALLY_UNDERCUT ("S2", "Failed Rally Attempt"),
	BREAK_BELOW_21D_MA ("S5", "Break below 21-day moving average"),
	OVERDUE_BREAK_BELOW_21D_MA ("S6", "Overdue break below 21-day moving average"),
	TRENDING_BELOW_21D_MA ("S7", "Trending below 21-day moving average"),
	LIVING_BELOW_21D_MA ("S8", "Living below 21-day moving average"),
	
	LOW_BELOW_50D_MA ("S9", "Low below 50-day moving average"),
	
	BAD_BREAK ("S10", "Bad Break"),
	LOWER_LOW ("S12", "Lower Low"),
	
	DOWNSIDE_REVERSAL("S11", "Downside reversal"),
	
	FULL_DISTRIBUTION_MINUS_ONE("S3", "Full distribution minus one"),
	FULL_DISTRIBUTION("S4", "Full Distribution"),
	
	CIRCUIT_BREAKER("SS0", "Circuit Breaker Rule: 10% off High"),
	CLOSE_BELOW_PREV_RALLY_HIGH ("SS1", "Buy switch turned off as index closed below the high that triggered buy-on");
	
	private String symbol = null;
	private String desc = null;
	
	private SellSignalType(String sym, String desc) {
		this.symbol = sym;
		this.desc = desc;
	}

	/**
	 * @return the symbol
	 */
	public String getSymbol() {
		return symbol;
	}

	/**
	 * @return the desc
	 */
	public String getDesc() {
		return desc;
	}
}
