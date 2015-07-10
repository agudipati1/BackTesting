/**
 * 
 */
package com.williamoneil.backtesting.markettimer;


/**
 * @author Gudipati
 *
 */
public class DistributionDaySignalModel implements ITimingSignal {

	private static final long serialVersionUID = 1L;
	
	private boolean stall = false;
	
	public DistributionDaySignalModel(){
		this.stall = false;
	}
	
	public DistributionDaySignalModel(final boolean isStall) {
		super();
		this.stall = isStall;
	}
	
	/**
	 * @return the Stall 
	 */
	public boolean isStall() {
		return stall;
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#isBuySignal()
	 */
	@Override
	public Boolean isBuySignal() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#getName()
	 */
	@Override
	public String getName() {
		return stall ? "Stall Day" : "Distribution day";
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#getDescription()
	 */
	@Override
	public String getDescription() {
		return stall ? "Stall Day" : "Distribution day";
	}

}
