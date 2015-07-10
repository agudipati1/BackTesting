/**
 * 
 */
package com.williamoneil.backtesting.markettimer;


/**
 * @author Gudipati
 *
 */
public class SellSignalModel implements ITimingSignal {

	private static final long serialVersionUID = 1L;
	
	private SellSignalType sellSignalType = null;
	
	public SellSignalModel(final SellSignalType sellSignalType) {
		super();
		this.sellSignalType = sellSignalType;
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#isBuySignal()
	 */
	@Override
	public Boolean isBuySignal() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#getName()
	 */
	@Override
	public String getName() {
		return sellSignalType.getSymbol() + ":" + sellSignalType.getDesc();
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.portfolios.data.ITimingSignal#getDescription()
	 */
	@Override
	public String getDescription() {
		return sellSignalType.getSymbol() + ":" + sellSignalType.getDesc();
	}

}
