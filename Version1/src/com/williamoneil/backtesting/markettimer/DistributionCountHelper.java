/**
 * 
 */
package com.williamoneil.backtesting.markettimer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.williamoneil.backtesting.dao.InstrumentPriceModel;
import com.williamoneil.backtesting.util.Helpers;
import com.williamoneil.backtesting.util.HighLowTickInTradingDaysHelper;
import com.williamoneil.backtesting.util.PriceChartAnalyzer;

/**
 * @author Gudipati
 *
 */
public class DistributionCountHelper {

	
	public DistributionCountHelper(){
	}
	
	
	private List<TimingSignalWrapper> _distributionDays = new ArrayList<TimingSignalWrapper>();
	private List<TimingSignalWrapper> _stallDays = new ArrayList<TimingSignalWrapper>();
	
	// always add +1 to num-trading days for this helper class
	private HighLowTickInTradingDaysHelper highLowInLast2DaysHelper = new HighLowTickInTradingDaysHelper(2+1);
	private HighLowTickInTradingDaysHelper highLowInLast7DaysHelper = new HighLowTickInTradingDaysHelper(7+1);
	private HighLowTickInTradingDaysHelper highLowInLast25DaysHelper = new HighLowTickInTradingDaysHelper(25+1);
	
	private int getStallDaysCount() {
		if(_stallDays.size() > 0) {
			if(_stallDays.size() < _distributionDays.size()) {
				return _stallDays.size();
			} else {
				return 1;
			}
		}
		
		return 0;
	}
	
	public void resetDistributionAndStallDays() {
		_distributionDays = new ArrayList<TimingSignalWrapper>();
		_stallDays = new ArrayList<TimingSignalWrapper>();
	}
	
	public boolean processTickWithSimpleRules(SimpleTimingSignalData signal, SimpleTimingSignalData lastTick) {
		if(signal == null || signal.getPriceVolumeData() == null) {
			return false;
		}
		highLowInLast2DaysHelper.processTick(signal.getPriceVolumeData());
		highLowInLast7DaysHelper.processTick(signal.getPriceVolumeData());
		highLowInLast25DaysHelper.processTick(signal.getPriceVolumeData());
		
		// make a copy of original list as we are going to remove distributions that fell off
		final List<TimingSignalWrapper> distributionDays = new ArrayList<TimingSignalWrapper>();
		distributionDays.addAll(_distributionDays);
		boolean distributionsFallOff = false;
		// remove the distributions that fell off
		for(int i=0;i<distributionDays.size();i++) {
			int index = distributionDays.size()-i-1;
			final TimingSignalWrapper aDistDay = distributionDays.get(index);
			aDistDay.numDaysFromDist++;
			if(aDistDay.numDaysFromDist >= 25) {
				// 25 days have past since this distribution
				// remove distribution from original list
				_distributionDays.remove(index);
				distributionsFallOff = true;
			} else if(aDistDay.distDay.getPriceVolumeData().getClose().doubleValue() * 1.06 <= signal.getPriceVolumeData().getClose().doubleValue()) {
				// index moved up 6% from this distribution
				// remove distribution from original list
				_distributionDays.remove(index);
				distributionsFallOff = true;
			}
		}
		
		// make a copy of original list as we are going to remove stall that fell off
		final List<TimingSignalWrapper> stallDays = new ArrayList<TimingSignalWrapper>();
		stallDays.addAll(_stallDays);
		boolean stallDaysFallOff = false;
		// remove the distributions that fell off
		for(int i=0;i<stallDays.size();i++) {
			int index = stallDays.size()-i-1;
			final TimingSignalWrapper aDistDay = stallDays.get(index);
			aDistDay.numDaysFromDist++;
			if(aDistDay.numDaysFromDist >= 25) {
				// 25 days have past since this distribution
				// remove distribution from original list
				_stallDays.remove(index);
				stallDaysFallOff = true;
			} else if(aDistDay.distDay.getPriceVolumeData().getClose().doubleValue() * 1.06 <= signal.getPriceVolumeData().getClose().doubleValue()) {
				// index moved up 6% from this distribution
				// remove distribution from original list
				_stallDays.remove(index);
				stallDaysFallOff = true;
			}
		}
		
		int stallCount = getStallDaysCount();
		int currentDistCount = _distributionDays.size() + stallCount;
		
		final int fullDistCount = getFullDistributionCount(signal.getPriceVolumeData().getPriceDate());
		final int fullDistCountMinusTwo = fullDistCount - 2;
		// if distribution count falls below full-dist-count minus two and closes above 21dma and buy-switch-on
		if((distributionsFallOff||stallDaysFallOff) && signal.getSimpleMarketTiming().getMarketSignalType() != SimpleMarketSignalType.UPTREND 
				&& distributionDays.size() > fullDistCountMinusTwo && _distributionDays.size() <= fullDistCountMinusTwo) {
			if(signal.getPriceMAShort().doubleValue() > signal.getPriceVolumeData().getClose().doubleValue()
					|| signal.getPriceMAMedium().doubleValue() > signal.getPriceVolumeData().getClose().doubleValue()) {
						signal.getSimpleMarketTiming().setMarketSignalType(SimpleMarketSignalType.CAUTIOUS_UPTREND);
			} else {
						signal.getSimpleMarketTiming().setMarketSignalType(SimpleMarketSignalType.UPTREND);
			}
		}
		
		// check for distribution-day
		// distribution-day = closes down >= 0.2% with  volume >= that of prev day
		boolean distributionDayAdded= false;
		if(lastTick != null) {
		  final boolean isDistTick = Helpers.isDistributionDay(signal.getPriceVolumeData(), lastTick.getPriceVolumeData());
			if(isDistTick) {

				TimingSignalWrapper distWrapper = new  TimingSignalWrapper(signal);
				_distributionDays.add(distWrapper);
				distributionDayAdded = true;
			}
			
			// check for stall-day
			boolean stallDayAdded= false;
			if(!distributionDayAdded) {
				// process for stall day only if this is not a distribution-day
				final double chgPercent = 100 * (signal.getPriceVolumeData().getClose().doubleValue() - lastTick.getPriceVolumeData().getClose().doubleValue()) / lastTick.getPriceVolumeData().getClose().doubleValue();
				final double closingRange = PriceChartAnalyzer.getClosingRange(signal.getPriceVolumeData(), null);
				final double spread = (signal.getPriceVolumeData().getHigh().doubleValue() - signal.getPriceVolumeData().getLow().doubleValue()) / signal.getPriceVolumeData().getLow().doubleValue();
				
				final InstrumentPriceModel highTickInPast2Days = highLowInLast2DaysHelper.getHighTick();
				final InstrumentPriceModel highTickInPast7Days = highLowInLast7DaysHelper.getHighTick();
				final InstrumentPriceModel lowTickInPast7Days = highLowInLast7DaysHelper.getLowTick();
				final InstrumentPriceModel highTickInPast25Days = highLowInLast25DaysHelper.getHighTick();

				// stall-day conditions:
				// 1: price-up between 0% - +0.4% WITH volume >= 0.95 * prev-day-vol and closing range <= 50 AND price up in one of last-2-trading-days >= 0.2% and (price-high >= prior 2 days high OR price-high >= prior-day-high and within 3% of 25d price-high)
				// 2: closing-range between 50%-65% AND price-up between 0% - +0.4% AND (price-high >= high of last-7-days OR price-high >= 4% above low of last-7-days) AND (volume >= avg-vol AND (spread < 0.6 OR price-pct-chg <= 25% of highest-price-pct-change over lsat-2-days)) 
				// 3: closing-range between 65%-80% AND (price-high >= high of last-7-days OR price-high >= 4% above low of last-7-days) AND volume >= avg-vol AND vol > prev-day-vol AND spread < 0.6 AND price-pct-chg <= 0.2%
				
				//CONDITION#1:
				if( (chgPercent >= 0 && chgPercent <= 0.4) && closingRange <= 50 && 
						(signal.getPriceVolumeData().getVolume() >= 0.95 * lastTick.getPriceVolumeData().getVolume())) {	
					if(highTickInPast2Days != null && signal.getPriceVolumeData().getHigh().doubleValue() >= highTickInPast2Days.getHigh().doubleValue()
							|| 
							(signal.getPriceVolumeData().getHigh().doubleValue() >= lastTick.getPriceVolumeData().getHigh().doubleValue()
								&& highTickInPast25Days != null &&  1.03 * signal.getPriceVolumeData().getHigh().doubleValue() >= highTickInPast25Days.getHigh().doubleValue())) {

						final TimingSignalWrapper distWrapper = new  TimingSignalWrapper(signal);
						_stallDays.add(distWrapper);
						stallDayAdded = true;	
					}					
				}
				
				if(!stallDayAdded) {
					//CONDITION#2
					if((chgPercent >= 0 && chgPercent <= 0.4) && closingRange >= 50 && closingRange <= 65 && signal.getVolMA() != null && signal.getPriceVolumeData().getVolume() >= signal.getVolMA().longValue() 
							&& (
								   (highTickInPast7Days != null && signal.getPriceVolumeData().getHigh().doubleValue() >= highTickInPast7Days.getHigh().doubleValue())
								|| (lowTickInPast7Days != null && signal.getPriceVolumeData().getHigh().doubleValue() >= 1.04 * lowTickInPast7Days.getLow().doubleValue())
								 )
						){
						if(spread< 0.6) {
							final TimingSignalWrapper distWrapper = new  TimingSignalWrapper(signal);
							_stallDays.add(distWrapper);
							stallDayAdded = true;	
						} else {
							final List<InstrumentPriceModel> last2TradingDaysSignals = highLowInLast2DaysHelper.getPrices();
							if(last2TradingDaysSignals != null) {
								Double highestPricePctChg = null;
								InstrumentPriceModel prevSigTemp = null;
								for(final InstrumentPriceModel aSig : last2TradingDaysSignals) {
									if(prevSigTemp != null) {
										final double tempPctChg = 100 * (aSig.getClose().doubleValue() - prevSigTemp.getClose().doubleValue())/prevSigTemp.getClose().doubleValue();
										
										if(highestPricePctChg == null || highestPricePctChg <= tempPctChg) {
											highestPricePctChg = tempPctChg;
										}
									}
									prevSigTemp = aSig;
								}
								
								if(highestPricePctChg != null && chgPercent <= 0.25 * highestPricePctChg) {
									final TimingSignalWrapper distWrapper = new  TimingSignalWrapper(signal);
									_stallDays.add(distWrapper);
									stallDayAdded = true;			
								}
							}
						}
					}
				}
				
				if(!stallDayAdded) {
					//CONDITION#3
					if(closingRange >= 65 && closingRange <= 80 && spread < 0.6 && chgPercent <= 0.2 
							&& signal.getVolMA() != null && signal.getPriceVolumeData().getVolume() >= signal.getVolMA().longValue()
							&& lastTick != null && signal.getPriceVolumeData().getVolume() > lastTick.getPriceVolumeData().getVolume()) {
						if( (highTickInPast7Days != null && signal.getPriceVolumeData().getHigh().doubleValue() >= highTickInPast7Days.getHigh().doubleValue()) 
						   ||(lowTickInPast7Days != null && signal.getPriceVolumeData().getHigh().doubleValue() >= 1.04*lowTickInPast7Days.getLow().doubleValue()) ) {
							
							TimingSignalWrapper distWrapper = new  TimingSignalWrapper(signal);
							_stallDays.add(distWrapper);
							stallDayAdded = true;							
						}
					}
				}
			}
			// add the distributions & stalls to signal
			for(final TimingSignalWrapper aDistWrapper : _distributionDays) {
				final DistributionDayModel aDistData = new  DistributionDayModel(aDistWrapper.distDay.getPriceVolumeData().getPriceDate(), false);
				signal.getSimpleMarketTiming().getDistributions().add(aDistData);
			}
			for(final TimingSignalWrapper aDistWrapper : _stallDays) {
				final DistributionDayModel aDistData = new  DistributionDayModel(aDistWrapper.distDay.getPriceVolumeData().getPriceDate(), true);
				signal.getSimpleMarketTiming().getDistributions().add(aDistData);
			}
			
			if(distributionDayAdded || stallDayAdded) {
				stallCount = getStallDaysCount();
				currentDistCount = _distributionDays.size() + stallCount;
				
				if(currentDistCount >= fullDistCount && currentDistCount <= fullDistCount+2) {
					signal.getSimpleMarketTiming().setMarketSignalType(SimpleMarketSignalType.CAUTIOUS_UPTREND);
				}
			}
		}
		
		return false;
	}
	
	public boolean processTick(TimingSignalData signal, TimingSignalData lastTick) {
		if(signal == null || signal.getPriceVolumeData() == null) {
			return false;
		}
		highLowInLast2DaysHelper.processTick(signal.getPriceVolumeData());
		highLowInLast7DaysHelper.processTick(signal.getPriceVolumeData());
		highLowInLast25DaysHelper.processTick(signal.getPriceVolumeData());
		
		// make a copy of original list as we are going to remove distributions that fell off
		final List<TimingSignalWrapper> distributionDays = new ArrayList<TimingSignalWrapper>();
		distributionDays.addAll(_distributionDays);
		boolean distributionsFallOff = false;
		// remove the distributions that fell off
		for(int i=0;i<distributionDays.size();i++) {
			int index = distributionDays.size()-i-1;
			final TimingSignalWrapper aDistDay = distributionDays.get(index);
			aDistDay.numDaysFromDist++;
			if(aDistDay.numDaysFromDist >= 25) {
				// 25 days have past since this distribution
				// remove distribution from original list
				_distributionDays.remove(index);
				distributionsFallOff = true;
			} else if(aDistDay.distDay.getPriceVolumeData().getClose().doubleValue() * 1.06 <= signal.getPriceVolumeData().getClose().doubleValue()) {
				// index moved up 6% from this distribution
				// remove distribution from original list
				_distributionDays.remove(index);
				distributionsFallOff = true;
			}
		}
		
		// make a copy of original list as we are going to remove stall that fell off
		final List<TimingSignalWrapper> stallDays = new ArrayList<TimingSignalWrapper>();
		stallDays.addAll(_stallDays);
		boolean stallDaysFallOff = false;
		// remove the distributions that fell off
		for(int i=0;i<stallDays.size();i++) {
			int index = stallDays.size()-i-1;
			final TimingSignalWrapper aDistDay = stallDays.get(index);
			aDistDay.numDaysFromDist++;
			if(aDistDay.numDaysFromDist >= 25) {
				// 25 days have past since this distribution
				// remove distribution from original list
				_stallDays.remove(index);
				stallDaysFallOff = true;
			} else if(aDistDay.distDay.getPriceVolumeData().getClose().doubleValue() * 1.06 <= signal.getPriceVolumeData().getClose().doubleValue()) {
				// index moved up 6% from this distribution
				// remove distribution from original list
				_stallDays.remove(index);
				stallDaysFallOff = true;
			}
		}
		
		int stallCount = getStallDaysCount();
		int currentDistCount = _distributionDays.size() + stallCount;
		
		final int fullDistCount = getFullDistributionCount(signal.getPriceVolumeData().getPriceDate());
		final int fullDistCountMinusOne = fullDistCount - 1;
		final int fullDistCountMinusTwo = fullDistCount - 2;
		// if distribution count falls below full-dist-count minus two and closes above 21dma and buy-switch-on
		if((distributionsFallOff||stallDaysFallOff) && signal.getMarketTiming().isBuySwitchOn() 
				&& signal.getPriceVolumeData().getClose().doubleValue() > signal.getPriceMAShort().doubleValue()
				&& distributionDays.size() > fullDistCountMinusTwo && _distributionDays.size() <= fullDistCountMinusTwo) {
			final BuySignalModel buySignal = new BuySignalModel(BuySignalType.DISTRIBUTION_DAY_FALLOFF);
			signal.addSignal(buySignal);
		}
		
		// check for distribution-day
		// distribution-day = closes down >= 0.2% with  volume >= that of prev day
		boolean distributionDayAdded= false;
		if(lastTick != null) {
		  final boolean isDistTick = Helpers.isDistributionDay(signal.getPriceVolumeData(), lastTick.getPriceVolumeData());
			if(isDistTick) {
				signal.addSignal(new DistributionDaySignalModel(false));
					
				TimingSignalWrapper distWrapper = new  TimingSignalWrapper(signal);
				_distributionDays.add(distWrapper);
				distributionDayAdded = true;
			}
			
			// check for stall-day
			boolean stallDayAdded= false;
			if(!distributionDayAdded) {
				// process for stall day only if this is not a distribution-day
				final double chgPercent = 100 * (signal.getPriceVolumeData().getClose().doubleValue() - lastTick.getPriceVolumeData().getClose().doubleValue()) / lastTick.getPriceVolumeData().getClose().doubleValue();
				final double closingRange = PriceChartAnalyzer.getClosingRange(signal.getPriceVolumeData(), null);
				final double spread = (signal.getPriceVolumeData().getHigh().doubleValue() - signal.getPriceVolumeData().getLow().doubleValue()) / signal.getPriceVolumeData().getLow().doubleValue();
				
				final InstrumentPriceModel highTickInPast2Days = highLowInLast2DaysHelper.getHighTick();
				final InstrumentPriceModel highTickInPast7Days = highLowInLast7DaysHelper.getHighTick();
				final InstrumentPriceModel lowTickInPast7Days = highLowInLast7DaysHelper.getLowTick();
				final InstrumentPriceModel highTickInPast25Days = highLowInLast25DaysHelper.getHighTick();

				// stall-day conditions:
				// 1: price-up between 0% - +0.4% WITH volume >= 0.95 * prev-day-vol and closing range <= 50 AND price up in one of last-2-trading-days >= 0.2% and (price-high >= prior 2 days high OR price-high >= prior-day-high and within 3% of 25d price-high)
				// 2: closing-range between 50%-65% AND price-up between 0% - +0.4% AND (price-high >= high of last-7-days OR price-high >= 4% above low of last-7-days) AND (volume >= avg-vol AND (spread < 0.6 OR price-pct-chg <= 25% of highest-price-pct-change over lsat-2-days)) 
				// 3: closing-range between 65%-80% AND (price-high >= high of last-7-days OR price-high >= 4% above low of last-7-days) AND volume >= avg-vol AND vol > prev-day-vol AND spread < 0.6 AND price-pct-chg <= 0.2%
				
				//CONDITION#1:
				if( (chgPercent >= 0 && chgPercent <= 0.4) && closingRange <= 50 && 
						(signal.getPriceVolumeData().getVolume() >= 0.95 * lastTick.getPriceVolumeData().getVolume())) {	
					if(highTickInPast2Days != null && signal.getPriceVolumeData().getHigh().doubleValue() >= highTickInPast2Days.getHigh().doubleValue()
							|| 
							(signal.getPriceVolumeData().getHigh().doubleValue() >= lastTick.getPriceVolumeData().getHigh().doubleValue()
								&& highTickInPast25Days != null &&  1.03 * signal.getPriceVolumeData().getHigh().doubleValue() >= highTickInPast25Days.getHigh().doubleValue())) {
						signal.addSignal(new DistributionDaySignalModel(true));
						
						final TimingSignalWrapper distWrapper = new  TimingSignalWrapper(signal);
						_stallDays.add(distWrapper);
						stallDayAdded = true;	
					}					
				}
				
				if(!stallDayAdded) {
					//CONDITION#2
					if((chgPercent >= 0 && chgPercent <= 0.4) && closingRange >= 50 && closingRange <= 65 && signal.getVolMA() != null && signal.getPriceVolumeData().getVolume() >= signal.getVolMA().longValue() 
							&& (
								   (highTickInPast7Days != null && signal.getPriceVolumeData().getHigh().doubleValue() >= highTickInPast7Days.getHigh().doubleValue())
								|| (lowTickInPast7Days != null && signal.getPriceVolumeData().getHigh().doubleValue() >= 1.04 * lowTickInPast7Days.getLow().doubleValue())
								 )
						){
						if(spread< 0.6) {
							signal.addSignal(new DistributionDaySignalModel(true));
							
							final TimingSignalWrapper distWrapper = new  TimingSignalWrapper(signal);
							_stallDays.add(distWrapper);
							stallDayAdded = true;	
						} else {
							final List<InstrumentPriceModel> last2TradingDaysSignals = highLowInLast2DaysHelper.getPrices();
							if(last2TradingDaysSignals != null) {
								Double highestPricePctChg = null;
								InstrumentPriceModel prevSigTemp = null;
								for(final InstrumentPriceModel aSig : last2TradingDaysSignals) {
									if(prevSigTemp != null) {
										final double tempPctChg = 100 * (aSig.getClose().doubleValue() - prevSigTemp.getClose().doubleValue())/prevSigTemp.getClose().doubleValue();
										
										if(highestPricePctChg == null || highestPricePctChg <= tempPctChg) {
											highestPricePctChg = tempPctChg;
										}
									}
									prevSigTemp = aSig;
								}
								
								if(highestPricePctChg != null && chgPercent <= 0.25 * highestPricePctChg) {
									signal.addSignal(new DistributionDaySignalModel(true));
									
									final TimingSignalWrapper distWrapper = new  TimingSignalWrapper(signal);
									_stallDays.add(distWrapper);
									stallDayAdded = true;			
								}
							}
						}
					}
				}
				
				if(!stallDayAdded) {
					//CONDITION#3
					if(closingRange >= 65 && closingRange <= 80 && spread < 0.6 && chgPercent <= 0.2 
							&& signal.getVolMA() != null && signal.getPriceVolumeData().getVolume() >= signal.getVolMA().longValue()
							&& lastTick != null && signal.getPriceVolumeData().getVolume() > lastTick.getPriceVolumeData().getVolume()) {
						if( (highTickInPast7Days != null && signal.getPriceVolumeData().getHigh().doubleValue() >= highTickInPast7Days.getHigh().doubleValue()) 
						   ||(lowTickInPast7Days != null && signal.getPriceVolumeData().getHigh().doubleValue() >= 1.04*lowTickInPast7Days.getLow().doubleValue()) ) {
							signal.addSignal(new DistributionDaySignalModel(true));
							
							TimingSignalWrapper distWrapper = new  TimingSignalWrapper(signal);
							_stallDays.add(distWrapper);
							stallDayAdded = true;							
						}
					}
				}
			}
			// add the distributions & stalls to signal
			for(final TimingSignalWrapper aDistWrapper : _distributionDays) {
				final DistributionDayModel aDistData = new  DistributionDayModel(aDistWrapper.distDay.getPriceVolumeData().getPriceDate(), false);
				signal.getMarketTiming().getDistributions().add(aDistData);
			}
			for(final TimingSignalWrapper aDistWrapper : _stallDays) {
				final DistributionDayModel aDistData = new  DistributionDayModel(aDistWrapper.distDay.getPriceVolumeData().getPriceDate(), true);
				signal.getMarketTiming().getDistributions().add(aDistData);
			}
			
			if(distributionDayAdded || stallDayAdded) {
				stallCount = getStallDaysCount();
				currentDistCount = _distributionDays.size() + stallCount;
				
				if(currentDistCount == fullDistCountMinusOne) {
					final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.FULL_DISTRIBUTION_MINUS_ONE);
					signal.addSignal(sellSignal);
				} else if(currentDistCount >= fullDistCount && currentDistCount <= fullDistCount+2) {
					final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.FULL_DISTRIBUTION);
					signal.addSignal(sellSignal);
					
					if(signal.getMarketTiming().getSignalCount() <= 0) {
						signal.getMarketTiming().setBuySwitchOn(false);

						signal.resetSignalCount();
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	
	
	@SuppressWarnings("deprecation")
	public static int getFullDistributionCount(Date date) {
		if(date.getYear() >= 105) {
			// 2005 - present
			return 6;
		} else if(date.getYear() >= 91 && date.getYear() <= 104) {
			// 1991 - 2004
			return 5;
		} else {
			// all years before 1991
			return 4;
		}
	}
	
	class TimingSignalWrapper {
		public ITimingSignalData distDay = null;
		public int numDaysFromDist = 0;
		public TimingSignalWrapper(ITimingSignalData _data) {
			distDay = _data;
			numDaysFromDist = 1;
		}
		
	}
}
