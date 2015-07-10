/**
 * 
 */
package com.williamoneil.backtesting.markettimer;


/**
 * @author Gudipati
 *
 */
public class IndexNewHighSignalModel implements ITimingSignal {

	private static final long serialVersionUID = 1L;
	public IndexNewHighSignalModel() {
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
		return "BB0-Index moving new high without follow-through-day. Buy switch ON.";
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#getDescription()
	 */
	@Override
	public String getDescription() {
		return "BB0-Index moving new high without follow-through-day. Buy switch ON.";
	}

}
