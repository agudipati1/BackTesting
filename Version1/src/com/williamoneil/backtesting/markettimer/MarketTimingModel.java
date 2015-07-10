/**
 * 
 */
package com.williamoneil.backtesting.markettimer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Gudipati
 *
 */
public class MarketTimingModel {

	private Date signalDate = null;
	
	private List<ITimingSignal> signals = null;
	private Integer signalCount = null;
	
	private boolean buySwitchOn = false;
	private boolean powerTrendOn = false; 
	
	private List<DistributionDayModel> distributions = null;


	public MarketTimingModel(Date _signalDate) {
		this.signalDate = _signalDate;
	}
	
	public int getMarketExposurePercent() {
		if(signalCount == null || !buySwitchOn || signalCount <= 0) {
			return 0;
		} else {
			if(signalCount == 1) {
				return 30;
			} else if(signalCount == 2) {
				return 55;
			} else if(signalCount == 3) {
				return 75;
			} else if(signalCount == 4) {
				return 90;
			} else if(signalCount == 5) {
				return 100;
			} else if (signalCount > 5) {
				return 101;
			}
		}
		
		return 0;
	}
	
	/**
	 * @return the signalDate
	 */
	public Date getSignalDate() {
		return signalDate;
	}

	/**
	 * @return the signals
	 */
	public List<ITimingSignal> getSignals() {
		if(signals == null) {
			signals = new  ArrayList<ITimingSignal>();
		}
		return signals;
	}


	/**
	 * @return the signalCount
	 */
	public Integer getSignalCount() {
		return signalCount;
	}
	
	/**
	 * @param signalCount the signalCount to set
	 */
	public void setSignalCount(Integer signalCount) {
		this.signalCount = signalCount;
	}

	/**
	 * @return the buySwitchOn
	 */
	public boolean isBuySwitchOn() {
		return buySwitchOn;
	}

	/**
	 * @param buySwitchOn the buySwitchOn to set
	 */
	public void setBuySwitchOn(boolean buySwitchOn) {
		this.buySwitchOn = buySwitchOn;
	}

	/**
	 * @return the powerTrendOn
	 */
	public boolean isPowerTrendOn() {
		return powerTrendOn;
	}

	/**
	 * @return the distributions
	 */
	public List<DistributionDayModel> getDistributions() {
		if(distributions == null) {
			distributions = new ArrayList<DistributionDayModel>();
		}
		return distributions;
	}
}
