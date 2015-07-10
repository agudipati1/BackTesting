/**
 * 
 */
package com.williamoneil.backtesting.markettimer;

import java.io.Serializable;

/**
 * @author Gudipati
 *
 */
public enum BuySignalType implements Serializable {
	FOLLOW_THROUGH_DAY("B1", "Follow Through Day"),
	ADDTL_FOLLOW_THROUGH_DAY("B2", "Additional Follow Through Day"),
	LOW_ABOVE_21D_MA ("B3", "Low above 21-day moving average"),
	TRENDING_ABOVE_21D_MA ("B4", "Trending above 21-day moving average"),
	LIVING_ABOVE_21D_MA ("B5", "Living above 21-day moving average"),
	
	LOW_ABOVE_50D_MA ("B6", "Low above 50-day moving average"),
	
	ACCUMULATION_AT_NEW_HIGH("B7","Accumulation day at new high"),
	HIGHER_HIGH("B8","Higher High"),
	
	DOWNSIDE_REVERSAL_BUYBACK("B9", "Downside reversal buyback"),
	
	DISTRIBUTION_DAY_FALLOFF("B10", "Distribution day fall off");
	
	private String symbol = null;
	private String desc = null;
	
	private BuySignalType(String sym, String desc) {
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
