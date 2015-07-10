/**
 * 
 */
package com.williamoneil.backtesting.alpha;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.springframework.beans.factory.annotation.Autowired;

import com.williamoneil.ApplicationException;
import com.williamoneil.Constants;
import com.williamoneil.backtesting.AlphaModel;
import com.williamoneil.backtesting.RunLogger;
import com.williamoneil.backtesting.dao.BasePatternModel;
import com.williamoneil.backtesting.dao.BaseStatusType;
import com.williamoneil.backtesting.dao.FundsHoldingsModel;
import com.williamoneil.backtesting.dao.InstrumentPriceModel;
import com.williamoneil.backtesting.dao.PeriodicityType;
import com.williamoneil.backtesting.dao.SymbolFundamentalsInfoModel;
import com.williamoneil.backtesting.dao.SymbolFundamentalsModel;
import com.williamoneil.backtesting.dao.SymbolHeaderInfoModel;
import com.williamoneil.backtesting.dao.SymbolModel;
import com.williamoneil.backtesting.dao.WONDAOImpl;
import com.williamoneil.backtesting.data.SignalData;
import com.williamoneil.backtesting.data.TransactionType;
import com.williamoneil.backtesting.util.ChartAnalysisType;
import com.williamoneil.backtesting.util.ChartElementType;
import com.williamoneil.backtesting.util.Helpers;
import com.williamoneil.backtesting.util.PriceAnalysisData;
import com.williamoneil.backtesting.util.PriceAnalysisElementData;
import com.williamoneil.backtesting.util.PriceChartAnalyzer;

/**
 * @author Gudipati
 *
 */
public class PatternRecAlphaModelImpl implements AlphaModel {
	private static final Log logger = LogFactory.getLog(PatternRecAlphaModelImpl.class);
	
	private Map<Date, Set<String>> watchList = new HashMap<Date, Set<String>>();
	
	@Autowired
	private WONDAOImpl wonDAO = null;
	
	@Override
	public void init() throws ApplicationException {
	}
	@Override
	public void prepareForRun(String runId, String runName, Date startDate, Date endDate) throws ApplicationException {		
	}

	@Override
	public void unProcessedSignals(final List<SignalData> signals, final Date date) throws ApplicationException {
		if(signals == null) {
			return;
		}
		
		// put the signals back into the watchlist for the specific date
		final Set<String> symsBackIntoWatchList = new HashSet<String>();
		for(final SignalData sig : signals) {
			symsBackIntoWatchList.add(sig.getSymbolInfo().getSymbol());
			
			Set<String> symsForRunDate = watchList.get(sig.getPrimarySignalDate());
			if(symsForRunDate == null) {
				symsForRunDate = new HashSet<String>();
				watchList.put(sig.getPrimarySignalDate(), symsForRunDate);
			}
			symsForRunDate.add(sig.getSymbolInfo().getSymbol());
		}

		RunLogger.getRunLogger().logPortfolio(date + " Adding unprocessed symbols to watchlist: " + symsBackIntoWatchList);
	}
	
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.AlphaModel#getSignals(java.util.Date)
	 */
	@Override
	public List<SignalData> getSignals(final Date runDate) throws ApplicationException {
		final List<SignalData> signals = new ArrayList<SignalData>();
		
		// first process watchlist symbols
		final List<SignalData> watchListSignals = processWatchList(runDate);
		if(watchListSignals != null) {
			signals.addAll(watchListSignals);
		}
		
		// next process the bases being broken out
		final List<BasePatternModel> bases = wonDAO.getBreakingOutBasesForDate(runDate);
		if(bases == null || bases.size() == 0) {
			return null;
		}
		
		logger.info("Got " + bases.size() + " bases to process.");
		
		final StringBuffer basesSym = new StringBuffer("");
		final StringBuffer basesApprovedSym = new StringBuffer("");
		for(final BasePatternModel aBase : bases) {
			//RunLogger.getRunLogger().logAlpha(runDate + " - " + aBase.getSymbol() + " - " + aBase.getBaseType() + " - " + aBase.getPivotPriceDt());

			basesSym.append( aBase.getSymbol() + "-" + (aBase.isDaily() ? "D" : "W")+ ",");

			final SignalData aSignal = checkBaseFundamentals(aBase, runDate, false);
			if(aSignal != null) {
				basesApprovedSym.append(aBase.getSymbol() + ",");
				signals.add(aSignal);
			}
		}
		
		RunLogger.getRunLogger().logAlpha(runDate + ": Bases processed : " + basesSym.toString());
		RunLogger.getRunLogger().logAlpha(runDate + ": signals : " + basesApprovedSym.toString());
		
		/*
		 * Comparator to sort by % gains.
		 */
		final Comparator<SignalData> signalStrSorter = new Comparator<SignalData>() {
			@Override
			public int compare(SignalData arg0, SignalData arg1) {
				return arg1.getSignalStrength().compareTo(arg0.getSignalStrength());
			}
		};
	
		Collections.sort(signals, signalStrSorter);
		
		final StringBuffer buffy = new StringBuffer("");
		for(final SignalData signal : signals) {
			buffy.append( signal.getSymbolInfo().getSymbol() + "(" + signal.getSignalStrength() +"), ");
		}
		
		RunLogger.getRunLogger().logAlpha(runDate + ": Signals ordered by strength : " + buffy.toString());
		return signals;
	}
	
	public List<SignalData> processWatchList(final Date runDate) throws ApplicationException {
		final List<SignalData> signals = new ArrayList<SignalData>();
		
		final StringBuffer watchListSymAdded  = new StringBuffer("");
		
		final Set<Date> watchDates = watchList.keySet();
		
		final Set<Date> removeDates = new HashSet<Date>();
		for(final Date watchDate : watchDates) {
			final int daysDiff = Math.abs(Days.daysBetween(new DateTime(watchDate), new DateTime(runDate)).getDays());
			if(daysDiff > 15) {
				// do not process and remove all watch-list symbols more than 7 days
				removeDates.add(watchDate);
				continue;
			}
			
			// process the watchlist
			final Set<String> syms = watchList.get(watchDate);
			final Set<String> removeSyms = new HashSet<String>();
			for(final String symStr : syms) {
				
				final SymbolModel sym = this.getSymbolModel(symStr, runDate);
				if(sym == null) {
					continue;
				}
				
				final SignalData aSignal = checkSymbolFundamentals(sym, runDate);
				if(aSignal != null) {
					aSignal.setPrimarySignalDate(watchDate);
					aSignal.setStrongSignal(true); // all watchlists are strong signals
					final boolean technicalsPassed = performCurrrentDayTechnicals(sym, runDate);
					if(technicalsPassed) {
						signals.add(aSignal);
					
						watchListSymAdded.append(aSignal.getSymbolInfo().getSymbol() + ",");
						removeSyms.add(aSignal.getSymbolInfo().getSymbol());
					} else {
						// didnt pass technical.. but maybe this is still buyable
						
						final BasePatternModel base = this.wonDAO.getBreakingOutBasesForOsidAndDate(aSignal.getSymbolInfo().getInstrumentId(), aSignal.getPrimarySignalDate());
						if(base == null) {
							continue;
						}
						final PriceAnalysisData breakOutDayPA = sym.getPriceAnalysisForDate(base.getPivotDt());
						if(breakOutDayPA == null) {
							continue;
						}
						final PriceAnalysisData pivotPriceDayPA = sym.getPriceAnalysisForDate(base.getPivotPriceDt());
						if(pivotPriceDayPA == null) {
							continue;
						}
						final PriceAnalysisData currentDayPA = sym.getPriceAnalysisForDate(runDate);
						if(currentDayPA == null) {
							continue;
						}
						
						if(breakOutDayPA.getPrice().getLow().doubleValue() > currentDayPA.getPrice().getClose().doubleValue()) {
							// below the breakout day low
							// then this should be in the buy area and above 10ema for us to consider as a buy signal
							if(currentDayPA.getElementPctChg(ChartElementType.PRICE_MA_10).doubleValue() < 0) {
								continue;
							}
						}
						
							// still above the breakout day low.. good thing.. just need to check if it is still in buy-range.
							if(pivotPriceDayPA.getPrice().getHigh().multiply(new BigDecimal(1.05)).doubleValue() < currentDayPA.getPrice().getClose().doubleValue()) {
								continue;
							}
							signals.add(aSignal);
							
							watchListSymAdded.append(aSignal.getSymbolInfo().getSymbol() + ",");
							removeSyms.add(aSignal.getSymbolInfo().getSymbol());
					}

				}
			}
			// remove bases that are added to signal to prevent us from buying them again
			syms.removeAll(removeSyms);
			
		}
		
		for(final Date removeDate : removeDates) {
			watchList.remove(removeDate);
		}
		
		RunLogger.getRunLogger().logAlpha( runDate + " : Watchlists added : " + watchListSymAdded);
		
		return signals;
	}
	
	private boolean performCurrrentDayTechnicals(final SymbolModel sym, final Date runDate) throws ApplicationException {
		final PriceAnalysisData currentDayPA = sym.getPriceAnalysisForDate(runDate);
		if(currentDayPA == null) {
			return false;
		}
		
		// check to make sure it has support at 10/20ema or breaking up from there
		final PriceAnalysisElementData pae0= currentDayPA.getAnalysisFor(ChartElementType.PRICE_MA_10);
		final PriceAnalysisElementData pae1 = currentDayPA.getAnalysisFor(ChartElementType.PRICE_MA_20); 
		if(pae0 != null) {
			ChartAnalysisType cat = pae0.getAnalysisType();
				if(cat == ChartAnalysisType.BREAKING_UP || cat == ChartAnalysisType.SUPPORT) {
					return true;
				}
		} else  if(pae1 != null) {
			ChartAnalysisType cat = pae1.getAnalysisType();
				if(cat == ChartAnalysisType.BREAKING_UP) {
					if(Helpers.getPercetChange(currentDayPA.getPrice().getClose(), currentDayPA.getPrice20dEma()).doubleValue() <= 1.04) {
						return true;
					}
				} else if(cat == ChartAnalysisType.SUPPORT) {
					return true;
				}
		}
		
		return false;
	}
	
	private SignalData checkSymbolFundamentals(final SymbolModel sym, final Date runDate) throws ApplicationException {
		if(sym == null) {
			return null;
		}
		
		final SymbolHeaderInfoModel symHeader = sym.getHeaderInfo();
		if(symHeader == null) {
			return null;
		}
		
		if(sym.isMergerEvent()) {
			return null;
		}
		if(symHeader.isFfo() || symHeader.isNav() || (symHeader.getIndustryName() != null && (symHeader.getIndustryName().contains("REIT") || symHeader.getIndustryName().contains("ETF")  || symHeader.getIndustryName().contains("ETN")))) {
			//logger.info("Ignoring REIT/ETFs signal - " + symHeader.getSymbol());
			return null;
		}
		
		if(sym.getSymInfo().getSymbol().equalsIgnoreCase("vwdry")) {
			System.err.print("");
		}
		if(sym.getSymInfo().getExchange().contains("OTC")) {
			return null;
		}
		
		final boolean isRecentIpo = sym.isRecentIPO(runDate);
		
		// first lets check its liquidity and price min thresholds

					if(symHeader.getCurrPrice() <= 7) {
						//logger.info("Ignoring less than $5 signal - " + symHeader.getSymbol() + " @" + symHeader.getCoName());
						return null;
					}
					
					if(symHeader.getMktCap() == null || symHeader.getMktCap().doubleValue() < 750000000) { //750M
						//logger.info("Ignoring small-cap signal - " + symHeader.getSymbol() + " - " + symHeader.getMktCap());
						return null;
					}
					if(symHeader.getAvgVol() == null ||symHeader.getAvgVol() <= 100000 || symHeader.getAvgVol() * symHeader.getCurrPrice() < 1000000) {
						//logger.info("Ignoring low avg-daily-dollar-vol signal - " + symHeader.getSymbol() + " - " + symHeader.getAvgVol() * symHeader.getCurrPrice());
						if(!isRecentIpo && symHeader.getAvgVol() != null) {
							return null;
						}
					}
					if(symHeader.getRsRank() == null || symHeader.getRsRank() < 70) {
						//logger.info("Ignoring low RS-Rating signal - " + symHeader.getSymbol() + " - " + symHeader.getRsRank());
						return null;
					}

		final SymbolFundamentalsModel fundies = this.wonDAO.getSymbolFundamentals(sym.getSymInfo().getMsId(), runDate);
		if(fundies == null) {
			return null;
		}
		/*
		if(fundies.getInfo().getRsRank()!= null && fundies.getInfo().getRsRank() < 70) {
			return null;
		}
		*/
		if(fundies.getInfo().getDgRating() != null && fundies.getInfo().getDgRating() < 60) {
			return null;
		}
		
		/*
		if(isRecentIpo) {
			if(fundies.getInfo().getCompRating() != null && fundies.getInfo().getCompRating() < 50) {
				return null;
			}
		} else if(fundies.getInfo().getCompRating() != null && fundies.getInfo().getCompRating() < 60) {
			return null;
		}
		*/
		
		/*
		// EPS rank must be > 70 unless it is a recent IPO
		if(sym.getHeaderInfo().getIpoDt() != null  ) {
			final int monthsDiff = Math.abs(Months.monthsBetween(new DateTime(sym.getHeaderInfo().getIpoDt()), new DateTime(runDate)).getMonths());
			if(monthsDiff > 48 && fundies.getInfo().getEpsRank() != null && fundies.getInfo().getEpsRank() < 70) { 
				// 4 years after IPO means it should have good EPS rank
				return null;			
			}
		}
		*/
		final FundamentalScore score = new FundamentalScore(sym, fundies);
		
		final SignalData aSignal = new SignalData();
		aSignal.setSignalDate(runDate);
		aSignal.setSymbolInfo(sym.getSymInfo());
		aSignal.setTransactionType(TransactionType.BUY);
		aSignal.setSignalStrength(score.getFinalScore().floatValue());
		//aSignal.setSignalInfos(signalInfos);
		
		return aSignal;
	}
	
	private SymbolModel getSymbolModel(final String symbol, final Date runDate) throws ApplicationException {
		final Calendar startCal = Calendar.getInstance();
		startCal.setTime(runDate);
		startCal.add(Calendar.DATE, -1 * Constants.DAYS_BACK);
		
		final Date startDate = startCal.getTime();
		
		// get everything about that signal for that date
		final SymbolModel sym = this.getWonDAO().getSymbolData(symbol, startDate, runDate, PeriodicityType.DAILY);
		if(sym == null) {
			return null;
		}
		
		final SymbolHeaderInfoModel symHeader = sym.getHeaderInfo();
		if(symHeader == null) {
			return null;
		}
		
		return sym;
	}
	
	private SignalData checkBaseFundamentals(final BasePatternModel aBase, final Date runDate, final boolean isWatchList) throws ApplicationException {
		if(aBase == null) {
			return null;
		}
		
		if(aBase.getSymbol().equalsIgnoreCase("amba")){
			System.out.print("");
		}
		
		try {
			if(!aBase.isDaily()) {
				//TODO: need to process weekly bases too in future
				return null;
			}
			
			if(aBase.getStatusType() == BaseStatusType.FAILED || aBase.getStatusType() == BaseStatusType.MAXOUT) {
				//ignore failed bases
				return null;
			}
			
			int baseNum = 0;
			final String baseStg = aBase.getBaseStage();
			if(baseStg != null && baseStg.length() >= 1 ) {
				baseNum = Integer.parseInt(String.valueOf(baseStg.charAt(0)));
				if(baseNum > 5) {
					//logger.info("Ignoring late stage bases- " + symHeader.getSymbol() + " "  + aBase.getBaseStage() + ":" + aBase.getBaseNum());
					return null;
				}
			}
			
			// get everything about that signal for that date
			final SymbolModel sym = getSymbolModel(aBase.getSymbol(), runDate);
			if(sym == null) {
				return null;
			}
			
			final SymbolHeaderInfoModel symHeader = sym.getHeaderInfo();

			if(sym.isMergerEvent()) {
				return null;
			}
			if(symHeader.isFfo() || symHeader.isNav() || (symHeader.getIndustryName() != null && (symHeader.getIndustryName().contains("REIT") || symHeader.getIndustryName().contains("ETF")  || symHeader.getIndustryName().contains("ETN")))) {
				//logger.info("Ignoring REIT/ETFs signal - " + symHeader.getSymbol());
				return null;
			}
			
			// analyze the prices
			final List<PriceAnalysisData> paList = sym.getPriceAnalysisData();
			if(paList == null || paList.size() == 0 || paList.size() <= 3) {
				return null;
			}
			
			final boolean isRecentIpo = sym.isRecentIPO(runDate);
			
			boolean goodSignal = false;
			boolean strongSignal = false;
			boolean weakSignal = false;
			
			final PriceAnalysisData currentDayPA = sym.getPriceAnalysisForDate(runDate);
			final PriceAnalysisData breakOutDayPA = sym.getPriceAnalysisForDate(aBase.getPivotDt());
			final PriceAnalysisData pivotPriceDayPA = sym.getPriceAnalysisForDate(aBase.getPivotPriceDt());
			if(currentDayPA == null || breakOutDayPA == null || pivotPriceDayPA == null) {
				return null;
			}
			
			final BigDecimal volPctChgOnBreakOutDay = Helpers.getPercetChange(new BigDecimal(breakOutDayPA.getPrice().getVolume()), breakOutDayPA.getVol50dSma());
				if(volPctChgOnBreakOutDay != null  && volPctChgOnBreakOutDay.doubleValue() >= 150) {
					//signalDescList.add("Vol-Rate on breakout is: " + volPctChgOnBreakOutDay);
					goodSignal = true;
					strongSignal = true;
//					RunLogger.getRunLogger().logAlpha(aBase.getPivotDt() + " " + aBase.getSymbol() + " Good Vol rate is: " + volPctChgOnBreakOutDay.doubleValue());
				} else if(volPctChgOnBreakOutDay != null  && volPctChgOnBreakOutDay.doubleValue() >= 100) {
					//signalDescList.add("Vol-Rate on breakout is: " + volPctChgOnBreakOutDay);
					goodSignal = true;
					weakSignal = true;
//					RunLogger.getRunLogger().logAlpha(aBase.getPivotDt() + " " + aBase.getSymbol() + " Good Vol rate is: " + volPctChgOnBreakOutDay.doubleValue());
				}  else {
					if(isRecentIpo && volPctChgOnBreakOutDay == null) {
						goodSignal = true;
					} else {
						//RunLogger.getRunLogger().logAlpha(aBase.getPivotDt() + " " + aBase.getSymbol() + " Bad Vol rate is: " + volPctChgOnBreakOutDay.doubleValue());
						return null;
					}
				}
			
				
				boolean gapup = false;
				boolean newHigh = false;
				boolean newRecentHigh = false;
				
				final List<PriceAnalysisElementData> paes= breakOutDayPA.getPriceAnalysisElements();
				if(paes != null && paes.size() > 0) {
					for(final PriceAnalysisElementData pae : paes) {
						if(pae.getElementType() == ChartElementType.PRICE && pae.getAnalysisType() == ChartAnalysisType.GAP_UP) {
							gapup = true;
						} else if(pae.getElementType() == ChartElementType.PRICE && pae.getAnalysisType() == ChartAnalysisType.NEW_HIGHS) {
							newHigh = true;
						} else if(pae.getElementType() == ChartElementType.PRICE && pae.getAnalysisType() == ChartAnalysisType.NEW_RECENT_HIGHS) {
							newRecentHigh = true;
						}
					}
				}
				
				// we need the breakout to be at a new-high/recent new highs
				if(newHigh) {
					goodSignal = true;
					if(!weakSignal) {
						strongSignal = true;
					}
			  } else if(newRecentHigh) {
					if(gapup) {
						goodSignal = true;
						weakSignal = true;
					} else {
						// must be gap-up if its a recent-new-high
						return null;
					}
				} else if(!isRecentIpo) {
					// not a new-high or recent-new-high.. we reject this if it isnt a recent ipo (coz ipos seem to have bug with recent-high/new-high logic)
					return null;
				}
				
				final InstrumentPriceModel priceBeforeBreakOut = sym.getPriceForNDaysBeforeDate(1, breakOutDayPA.getPrice().getPriceDate());
				final double breakOutDayPctChg = Helpers.getPercetChange(breakOutDayPA.getPrice().getClose(), priceBeforeBreakOut.getClose()).doubleValue();
				if(breakOutDayPctChg <= 5.5) {
					return null;
				}
				
				if(pivotPriceDayPA.getPrice().getHigh().doubleValue() + 0.10 >= breakOutDayPA.getPrice().getClose().doubleValue()) {
					//logger.info("Ignoring because it closed below the pivot-price - " + symHeader.getSymbol() + " - " + breakOutDayPA.getPrice().getClose().doubleValue());
					return null;
				}
				
					//PriceAnalysisData dayPriorToBreakOutDayPA = paList.get(paList.size()-2);
					//final double closingRange = PriceChartAnalyzer.getClosingRange(breakOutDayPA.getPrice(), dayPriorToBreakOutDayPA); 
					final double closingRange = PriceChartAnalyzer.getClosingRange(breakOutDayPA.getPrice(), null);
					if(closingRange < 40 && goodSignal && strongSignal) {
						logger.info("Adding to WatchList because its close was weak " + symHeader.getSymbol() + " - closing range: " + closingRange);
						
						Set<String> symsForRunDate = watchList.get(runDate);
						if(symsForRunDate == null) {
							symsForRunDate = new HashSet<String>();
							watchList.put(runDate, symsForRunDate);
						}
						symsForRunDate.add(aBase.getSymbol());
						return null;
					} 
					
					// check if it is beyond buy-price (if so -add it to watchlist)
					if(goodSignal && pivotPriceDayPA.getPrice().getHigh().multiply(new BigDecimal(1.05)).doubleValue() < currentDayPA.getPrice().getClose().doubleValue()) {
						logger.info("Adding to WatchList because it is currently beyond buy-range - " + symHeader.getSymbol() + " - " + breakOutDayPA.getPrice().getClose().doubleValue());
						
						Set<String> symsForRunDate = watchList.get(runDate);
						if(symsForRunDate == null) {
							symsForRunDate = new HashSet<String>();
							watchList.put(runDate, symsForRunDate);
						}
						symsForRunDate.add(aBase.getSymbol());
						
						return null;
					}
							
			if(goodSignal) {
				final SignalData signal = this.checkSymbolFundamentals(sym, runDate);
				if(signal != null) {
					signal.setStrongSignal(strongSignal);
					signal.setPrimarySignalDate(aBase.getPivotDt());
				}
				return signal;
			}
			
		}catch(ApplicationException aex) {
			//aex.printStackTrace();
			throw aex;
		} catch(Exception ex) {
			ex.printStackTrace();
			throw new ApplicationException(ex);
		}
		
		return null;
	}
	

	/**
	 * @return the wonDAO
	 */
	public WONDAOImpl getWonDAO() {
		return wonDAO;
	}

	/**
	 * @param wonDAO the wonDAO to set
	 */
	public void setWonDAO(WONDAOImpl wonDAO) {
		this.wonDAO = wonDAO;
	}
	
}

class FundamentalScore implements Comparable<FundamentalScore> {

	
	private SymbolModel sym = null;

	private int ratingsScore = 0;
	private int ownersScore = 0;
	private int ipoScore = 0;
	private int groupScore = 0;
	
	public FundamentalScore(SymbolModel sym, SymbolFundamentalsModel fundies) {
		this.sym = sym;

		// RATINGS-SCORE
		ratingsScore = 0;
		final SymbolFundamentalsInfoModel info = fundies.getInfo();
		if(info != null) {
			if(info.getEpsRank() != null && info.getEpsRank() >= 90) {
				ratingsScore+= 20;
			} else if (info.getEpsRank() != null && info.getEpsRank() >= 80) {
				ratingsScore+= 15;
			}else if (info.getEpsRank() != null && info.getEpsRank() >= 70) {
				ratingsScore+= 10;
			}
			
			if(info.getCompRating() != null && info.getCompRating() >= 90) {
				ratingsScore+= 20;
			}else if(info.getCompRating() != null && info.getCompRating() >= 80) {
				ratingsScore+= 15;
			}else if(info.getCompRating() != null && info.getCompRating() >= 70) {
				ratingsScore+= 10;
			}
			
			if(info.getDgRating() != null && info.getDgRating() >= 90) {
				ratingsScore+= 35;
			} else if(info.getDgRating() != null && info.getDgRating() >= 80) {
				ratingsScore+= 30;
			} else if(info.getDgRating() != null && info.getDgRating() >= 70) {
				ratingsScore+= 25;
			}
			
			if(info.getAdRating() != null && info.getAdRating() >= 45) {
				ratingsScore+= 15;
			} else if(info.getAdRating() != null && info.getAdRating() >= 25) {
				ratingsScore+= 10;
			} else if(info.getAdRating() != null && info.getAdRating() >= 15) {
				ratingsScore+= 5;
			}
			
			if(info.getSmrRating() != null && info.getSmrRating().equalsIgnoreCase("A")) {
				ratingsScore+= 15;
			} else if(info.getSmrRating() != null && info.getSmrRating().equalsIgnoreCase("B")) {
				ratingsScore+= 10;
			}
			
			if(info.getRsRank() != null && info.getRsRank() >= 90) {
				ratingsScore+= 25;
			}else if(info.getRsRank() != null && info.getRsRank() >= 80) {
				ratingsScore+= 20;
			}else if(info.getRsRank() != null && info.getRsRank() >= 70) {
				ratingsScore+= 15;
			}
		}

		//GROUP SCORE
		groupScore = 0;
		if(info != null) {
			if(info.getGroupRank() != null && info.getGroupRank() <= 5) {
				groupScore =  25;
			} else if(info.getGroupRank() != null && info.getGroupRank() <= 10) {
				groupScore =  20;
			} else if(info.getGroupRank() != null && info.getGroupRank() <= 15) {
				groupScore =  15;
			}
		}
		
		//IPO SCORE
		ipoScore = 0;
		if(sym != null && sym.getHeaderInfo().getIpoDt() != null) {
			final int monthsDiff = Math.abs(Months.monthsBetween(new DateTime(sym.getHeaderInfo().getIpoDt()), new DateTime(sym.getAsOfDate())).getMonths());
			if(monthsDiff <= 3) {
				ipoScore = 140;
			} else if(monthsDiff <= 6) {
				ipoScore = 120;
			} else if(monthsDiff <= 12) {
				ipoScore = 100;
			} else if (monthsDiff <= 24) {
				ipoScore = 90;
			} else if (monthsDiff <= 36) {
				ipoScore = 80;
			} else if (monthsDiff <= 48) {
				ipoScore = 70;
			} else if (monthsDiff <= 60) {
				ipoScore = 50;
			} else if (monthsDiff <= 72) {
				ipoScore = 30;
			} else if (monthsDiff <= 84) {
				ipoScore = 10;
			} else if (monthsDiff <= 96) {
				ipoScore = 5;
			}
		}
	
		// OWNERSHIP SCORE
		ownersScore = 0;
		final List<FundsHoldingsModel> hlds = fundies.getFundsHoldings();
		if(hlds != null && hlds.size() > 0) {
			
			Integer oneFundHld = null;
			Integer twoFundHld = null;
			Integer threeFundHld = null;
			
			Long oneSharesHld = null;
			Long twoSharesHld = null;
			//Long threeSharesHld = null;
			
			// check if ownership is growing
			int index = 1;
			for(final FundsHoldingsModel aHld : hlds) {
				if(aHld.getNumOfFunds() != null) {
					if(index == 1) {
						oneFundHld = aHld.getNumOfFunds();
						
						if(aHld.getNumOfShares() != null) {
							oneSharesHld = aHld.getNumOfShares();
						}
					} else if (index == 2) {
						twoFundHld = aHld.getNumOfFunds();
						
						if(aHld.getNumOfShares() != null) {
							twoSharesHld = aHld.getNumOfShares();
						}
					} else if (index == 3) {
						threeFundHld = aHld.getNumOfFunds();
						
						/*
						if(aHld.getNumOfShares() != null) {
							threeSharesHld = aHld.getNumOfShares();
						}
						*/
					} else {
						break;
					}
				}
				index++;
			}
			
			if(oneFundHld != null && twoFundHld != null) {
				if(oneFundHld > twoFundHld) {
					ownersScore+= 15;
				}
			}
			if(twoFundHld != null && threeFundHld != null) {
				if(twoFundHld > threeFundHld) {
					ownersScore+= 5;
				}
			}
			if(oneSharesHld != null && twoSharesHld != null) {
				if(oneSharesHld > twoSharesHld) {
					ownersScore+= 5;
				}
			}
		}
	}
	
	
	public Integer getFinalScore(){
		return ownersScore + ratingsScore + ipoScore + groupScore;
	}
	
	@Override
	public int compareTo(FundamentalScore arg0) {
		return this.getFinalScore().compareTo(arg0.getFinalScore());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj != null & obj instanceof  FundamentalScore) {
			return ((FundamentalScore) obj).sym.getSymInfo().getMsId() == this.sym.getSymInfo().getMsId();
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.sym.getSymInfo().getMsId();
	}
}
