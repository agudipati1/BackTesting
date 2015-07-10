/**
 * 
 */
package com.williamoneil.backtesting.markettimer;


/**
 * @author Gudipati
 *
 */
public class RallySignalModel implements ITimingSignal {

	private static final long serialVersionUID = 1L;
	public RallySignalModel() {
		super();
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
		return "Rally Day";
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Rally Day";
	}

}
