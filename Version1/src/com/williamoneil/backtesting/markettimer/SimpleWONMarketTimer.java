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
import org.springframework.beans.factory.annotation.Autowired;

import com.williamoneil.ApplicationException;
import com.williamoneil.backtesting.dao.InstrumentPriceModel;
import com.williamoneil.backtesting.dao.PeriodicityType;
import com.williamoneil.backtesting.dao.SymbolInfoModel;
import com.williamoneil.backtesting.dao.TradeDateType;
import com.williamoneil.backtesting.dao.WONDAOImpl;
import com.williamoneil.backtesting.util.Helpers;
import com.williamoneil.backtesting.util.PriceChartAnalyzer;
import com.williamoneil.backtesting.util.PriceMovingAverageHelper;
import com.williamoneil.backtesting.util.VolumeMovingAverageHelper;

/**
 * @author Gudipati
 *
 */
public class SimpleWONMarketTimer  {
	private static final Log logger = LogFactory.getLog(SimpleWONMarketTimer.class);
	
	@Autowired
	private WONDAOImpl wonDAO = null;

	String[] marketIndices = new String[]{"0NDQC"};//, "0S&P5", "0DJIA"};
	
	private HashMap<String , List<SimpleTimingSignalData>> marketTimingModelsMap = null;
	public void init(Date startDate, Date endDate) throws ApplicationException {
		marketTimingModelsMap = new HashMap<String, List<SimpleTimingSignalData>>();
		
		// get last n-years worth of data
		final Calendar startCal = Calendar.getInstance();
		startCal.setTime(startDate);
		startCal.add(Calendar.DATE, -365); //add 1 years back to the index start-time to accommodate for past signals

		logger.info("Initiating WON-Market-Timer from: " + startCal.getTime() + " till " + endDate);
		
		for(final String aIndex : marketIndices) {
			final List<SimpleTimingSignalData> signals = getMarketTiming(aIndex, startCal.getTime(), endDate);
			
			marketTimingModelsMap.put(aIndex, signals);
		}
	}
	
	public SimpleMarketTimingModel getMarketTiming(Date dt) throws ApplicationException {
		final List<SimpleTimingSignalData> signals = marketTimingModelsMap.get("0NDQC");

		if(signals != null && signals.size() > 0) {
			SimpleTimingSignalData latestSignal = null;
			for(final SimpleTimingSignalData signal : signals) {
				if(signal.getSimpleMarketTiming().getSignalDate().before(dt)) {
					latestSignal = signal;
				} else {
					break;
				}
			}
			
			if(latestSignal != null) {
				return latestSignal.getSimpleMarketTiming();
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

	private List<SimpleTimingSignalData> getMarketTiming(String indexSymbol, Date startDate, Date endDate) throws ApplicationException {
		
		final SymbolInfoModel indexInfo = wonDAO.getSymbolInfoDataForSymbol(indexSymbol);
		if(indexInfo == null) {
			throw new ApplicationException("No index symbol found for: " + indexSymbol);
		}
		
		final List<InstrumentPriceModel> prices = wonDAO.getPriceHistory(indexInfo.getInstrumentId(), startDate, endDate, PeriodicityType.DAILY, false, null);
		
		final List<SimpleTimingSignalData> signals = SimpleWONMarketTimerHelper.getMarketTimings(prices);
		
		final DateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		if(signals != null && signals.size() > 0) {
			for(final SimpleTimingSignalData signal : signals) {
				if(signal.getSimpleMarketTiming() != null ) {
					System.out.println(sdf.format(signal.getPriceVolumeData().getPriceDate()) + " : " + signal.getSimpleMarketTiming().getMarketSignalType());
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


/*
 * simplified version of market timer
 * 
 * 3 stages of market
 * DOWNTREND, UPTREND and CAUTIOUS-UPTREND
 * 
 * Market goes to 0.5% above 50dma - (if 0.5% above 21dma then UPTREND else CAUTIOUS-UPTREND)
 * 
 * Market goes to 0.5% below 50dma - DOWNTREND (neeed to go above 50dma or find a FT day to change trend)
 * Market is below 50dma, but FollowThroughDay happens - then change to CAUTIOUS-UPTREND
 * Market is below 50dma, and goes below the RallyDay lows - then change to DOWNTREND 
 *
 * Rally and FT logic is the same. 
 * 
 */
class SimpleWONMarketTimerHelper {
	
	@SuppressWarnings("deprecation")
	private static final Date DATE_JAN1_2000 = new Date(100,0,1);
	
	@SuppressWarnings("deprecation")
	public static List<SimpleTimingSignalData> getMarketTimings(List<InstrumentPriceModel> _pvList) {
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
		
		final List<SimpleTimingSignalData> signals = new ArrayList<SimpleTimingSignalData>();
		
		//first calculate the signal data with MAs
		BigDecimal lastPriceMA1 = null;
		BigDecimal lastPriceMA2 = null;
		BigDecimal lastVolMA = null;
		for(int i = 0; i < pvList.size(); i++) {
			final InstrumentPriceModel pvData = pvList.get(i);
			if(pvData == null) {
				continue;
			}
			
			final SimpleTimingSignalData signal = new SimpleTimingSignalData(pvData, new SimpleMarketTimingModel(pvData.getPriceDate()));
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
		
		// next we process the signal list and populate the market-signals into it
		SimpleTimingSignalData lastTick = null;
		
		SimpleTimingSignalData lastRallySignal = null;
		int numTradingDaysFromLastRallySignal = 0;

		SimpleTimingSignalData followThroughBuySignal = null;
		
		final DistributionCountHelper distributionCountHelper = new DistributionCountHelper();
		/*
		 * Market goes to 0.5% above 50dma - (if 0.5% above 21dma then UPTREND else CAUTIOUS-UPTREND)
		 * 
		 * Market goes to 0.5% below 50dma - DOWNTREND (neeed to go above 50dma or find a FT day to change trend)
		 * Market is below 50dma, but FollowThroughDay happens - then change to CAUTIOUS-UPTREND
		 * Market is below 50dma, and goes below the RallyDay lows - then change to DOWNTREND 
		 */
		for(int i = 0; i < signals.size(); i++) {
			final SimpleTimingSignalData signal = signals.get(i);
			if(signal == null || signal.getPriceVolumeData() == null || signal.getPriceVolumeData().getDateType() != TradeDateType.TRADING_DATE || signal.getPriceVolumeData().getVolume() == null) {
				continue;
			}
			
			SimpleMarketSignalType lastTickMarketSignal = null;  
			if(lastTick != null) {
				lastTickMarketSignal = lastTick.getSimpleMarketTiming().getMarketSignalType();
				signal.getSimpleMarketTiming().setMarketSignalType(lastTick.getSimpleMarketTiming().getMarketSignalType());
			} else {
				//signal.getSimpleMarketTiming().setMarketSignalType(SimpleMarketSignalType.DOWNTREND);
			}
			
			if(signal.getPriceMAMedium() != null) {
				final boolean isBelow50Dma = Helpers.getPercetChange(signal.getPriceVolumeData().getClose(), signal.getPriceMAMedium()).doubleValue() <= -0.5;
				final boolean isAbove50Dma = Helpers.getPercetChange(signal.getPriceVolumeData().getClose(), signal.getPriceMAMedium()).doubleValue() >= 0.5;
				
				final boolean isBelow21Dma = Helpers.getPercetChange(signal.getPriceVolumeData().getClose(), signal.getPriceMAShort()).doubleValue() <= -0.5;
				final boolean isAbove21Dma = Helpers.getPercetChange(signal.getPriceVolumeData().getClose(), signal.getPriceMAShort()).doubleValue() >= 0.5;
				
				if(isAbove50Dma) {					
					if(isAbove21Dma) {
						if(lastTickMarketSignal != null && lastTickMarketSignal == SimpleMarketSignalType.DOWNTREND) {
							// this prevents the transitioning from downtrend<->uptrend directly
							signal.getSimpleMarketTiming().setMarketSignalType(SimpleMarketSignalType.CAUTIOUS_UPTREND);
						} else {
							signal.getSimpleMarketTiming().setMarketSignalType(SimpleMarketSignalType.UPTREND);
						}
					} else if(isBelow21Dma) {
						signal.getSimpleMarketTiming().setMarketSignalType(SimpleMarketSignalType.CAUTIOUS_UPTREND);
					} else if(signal.getSimpleMarketTiming().getMarketSignalType() == null) {
						signal.getSimpleMarketTiming().setMarketSignalType(SimpleMarketSignalType.CAUTIOUS_UPTREND);
					}
					
					lastRallySignal = null;
					numTradingDaysFromLastRallySignal = 0;
					followThroughBuySignal = null;
				} else if (isBelow50Dma) {
					// we are below 50dma
					if(lastRallySignal == null) {
						signal.getSimpleMarketTiming().setMarketSignalType(SimpleMarketSignalType.DOWNTREND);
						numTradingDaysFromLastRallySignal = 0;
						followThroughBuySignal = null;

						// check if this is a rally day
						
						// since we may not have the FULL price-ticks, lets assume that if the current tick is below the price-marked-low then it could be a potential rally tick
						if(!rallyWasFoundAtleastOnce && !signal.isPriceMarkedLow()) {
							// we havent found a price-marked-low yet.. lets move on
							continue;
						} else {
							// this is the marked low tick or is lower than the marked low tick
							// lets consider this for our rally day criteria
							final boolean aPotentialRallyDay = isRallyDay(signal, lastTick);
							if (aPotentialRallyDay) {
								lastRallySignal = signal;
								numTradingDaysFromLastRallySignal++;

								rallyWasFoundAtleastOnce = true;
							}
						}
					} else {
						numTradingDaysFromLastRallySignal++;
						
						// rally signal is found.. lets try to find FT-day
						// but first if the current-days low under-cuts rally-date then this is not a rally.
						if(signal.getPriceVolumeData().getLow().doubleValue() < lastRallySignal.getPriceVolumeData().getLow().doubleValue()) {
							lastRallySignal = null;
							numTradingDaysFromLastRallySignal = 0;
							signal.getSimpleMarketTiming().setMarketSignalType(SimpleMarketSignalType.DOWNTREND);

							// we negated the last-rally-day.. lets check if today is a rally-day
							final boolean aPotentialRallyDay = isRallyDay(signal, lastTick);
							if (aPotentialRallyDay) {
								lastRallySignal = signal;
								numTradingDaysFromLastRallySignal++;

								rallyWasFoundAtleastOnce = true;
							}
						} else {
							// check for a follow through day 
							final BuySignalModel ftSignal = findFollowThroughSignal(signal, lastTick, numTradingDaysFromLastRallySignal, followThroughBuySignal);
							if(ftSignal != null) {							
								if(followThroughBuySignal == null) {
									// this is  a first follow-through-day
									followThroughBuySignal = signal;

									signal.getSimpleMarketTiming().setMarketSignalType(SimpleMarketSignalType.CAUTIOUS_UPTREND);
									
									//reset distribution count when a follow through day is found
									distributionCountHelper.resetDistributionAndStallDays();
								}
							}
						}
					}
				}
			
				lastTick = signal;
			}
		}
		return signals;
	}
	
	private static BuySignalModel findFollowThroughSignal(SimpleTimingSignalData signal, SimpleTimingSignalData lastSignal, int numTradingDaysFromLastRallySignal, SimpleTimingSignalData firstFollowThroughSignal) {
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

	private static double getFTThreshold(Date tradeDate) {
		double FT_UP_PCT_THRESHOLD = 1.25;
		if(tradeDate.before(DATE_JAN1_2000)) {
			FT_UP_PCT_THRESHOLD = 1.0;
		}
		
		return FT_UP_PCT_THRESHOLD;
	}
	
	private static boolean isRallyDay(SimpleTimingSignalData signal, SimpleTimingSignalData lastTick) {
		// any day that closes up/flat OR with closing-range in upper-half is a potential rally-day
		final double clsRange = PriceChartAnalyzer.getClosingRange(signal.getPriceVolumeData(), null);
		return (clsRange > 50 || (lastTick != null && signal.getPriceVolumeData().getClose().doubleValue() >= lastTick.getPriceVolumeData().getClose().doubleValue()));
	}
}

