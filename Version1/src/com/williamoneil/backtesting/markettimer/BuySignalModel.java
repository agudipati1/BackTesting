/**
 * 
 */
package com.williamoneil.backtesting.markettimer;


/**
 * @author Gudipati
 *
 */
public class BuySignalModel implements ITimingSignal {

	private static final long serialVersionUID = 1L;
	
	private BuySignalType buySignalType = null;
	
	public BuySignalModel(final BuySignalType signalType) {
		super();
		this.buySignalType = signalType;
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#isBuySignal()
	 */
	@Override
	public Boolean isBuySignal() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#getName()
	 */
	@Override
	public String getName() {
		return buySignalType.getSymbol() + ":" + buySignalType.getDesc();
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#getDescription()
	 */
	@Override
	public String getDescription() {
		return buySignalType.getSymbol() + ":" + buySignalType.getDesc();
	}

}
