/**
 * 
 */
package com.williamoneil.backtesting.markettimer;

import java.io.Serializable;

/**
 * @author Gudipati
 *
 */
public enum SimpleMarketSignalType implements Serializable {
	UPTREND(true, 0, 100, true),
	DOWNTREND(false, 0, 0, false),
	CAUTIOUS_UPTREND(true, 0, 50, false);
	
	private boolean buySwitchOn = false;
	private int minMarketExposure = 0;
	private int maxMarketExposure = 0;
	private boolean marginOn = false;
	
	private SimpleMarketSignalType(boolean buy, int min, int max, boolean margin) {
		this.buySwitchOn = buy;
		this.marginOn = margin;
		this.minMarketExposure = min;
		this.maxMarketExposure = max;
	}

	/**
	 * @return the buySwitchOn
	 */
	public boolean isBuySwitchOn() {
		return buySwitchOn;
	}

	/**
	 * @return the minMarketExposure
	 */
	public int getMinMarketExposure() {
		return minMarketExposure;
	}

	/**
	 * @return the maxMarketExposure
	 */
	public int getMaxMarketExposure() {
		return maxMarketExposure;
	}

	/**
	 * @return the marginOn
	 */
	public boolean isMarginOn() {
		return marginOn;
	}
}
