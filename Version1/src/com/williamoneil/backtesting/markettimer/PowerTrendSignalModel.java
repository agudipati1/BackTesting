/**
 * 
 */
package com.williamoneil.backtesting.markettimer;


/**
 * @author Gudipati
 *
 */
public class PowerTrendSignalModel implements ITimingSignal {

	private static final long serialVersionUID = 1L;
	
	private boolean on = false;
	
	public PowerTrendSignalModel(final boolean isOn) {
		super();
		this.on = isOn;
	}
	
	/**
	 * @return the on
	 */
	public boolean isOn() {
		return on;
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
		return "Power-Trend" + (on? " ON" : " OFF");
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Power-Trend" + (on? " ON" : " OFF");
	}

}
