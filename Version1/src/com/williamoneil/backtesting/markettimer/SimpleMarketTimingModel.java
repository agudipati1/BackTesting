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
public class SimpleMarketTimingModel {

	private Date signalDate = null;
	private SimpleMarketSignalType marketSignalType = null;
	
	private List<DistributionDayModel> distributions = null;

	public SimpleMarketTimingModel(Date _signalDate) {
		this.signalDate = _signalDate;
	}
	
	/**
	 * @return the marketSignalType
	 */
	public SimpleMarketSignalType getMarketSignalType() {
		return marketSignalType;
	}

	/**
	 * @param marketSignalType the marketSignalType to set
	 */
	public void setMarketSignalType(SimpleMarketSignalType marketSignalType) {
		this.marketSignalType = marketSignalType;
	}




	/**
	 * @return the signalDate
	 */
	public Date getSignalDate() {
		return signalDate;
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
