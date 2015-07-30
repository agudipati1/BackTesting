/**
 * 
 */
package com.williamoneil.backtesting.markettimer;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.beans.factory.annotation.Autowired;

import com.williamoneil.ApplicationException;
import com.williamoneil.backtesting.dao.InstrumentPriceModel;
import com.williamoneil.backtesting.dao.PeriodicityType;
import com.williamoneil.backtesting.dao.SymbolInfoModel;
import com.williamoneil.backtesting.dao.TradeDateType;
import com.williamoneil.backtesting.dao.WONDAOImpl;
import com.williamoneil.backtesting.util.Helpers;
import com.williamoneil.backtesting.util.HighTickInCalendarDaysHelper;
import com.williamoneil.backtesting.util.PriceChartAnalyzer;
import com.williamoneil.backtesting.util.PriceMovingAverageHelper;
import com.williamoneil.backtesting.util.VolumeMovingAverageHelper;

/**
 * @author Gudipati
 *
 */
public class WONMarketTimer  {
	private static final Log logger = LogFactory.getLog(WONMarketTimer.class);
	
	@Autowired
	private WONDAOImpl wonDAO = null;

	String[] marketIndices = new String[]{"0NDQC"};//, "0S&P5", "0DJIA"};
	
	private HashMap<String , List<TimingSignalData>> marketTimingModelsMap = null;
	public void init(Date startDate, Date endDate) throws ApplicationException {
		marketTimingModelsMap = new HashMap<String, List<TimingSignalData>>();
		
		// get last n-years worth of data
		final Calendar startCal = Calendar.getInstance();
		startCal.setTime(startDate);
		startCal.add(Calendar.DATE, -20);//-365 * 2); //add 2 years back to the index start-time to accomodate for past signals

		logger.debug("Initiating WON-Market-Timer from: " + startCal.getTime() + " to " + endDate);
		
		for(final String aIndex : marketIndices) {
			final List<TimingSignalData> signals = getMarketTiming(aIndex, startCal.getTime(), endDate);
			
			marketTimingModelsMap.put(aIndex, signals);
		}
	}
	
	public MarketTimingModel getMarketTiming(Date dt) throws ApplicationException {
		final List<TimingSignalData> signals = marketTimingModelsMap.get("0NDQC");

		if(signals != null && signals.size() > 0) {
			TimingSignalData latestSignal = null;
			for(final TimingSignalData signal : signals) {
				if(signal.getMarketTiming().getSignalDate().before(dt)) {
					latestSignal = signal;
				} else {
					break;
				}
			}
			
			if(latestSignal != null) {
				return latestSignal.getMarketTiming();
			}
		}
		
		return null;
	}
	
	
	public WONDAOImpl getWonDAO() {
		return wonDAO;
	}
	public void setWonDAO(WONDAOImpl wonDAO) {
		this.wonDAO = wonDAO;
	}

	private List<TimingSignalData> getMarketTiming(String indexSymbol, Date startDate, Date endDate) throws ApplicationException {
		final SymbolInfoModel indexInfo = wonDAO.getSymbolInfoDataForSymbol(indexSymbol);
		if(indexInfo == null) {
			throw new ApplicationException("No index symbol found for: " + indexSymbol);
		}
		final List<InstrumentPriceModel> prices = wonDAO.getPriceHistory(indexInfo.getInstrumentId(), startDate, endDate, PeriodicityType.DAILY, false, null);
		
		final List<TimingSignalData> signals = WONMarketTimerHelper.getMarketTimings(prices);
		
		final DateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		if(signals != null && signals.size() > 0) {
			for(final TimingSignalData signal : signals) {
				if(signal.getMarketTiming().getSignals() != null && signal.getMarketTiming().getSignals().size() > 0) {
					for(final ITimingSignal aSignal : signal.getMarketTiming().getSignals()) {
						System.out.println(sdf.format(signal.getPriceVolumeData().getPriceDate()) + " : " + (aSignal.isBuySignal() == null ? "<" : (signal.getMarketTiming().getSignalCount()  == null ? "?" : signal.getMarketTiming().getSignalCount()) + "-")  + aSignal.getName() + (aSignal.isBuySignal() == null ? ">" : ""));
					}
				}
			}
			
			/*
			// also print the final day's analysis
			if(signals.size() > 1) {
				final TimingSignalData lastSignal = signals.get(signals.size()-1);
				System.out.println("----------------");
				System.out.println(sdf.format(lastSignal.getPriceVolumeData().getPriceDate()));
				System.out.println("Buy Switch: " + lastSignal.getMarketTiming().isBuySwitchOn());
				if(lastSignal.getMarketTiming().getDistributions() != null && lastSignal.getMarketTiming().getDistributions().size() > 0) {
					String dists = "";
					for(final DistributionDayModel aDist : lastSignal.getMarketTiming().getDistributions()) {
						dists += sdf.format(aDist.getTradeDate()) + "(" + (aDist.isStall() ? "S" : "D") + "),";
					}
					System.out.println("Current distrubution days: " + dists);
					System.out.println("Market Exposure: " + lastSignal.getMarketTiming().getMarketExposurePercent() + "%");
				}
				
				if(lastSignal.getMarketTiming().getSignals() != null && lastSignal.getMarketTiming().getSignals().size() > 0) {
					for(final ITimingSignal aSignal : lastSignal.getMarketTiming().getSignals()) {
						System.out.println((lastSignal.getMarketTiming().getSignalCount()  == null ? "?" : lastSignal.getMarketTiming().getSignalCount()) + "-"  + aSignal.getName());
					}
				}
			}
			*/
		}
		
		return signals;
	}
	
}

class WONMarketTimerHelper {
	
	@SuppressWarnings("deprecation")
	private static final Date DATE_JAN1_2000 = new Date(100,0,1);
	
	@SuppressWarnings("deprecation")
	public static List<TimingSignalData> getMarketTimings(List<InstrumentPriceModel> _pvList) {
		if(_pvList == null || _pvList.size() <= 1) {
			return null;
		}
		
		final List<InstrumentPriceModel> pvList = new ArrayList<InstrumentPriceModel>(_pvList);
		if(pvList.get(0).getPriceDate().after(pvList.get(1).getPriceDate())) {
			Collections.reverse(pvList);
		}
		
		final InstrumentPriceModel latestTick = pvList.get(pvList.size() - 1);
		final Long currentLatestProjectedVol = Helpers.getProjectedVolume(latestTick);
		if(currentLatestProjectedVol != null) {
		  latestTick.setVolume(currentLatestProjectedVol);
		}
		
		final PriceMovingAverageHelper pMAHelper1 = new PriceMovingAverageHelper(21, false); //21 EMA
		final PriceMovingAverageHelper pMAHelper2 = new PriceMovingAverageHelper(50, true); //50 SMA
		final VolumeMovingAverageHelper vMAHelper = new VolumeMovingAverageHelper(50, true); //50 SMA
		
		final PriceMarkedLabelHelper priceMarker = new PriceMarkedLabelHelper();
		
		final List<TimingSignalData> signals = new ArrayList<TimingSignalData>();
		
		//first calculate the signal data with MAs
		BigDecimal lastPriceMA1 = null;
		BigDecimal lastPriceMA2 = null;
		BigDecimal lastVolMA = null;
		for(int i = 0; i < pvList.size(); i++) {
			final InstrumentPriceModel pvData = pvList.get(i);
			if(pvData == null) {
				continue;
			}
			
			final TimingSignalData signal = new TimingSignalData(pvData, new MarketTimingModel(pvData.getPriceDate()));
			signals.add(signal);
			

            if(signal.getPriceVolumeData().getPriceDate().after(new  Date(114, 0, 1))) {
            	System.out.print("");
               // System.err.println(signal.getPriceVolumeData().getPriceDate());
            }
			
			priceMarker.processTick(pvList, signal, i);
			
			// first calculate moving averages because we would need it in checking the signals
			final BigDecimal priceMA1 = pMAHelper1.processTick(pvData, lastPriceMA1);
			final BigDecimal priceMA2 = pMAHelper2.processTick(pvData, lastPriceMA2);
			final BigDecimal volMA = vMAHelper.processTick(pvData, lastVolMA);

			signal.setPriceMAShort(priceMA1);
			signal.setPriceMAMedium(priceMA2);
			signal.setVolMA(volMA);
			
			lastPriceMA1 = priceMA1;
			lastPriceMA2 = priceMA2;
			lastVolMA = volMA;			
		}
		
		boolean rallyWasFoundAtleastOnce = false;
		
		boolean powerTrendOn = false;
		// next we process the signal list and populate the market-signals into it
		TimingSignalData lastTick = null;
		
		TimingSignalData lastRallySignal = null;
		int numTradingDaysFromLastRallySignal = 0;
		int numTradingDaysAbove21dMA = 0;
		int numTradingDaysBelow21dMA = 0;
		int markedNumTradingDaysAbove21dMA = 0;
		
		Date dayWhen21dMATrendedOver50dMA = null;
		
		TimingSignalData followThroughBuySignal = null;
		
		TimingSignalData lowAbove21dMABuySignal = null;
		TimingSignalData trendingAbove21dMABuySignal = null;
		TimingSignalData livingAbove21dMABuySignal = null;
		
		TimingSignalData followThroughUndercutSellSignal = null;
		
		TimingSignalData lowBelow21dMASellSignal = null;
		TimingSignalData overdueBreakBelow21dMASellSignal = null;
		TimingSignalData trendingBelow21dMASellSignal = null;
		TimingSignalData livingBelow21dMASellSignal = null;
		
		TimingSignalData lowAbove50dMABuySignal = null;
		TimingSignalData lowBelow50dMASellSignal = null;
		
		TimingSignalData higherHighBuySignal = null;
		TimingSignalData lowerLowSellSignal = null;
		
		TimingSignalData downsideReversalSellSignal = null;
		int numTradingDaysFromDownsideReversalSell = 0;
		
		InstrumentPriceModel lastPriceMarkedHigh = null;
		InstrumentPriceModel lastPriceMarkedLow = null;
		
		InstrumentPriceModel highSinceFollowThrough = null;
		
		InstrumentPriceModel prevRallyHigh = null;
		InstrumentPriceModel currentRallyHigh = null;
		
		BigDecimal checkPriceBecauseIndexHighWithoutFTSignal = null;
		
		final HighTickInCalendarDaysHelper highHelper = new HighTickInCalendarDaysHelper(13 * 7); //13 weeks
		final DistributionCountHelper distributionCountHelper = new DistributionCountHelper();
		
		for(int i = 0; i < signals.size(); i++) {
			final TimingSignalData signal = signals.get(i);
			if(signal == null || signal.getPriceVolumeData() == null || signal.getPriceVolumeData().getDateType() != TradeDateType.TRADING_DATE || signal.getPriceVolumeData().getVolume() == null) {
				continue;
			}
			
			if(signal.getPriceVolumeData().getPriceDate().after(new  Date(114, 0, 1))) {
				System.err.println("");
			}
			
			if(lastTick != null) {
				signal.getMarketTiming().setBuySwitchOn(lastTick.getMarketTiming().isBuySwitchOn());
				signal.getMarketTiming().setSignalCount(lastTick.getMarketTiming().getSignalCount());
			} else {
				signal.getMarketTiming().setBuySwitchOn(false);
				signal.getMarketTiming().setSignalCount(null);
			}
			
			// POWER TREND LOGIC
			if(signal.getPriceMAMedium() != null && signal.getPriceMAShort() != null) {
				if(signal.getPriceMAShort().compareTo(signal.getPriceMAMedium()) > 0) {
					if(dayWhen21dMATrendedOver50dMA == null) {
						dayWhen21dMATrendedOver50dMA = signal.getPriceVolumeData().getPriceDate();
						powerTrendOn = false;
					} else {
						// check if we need to turn on power-trend
						if(!powerTrendOn) {
							// 8 weeks of 21dma trending above 50dma = power-trend on
							final int daysDiff = Math.abs(Days.daysBetween(new DateTime(dayWhen21dMATrendedOver50dMA), new DateTime(signal.getPriceVolumeData().getPriceDate())).getDays());
							if(daysDiff >= 8*7) {
								powerTrendOn = true;
								final PowerTrendSignalModel ptSignal = new PowerTrendSignalModel(powerTrendOn);
								signal.addSignal(ptSignal);
							} else {
								powerTrendOn = false;
							}
						}
					}
				} else {
					if(powerTrendOn) {
						powerTrendOn = false;
						final PowerTrendSignalModel ptSignal = new PowerTrendSignalModel(false);
						signal.addSignal(ptSignal);
					}
					
					dayWhen21dMATrendedOver50dMA = null;
				}
			}
			
			if(lastRallySignal != null) {
				numTradingDaysFromLastRallySignal++;
			} else {
				numTradingDaysFromLastRallySignal = 0;
			}
			
			final boolean highInPast13Weeks = highHelper.processTick(signal.getPriceVolumeData());
			
			// find a rally-day if it is not previously found
			if(lastRallySignal == null) {
				//it is not a buy signal if no rally date was found
				signal.getMarketTiming().setBuySwitchOn(false);
				
				// since we may not have the FULL price-ticks, lets assume that if the current tick is below the price-marked-low then it could be a potential rally tick
				if(!rallyWasFoundAtleastOnce && !signal.isPriceMarkedLow()) {
					// we havent found a price-marked-low yet.. lets move on
					//lastTick = signal;
					continue;
				} else {
					// this is the marked low tick or is lower than the marked low tick
					// lets consider this for our rally day criteria
					final boolean aPotentialRallyDay = isRallyDay(signal, lastTick);
					if (aPotentialRallyDay) {
						lastRallySignal = signal;
						numTradingDaysFromLastRallySignal++;

						rallyWasFoundAtleastOnce = true;
						signal.resetSignalCount();
					}
				}
			} else {
				// rally signal is found.. lets try to find other signals now
				
					// first if the current-days low under-cuts rally-date then this is not a rally.
					if(followThroughBuySignal == null && signal.getPriceVolumeData().getLow().doubleValue() < lastRallySignal.getPriceVolumeData().getLow().doubleValue()) {
						lastRallySignal = null;
						numTradingDaysFromLastRallySignal = 0;
						signal.getMarketTiming().setBuySwitchOn(false);
						
						signal.resetSignalCount();
						
						followThroughBuySignal = null;
						
						followThroughUndercutSellSignal = null;
						
						highSinceFollowThrough = null;
						
						downsideReversalSellSignal = null;
						numTradingDaysFromDownsideReversalSell = 0;
						
						currentRallyHigh = null;
						checkPriceBecauseIndexHighWithoutFTSignal = null;
						
						// we negated the last-rally-day.. lets check if today is a rally-day
						final boolean aPotentialRallyDay = isRallyDay(signal, lastTick);
						if (aPotentialRallyDay) {
							lastRallySignal = signal;
							numTradingDaysFromLastRallySignal++;

							rallyWasFoundAtleastOnce = true;
							signal.resetSignalCount();
						}
						
					} else {
						
						// keep track of current rally high
						if(currentRallyHigh == null) {
							currentRallyHigh = signal.getPriceVolumeData();
						} else if(currentRallyHigh.getHigh().doubleValue() < signal.getPriceVolumeData().getHigh().doubleValue()) {
							currentRallyHigh = signal.getPriceVolumeData();
						}

						// check for a follow through day 
						final BuySignalModel ftSignal = findFollowThroughSignal(signal, lastTick, numTradingDaysFromLastRallySignal, followThroughBuySignal);
						if(ftSignal != null) {							
							if(followThroughBuySignal == null) {
								// this is  a first follow-through-day
								followThroughBuySignal = signal;
								
								if(!signal.getMarketTiming().isBuySwitchOn()) {
									signal.getMarketTiming().setBuySwitchOn(true);
								
									// once we confirm that this rally has a Follow-Through, we register it as rally-date
									final RallySignalModel rallySignal = new RallySignalModel();
									lastRallySignal.addSignal(rallySignal);
								}
								
								//reset distribution count when a follow through day is found
								distributionCountHelper.resetDistributionAndStallDays();
	
								followThroughUndercutSellSignal = null;
								
								downsideReversalSellSignal = null;
								numTradingDaysFromDownsideReversalSell = 0;
							}
							
							// add the ft-signal to the signal-list
							signal.addSignal(ftSignal);
						} 
						
							// check for index-moving-new-high-without-FT signal
							if(!signal.getMarketTiming().isBuySwitchOn() && followThroughBuySignal == null && lastRallySignal != null && 
									prevRallyHigh != null && currentRallyHigh.getClose().doubleValue() >= prevRallyHigh.getHigh().doubleValue()) {
								// index moving into new high without follow-through-day.. we turn the buy-switch on
								signal.getMarketTiming().setBuySwitchOn(true);
								
								checkPriceBecauseIndexHighWithoutFTSignal = prevRallyHigh.getHigh();
								
								// we also confirm this rally and register it as rally-date
								final RallySignalModel rallySignal = new RallySignalModel();
								lastRallySignal.addSignal(rallySignal);
								
								downsideReversalSellSignal = null;
								numTradingDaysFromDownsideReversalSell = 0;
								
								final IndexNewHighSignalModel newHighSignal = new IndexNewHighSignalModel();
								signal.addSignal(newHighSignal);
								
								prevRallyHigh = null;
							} else {
								/*ARVIND 06192015 - dont see the below in marketschool book. also this signal triggered on 6/19 for 0ndqc 
								 * 
								 *
								// check if index closes below the high-price that triggered the buy-signal of index-moving-high-without-ft signal
								if(signal.getMarketTiming().isBuySwitchOn() && checkPriceBecauseIndexHighWithoutFTSignal != null &&
										signal.getPriceVolumeData().getClose().doubleValue() < checkPriceBecauseIndexHighWithoutFTSignal.doubleValue()) {
																	
									final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.CLOSE_BELOW_PREV_RALLY_HIGH);
									signal.addSignal(sellSignal);
									
									signal.getMarketTiming().setBuySwitchOn(false);
									
									signal.resetSignalCount();
									
									//followThroughBuySignal = null;
									//followThroughUndercutSellSignal = null;
									//lastRallySignal = null;

									downsideReversalSellSignal = null;
									numTradingDaysFromDownsideReversalSell = 0;
									
									prevRallyHigh = currentRallyHigh;
									currentRallyHigh = null;
									checkPriceBecauseIndexHighWithoutFTSignal = null;
								}
								*/
							}

						if(signal.getPriceMAShort() != null) {
							markedNumTradingDaysAbove21dMA = numTradingDaysAbove21dMA;
							
							if (signal.getPriceVolumeData().getLow().doubleValue() > signal.getPriceMAShort().doubleValue()) {
								numTradingDaysAbove21dMA++;
								numTradingDaysBelow21dMA = 0;
							} else {
								numTradingDaysAbove21dMA = 0;
							}
						
							if(signal.getPriceVolumeData().getHigh().doubleValue() < signal.getPriceMAShort().doubleValue()) {
								numTradingDaysBelow21dMA++;
								numTradingDaysAbove21dMA = 0;
							} else {
								numTradingDaysBelow21dMA = 0;
							}
						}

					// buy-signal was found. find other signals
					if(lastRallySignal != null && followThroughBuySignal != null) {						
						if(followThroughUndercutSellSignal == null && followThroughBuySignal.getPriceVolumeData().getLow().doubleValue() > signal.getPriceVolumeData().getClose().doubleValue()) {
							final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.FOLLOWTHROUGH_UNDERCUT);
							signal.addSignal(sellSignal);
							
							followThroughUndercutSellSignal = signal;
						}						
						
						if(lastRallySignal != null && lastRallySignal.getPriceVolumeData().getLow().doubleValue() > signal.getPriceVolumeData().getLow().doubleValue()) {
							final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.RALLY_UNDERCUT);
							signal.addSignal(sellSignal);
							
							signal.getMarketTiming().setBuySwitchOn(false);
							
							signal.resetSignalCount();
							
							followThroughBuySignal = null;
							followThroughUndercutSellSignal = null;
							lastRallySignal = null;

							followThroughBuySignal = null;

							followThroughUndercutSellSignal = null;

							downsideReversalSellSignal = null;
							numTradingDaysFromDownsideReversalSell = 0;
							// DONOT reset the high-since-FT here.. it will be reset later in the code below
							//highSinceFollowThrough = null;
						}
						
							if(highSinceFollowThrough == null && followThroughBuySignal != null) {
								highSinceFollowThrough = signal.getPriceVolumeData();
							} else {
								if(highSinceFollowThrough.getHigh().doubleValue() < signal.getPriceVolumeData().getHigh().doubleValue()) {
									//reset the high since we got a new high since FT
									highSinceFollowThrough = signal.getPriceVolumeData();
								} else if(signal.getPriceMAMedium() != null && signal.getPriceVolumeData().getLow().doubleValue() < signal.getPriceMAMedium().doubleValue() &&
										(highSinceFollowThrough.getHigh().doubleValue() * .90) > signal.getPriceVolumeData().getLow().doubleValue()) {
									//index-low is below 50dma and off-high < 10% then apply circuit breaker rule
									final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.CIRCUIT_BREAKER);
									signal.addSignal(sellSignal);
									
									signal.getMarketTiming().setBuySwitchOn(false);
									
									signal.resetSignalCount();
									
									followThroughBuySignal = null;
									followThroughUndercutSellSignal = null;
									lastRallySignal = null;

									followThroughBuySignal = null;

									followThroughUndercutSellSignal = null;

									downsideReversalSellSignal = null;
									numTradingDaysFromDownsideReversalSell = 0;
									highSinceFollowThrough = null;
								}
							}
						
						// if we negated the last-rally-day.. lets check if today is a rally-day
						if(lastRallySignal == null) {
							prevRallyHigh = currentRallyHigh;
							currentRallyHigh = null;
							checkPriceBecauseIndexHighWithoutFTSignal = null;
							
							highSinceFollowThrough = null;
							final boolean aPotentialRallyDay = isRallyDay(signal, lastTick);
							if (aPotentialRallyDay) {
								lastRallySignal = signal;
								numTradingDaysFromLastRallySignal++;
	
								rallyWasFoundAtleastOnce = true;
								signal.resetSignalCount();
							}
						}
					}
					
					// find MA-signals & other buy/sell signals
						if(lastRallySignal != null) {
							
							// buy-signal if the low moves above 21-d-ma on a flat/up day
							if(lowAbove21dMABuySignal == null && lastTick.getPriceMAShort() != null && signal.getPriceMAShort() != null
							        && signal.getPriceVolumeData().getClose().doubleValue() >= lastTick.getPriceVolumeData().getClose().doubleValue()
									&& signal.getPriceVolumeData().getLow().doubleValue() >= signal.getPriceMAShort().doubleValue()) {
								final BuySignalModel buySignal = new BuySignalModel(BuySignalType.LOW_ABOVE_21D_MA);
								signal.addSignal(buySignal);
								
								numTradingDaysAbove21dMA = 1;
								numTradingDaysBelow21dMA = 0;
								
								lowAbove21dMABuySignal = signal;
								
								//this resets other 21-d sell-signals
								lowBelow21dMASellSignal = null;
								overdueBreakBelow21dMASellSignal = null;
								livingBelow21dMASellSignal = null;
								trendingBelow21dMASellSignal = null;
							}
							
							// buy-signal on an up-day when the low stays above 21-d-ma for 5 or more days 
							if(trendingAbove21dMABuySignal == null && numTradingDaysAbove21dMA >= 5 && signal.getPriceVolumeData().getClose().doubleValue() >= lastTick.getPriceVolumeData().getClose().doubleValue()) {
								final BuySignalModel buySignal = new BuySignalModel(BuySignalType.TRENDING_ABOVE_21D_MA);
								signal.addSignal(buySignal);
								
								trendingAbove21dMABuySignal = signal;
							}
							
							// buy-signal on an up-day when the low stays above 21-d-ma for 10 or more days 
							if(livingAbove21dMABuySignal == null && numTradingDaysAbove21dMA >= 10 && signal.getPriceVolumeData().getClose().doubleValue() >= lastTick.getPriceVolumeData().getClose().doubleValue()) {
								final BuySignalModel buySignal = new BuySignalModel(BuySignalType.LIVING_ABOVE_21D_MA);
								signal.addSignal(buySignal);
								
								livingAbove21dMABuySignal = signal;
							}
							
							// buy-signal on a up/flat day if the low >= 50-d-ma and 50-dma is trending up 
							if(lowAbove50dMABuySignal == null && lastTick.getPriceMAMedium() != null && signal.getPriceMAMedium() != null
									&& signal.getPriceVolumeData().getClose().doubleValue() >= lastTick.getPriceVolumeData().getClose().doubleValue()
									&& signal.getPriceVolumeData().getLow().doubleValue() >= signal.getPriceMAMedium().doubleValue()
									&& signal.getPriceMAMedium().doubleValue() >= lastTick.getPriceMAMedium().doubleValue()) {
								final BuySignalModel buySignal = new BuySignalModel(BuySignalType.LOW_ABOVE_50D_MA);
								signal.addSignal(buySignal);
								
								lowAbove50dMABuySignal = signal;
								
								//this resets other 50-d sell-signals
								lowBelow50dMASellSignal = null;
							}
							
							// sell-signal when the close < 50-dma (exception: closes in upper half AND closes within 1% of 50-dma) 
							if(lowBelow50dMASellSignal == null && signal.getPriceMAMedium() != null && 
									signal.getPriceVolumeData().getClose().doubleValue() < signal.getPriceMAMedium().doubleValue()) {
								// check for exception
								if(signal.getPriceMAMedium().doubleValue() * 0.99 >= signal.getPriceVolumeData().getClose().doubleValue()
										|| PriceChartAnalyzer.getClosingRange(signal.getPriceVolumeData(), lastTick.getPriceVolumeData()) <= 50) {
								
									final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.LOW_BELOW_50D_MA);
									signal.addSignal(sellSignal);
									
									lowBelow50dMASellSignal = signal;
								
									//this resets other 50-d buy-signals
									lowAbove50dMABuySignal = null;
									
									if(powerTrendOn) {
										powerTrendOn = false;
										final PowerTrendSignalModel ptSignal = new PowerTrendSignalModel(powerTrendOn);
										signal.addSignal(ptSignal);
									}
									dayWhen21dMATrendedOver50dMA = null;
								}
							}
							
							// sell-signal on an down-day when the closes >=0.2% below 21-d-ma
							if(lowBelow21dMASellSignal == null && signal.getPriceVolumeData().getClose().doubleValue() < lastTick.getPriceVolumeData().getClose().doubleValue()
									&& signal.getPriceMAShort() != null && signal.getPriceMAShort().doubleValue() * 0.998 >= signal.getPriceVolumeData().getClose().doubleValue() ) {
								
								final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.BREAK_BELOW_21D_MA);
								signal.addSignal(sellSignal);
								
								lowBelow21dMASellSignal = signal;
								
								//this resets other 21-d ma buy-signals
								lowAbove21dMABuySignal = null;
								trendingAbove21dMABuySignal = null;
								livingAbove21dMABuySignal = null;
								
								// overdue sell-signal if num-days-above-21d-ma has been >= 25
								if(overdueBreakBelow21dMASellSignal == null && markedNumTradingDaysAbove21dMA >= 25) {
									final SellSignalModel sellSignal2 = new SellSignalModel(SellSignalType.OVERDUE_BREAK_BELOW_21D_MA);
									signal.addSignal(sellSignal2);
									
									overdueBreakBelow21dMASellSignal = signal;
								}
								
								numTradingDaysAbove21dMA = 0;
							}
							
							// sell-signal when the tick stays below 21-d-ma for 5 or more days 
							if(trendingBelow21dMASellSignal == null && numTradingDaysBelow21dMA >= 5) {
								final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.TRENDING_BELOW_21D_MA);
								signal.addSignal(sellSignal);
								
								trendingBelow21dMASellSignal = signal;
							}
							
							// sell-signal when tick stays below 21-d-ma for 10 or more days 
							if(livingBelow21dMASellSignal == null && numTradingDaysBelow21dMA >= 10) {
								final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.LIVING_BELOW_21D_MA);
								signal.addSignal(sellSignal);
								
								livingBelow21dMASellSignal = signal;
							}
							
							// buy when price closes higher than marked-high
							if(higherHighBuySignal == null && lastPriceMarkedHigh != null && signal.getPriceVolumeData().getClose().doubleValue() > lastPriceMarkedHigh.getHigh().doubleValue()) {
								final BuySignalModel buySignal = new BuySignalModel(BuySignalType.HIGHER_HIGH);
								signal.addSignal(buySignal);
								
								higherHighBuySignal = signal;
							}
							// sell when price closes lower than marked-low
							if(lowerLowSellSignal == null && lastPriceMarkedLow != null && signal.getPriceVolumeData().getClose().doubleValue() < lastPriceMarkedLow.getLow().doubleValue()) {
								final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.LOWER_LOW);
								signal.addSignal(sellSignal);
								
								lowerLowSellSignal = signal;
							}
						//buy on a accumulation-day (accum-day should not coincide with follow-through day)
							if(ftSignal == null) {
								if(highInPast13Weeks) {
									if(isAccumulationDay(signal, lastTick)) {
										final BuySignalModel buySignal = new BuySignalModel(BuySignalType.ACCUMULATION_AT_NEW_HIGH);
										signal.addSignal(buySignal);
									}
								}
							}
							//sell when closes down >=2.5% AND closes in bottom 25% range AND (closes < 50dma OR intraday-high < 21dma)
							if(signal.getPriceMAMedium() != null && signal.getPriceMAShort() != null
									&& lastTick.getPriceVolumeData().getClose().doubleValue() * 0.975 >= signal.getPriceVolumeData().getClose().doubleValue()
									&& PriceChartAnalyzer.getClosingRange(signal.getPriceVolumeData(), lastTick.getPriceVolumeData()) <= 25 
									&& (signal.getPriceMAMedium().doubleValue() > signal.getPriceVolumeData().getClose().doubleValue() || signal.getPriceMAShort().doubleValue() > signal.getPriceVolumeData().getClose().doubleValue())) {
								final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.BAD_BREAK);
								signal.addSignal(sellSignal);
							}
							
							// sell on down-side reversal (new intraday high for 13 weeks, closes in 25% range, closes down, volume > average, spread > 0.75%)
							if(lastTick.getVolMA() != null && highInPast13Weeks &&
							    PriceChartAnalyzer.getClosingRange(signal.getPriceVolumeData(), lastTick.getPriceVolumeData()) <= 25 && lastTick.getPriceVolumeData().getClose().doubleValue() > signal.getPriceVolumeData().getClose().doubleValue() &&
									lastTick.getVolMA().doubleValue() < signal.getPriceVolumeData().getVolume()) {
								final double spread = 100 * (signal.getPriceVolumeData().getHigh().doubleValue() - signal.getPriceVolumeData().getLow().doubleValue()) / signal.getPriceVolumeData().getLow().doubleValue();
								if(spread > 0.75) {
									final SellSignalModel sellSignal = new SellSignalModel(SellSignalType.DOWNSIDE_REVERSAL);
									signal.addSignal(sellSignal);
									
									downsideReversalSellSignal = signal;
								}
							}
							
							// buy if closes > downsideReversalSellSignal.High within 2 days
							if(downsideReversalSellSignal != null) {
								numTradingDaysFromDownsideReversalSell++;
								if(numTradingDaysFromDownsideReversalSell <= 2) {
									if(signal.getPriceVolumeData().getClose().doubleValue() >= downsideReversalSellSignal.getPriceVolumeData().getHigh().doubleValue()) {
										final BuySignalModel buySignal = new BuySignalModel(BuySignalType.DOWNSIDE_REVERSAL_BUYBACK);
										signal.addSignal(buySignal);
										
										downsideReversalSellSignal = null;
									}
								} else {
									downsideReversalSellSignal = null;
								}
							}
						//}
					}
					
				}
			}
			
			if(signal.getMarketTiming().isBuySwitchOn()) {
				final boolean buySwitchTurnedOff = distributionCountHelper.processTick(signal, lastTick);
				if(buySwitchTurnedOff) {
					lastRallySignal = null;
					
					followThroughBuySignal = null;
					
					followThroughUndercutSellSignal = null;
					
					downsideReversalSellSignal = null;
					numTradingDaysFromDownsideReversalSell = 0;
					
					highSinceFollowThrough = null;
					
					prevRallyHigh = currentRallyHigh;
					currentRallyHigh = null;
					checkPriceBecauseIndexHighWithoutFTSignal = null;
					
					// buy-switch is off bcoz of full-distributions. 
					// lets try to find the potential rally-day in the past 3-trading-days (3 days because we need 3 days to confirm a rally by FT-day)
					//7/16/2012 0ndqc case: market-exposure was ZERO because of full-dist-day and we should take 7/12 as a potential-rally-day
					
					Double lowestLowFromCurrentSignal = null; 
					//Integer potentialPastRallyDayIndex = null;
					TimingSignalData potentialPastRallyDay = null;
					int numTradingDaysFromPotentialRallyDay = 0;
					int ii = 0; // start with ZERO so that we can include current-signal
					int iNumTradingDays = 0;
					while(iNumTradingDays<=3){
					  final int index1 = i-ii; 
					  if(index1<1){
					    break;
					  }
					  ii = ii+1;
					  
					  final TimingSignalData pastSignal = signals.get(index1);
					  if(pastSignal.getPriceVolumeData().getDateType() != TradeDateType.TRADING_DATE) {
					    continue;
					  }
					  iNumTradingDays = iNumTradingDays+1;
					  
					  // a rally day has to have the lowest-low in the past 3 days 
					  if(lowestLowFromCurrentSignal == null) {
					    lowestLowFromCurrentSignal = pastSignal.getPriceVolumeData().getLow().doubleValue(); 
					  } else if(lowestLowFromCurrentSignal.doubleValue() < pastSignal.getPriceVolumeData().getLow().doubleValue()) {
					    continue;
					  } else {
					    lowestLowFromCurrentSignal = pastSignal.getPriceVolumeData().getLow().doubleValue();
					  }
					  
					  final boolean aPastRallyDay = isRallyDay(pastSignal, findPreviousTradingDayAtIndex(signals,index1));
					  if(aPastRallyDay) {
					      potentialPastRallyDay = pastSignal;
					      numTradingDaysFromPotentialRallyDay = iNumTradingDays-1;
					  }
					}
					
					if(potentialPastRallyDay != null) {
					  lastRallySignal = potentialPastRallyDay; //signals.get(potentialPastRallyDayIndex.intValue());
                      numTradingDaysFromLastRallySignal = numTradingDaysFromPotentialRallyDay;

                      rallyWasFoundAtleastOnce = true;
                      signal.resetSignalCount();
					}
				}
			}
			if(signal.isPriceMarkedLow()) {
				lastPriceMarkedLow = signal.getPriceVolumeData();
				lowerLowSellSignal = null;
			}
			if(signal.isPriceMarkedHigh()) {
				lastPriceMarkedHigh = signal.getPriceVolumeData();
				higherHighBuySignal = null;
			}
			
			lastTick = signal;
		}
		
		return signals;
	}
	
	private static ITimingSignalData findPreviousTradingDayAtIndex(final List<TimingSignalData> signals, final int index) {
	    int i = index - 1;
	    if(i<0) {
	      return null;
	    }
	    
	    final ITimingSignalData signal = signals.get(i);
	    if(signal.getPriceVolumeData().getDateType() != TradeDateType.TRADING_DATE) {
	      return findPreviousTradingDayAtIndex(signals, i);
	    } else {
	      return signal;
	    }
	}
	
	private static BuySignalModel findFollowThroughSignal(ITimingSignalData signal, ITimingSignalData lastSignal, int numTradingDaysFromLastRallySignal, ITimingSignalData firstFollowThroughSignal) {
		// ignore first 2-3 days from rally-date
		if(numTradingDaysFromLastRallySignal >= 4 && numTradingDaysFromLastRallySignal <= 25) {
			if(signal.getPriceVolumeData().getVolume() > lastSignal.getPriceVolumeData().getVolume()
					&& signal.getPriceVolumeData().getClose().doubleValue() > lastSignal.getPriceVolumeData().getClose().doubleValue()) {
				
				double ftThreshold = getFTThreshold(signal.getPriceVolumeData().getPriceDate());
				double upPercent = ((double) Math.round((10000 * (signal.getPriceVolumeData().getClose().doubleValue() - lastSignal.getPriceVolumeData().getClose().doubleValue()) / lastSignal.getPriceVolumeData().getClose().doubleValue()))) / 100;
				// closes above FT_UP_PERCENT_THRESHOLD and close is above the first-follow-through-day's low
				if(upPercent >= ftThreshold &&
					(firstFollowThroughSignal == null || signal.getPriceVolumeData().getClose().doubleValue() > firstFollowThroughSignal.getPriceVolumeData().getLow().doubleValue())) {					
					// this meets the FT - up-percent threshold criteria.. it should be a FT-day
					final BuySignalType ftSignalType = (firstFollowThroughSignal == null ? BuySignalType.FOLLOW_THROUGH_DAY : BuySignalType.ADDTL_FOLLOW_THROUGH_DAY); 
					return new BuySignalModel(ftSignalType);
				}
			}
		}
		
		return null;
	}
	
	private static boolean isAccumulationDay(ITimingSignalData signal, ITimingSignalData lastSignal) {
			if(signal.getPriceVolumeData().getVolume() > lastSignal.getPriceVolumeData().getVolume()
					&& signal.getPriceVolumeData().getClose().doubleValue() > lastSignal.getPriceVolumeData().getClose().doubleValue()) {
				
				double ftThreshold = getFTThreshold(signal.getPriceVolumeData().getPriceDate());
				double upPercent = 100 * (signal.getPriceVolumeData().getClose().doubleValue() - lastSignal.getPriceVolumeData().getClose().doubleValue()) / lastSignal.getPriceVolumeData().getClose().doubleValue();
				// closes abovce FT_UP_PERCENT_THRESHOLD and close in upper 25% range
				if(upPercent >= ftThreshold &&
				    PriceChartAnalyzer.getClosingRange(signal.getPriceVolumeData(), lastSignal.getPriceVolumeData()) >= 25) {					
					// this meets accumulation-day threshold
					return true;
				}
			}
		
		return false;
	}
	
	private static double getFTThreshold(Date tradeDate) {
		double FT_UP_PCT_THRESHOLD = 1.25;
		if(tradeDate.before(DATE_JAN1_2000)) {
			FT_UP_PCT_THRESHOLD = 1.0;
		}
		
		return FT_UP_PCT_THRESHOLD;
	}
	
	private static boolean isRallyDay(ITimingSignalData signal, ITimingSignalData lastTick) {
		// any day that closes up/flat OR with closing-range in upper-half is a potential rally-day
		final double clsRange = PriceChartAnalyzer.getClosingRange(signal.getPriceVolumeData(), null);
		return (clsRange > 50 || (lastTick != null && signal.getPriceVolumeData().getClose().doubleValue() >= lastTick.getPriceVolumeData().getClose().doubleValue()));
	}
}