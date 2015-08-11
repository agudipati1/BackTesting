/**
 * 
 */
package com.williamoneil.backtesting.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.springframework.beans.factory.annotation.Autowired;

import com.williamoneil.ApplicationException;
import com.williamoneil.backtesting.AlphaModel;
import com.williamoneil.backtesting.ExecutionModel;
import com.williamoneil.backtesting.PortfolioModel;
import com.williamoneil.backtesting.RunLogger;
import com.williamoneil.backtesting.dao.InstrumentPriceModel;
import com.williamoneil.backtesting.dao.PeriodicityType;
import com.williamoneil.backtesting.dao.SymbolModel;
import com.williamoneil.backtesting.dao.TradeDateType;
import com.williamoneil.backtesting.dao.WONDAOImpl;
import com.williamoneil.backtesting.data.CurrencyData;
import com.williamoneil.backtesting.data.PortfolioData;
import com.williamoneil.backtesting.data.PositionData;
import com.williamoneil.backtesting.data.SignalData;
import com.williamoneil.backtesting.data.TransactionData;
import com.williamoneil.backtesting.data.TransactionType;
import com.williamoneil.backtesting.markettimer.SimpleMarketSignalType;
import com.williamoneil.backtesting.markettimer.SimpleMarketTimingModel;
import com.williamoneil.backtesting.markettimer.SimpleWONMarketTimer;
import com.williamoneil.backtesting.markettimer.WONMarketTimer;
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
public class CanSlimPortfolioModelImpl implements PortfolioModel {

	private static final Log logger = LogFactory.getLog(CanSlimPortfolioModelImpl.class);
	
	@Autowired
	private WONDAOImpl wonDAO = null;
	@Autowired
	private WONMarketTimer marketTimer = null;
	
	@Autowired
	private SimpleWONMarketTimer simpleMarketTimer = null;
	
	private int numOfPositions = 5;
	private long initialCash = 100000;
	private float marginPctAllowed = 50; 
	
	//includes margin assumptions
	private float maxPercentInvestedInUptrend = 100 + marginPctAllowed;
	private float maxPercentInvestedInCautiousUptrend = 100;
	private float maxPercentInvestedInDowntrend = 75;
	
	private float stopLossPct_HardStop = -7f;
	private float stopLossPct_SoftStop = -5f;
	
	private ExecutionModel execModel = null;
	private AlphaModel alphaModel = null;
	
	private PortfolioData portfolio = null;
	
	@Override
	public void init(AlphaModel aModel, ExecutionModel eModel) throws ApplicationException {
		this.alphaModel = aModel;
		this.execModel = eModel;
		
		this.portfolio = new PortfolioData();
		
		final PositionData cashPosition = PositionData.createCashPosition(new BigDecimal(initialCash));
		portfolio.setCashPosition(cashPosition);
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.PortfolioModel#prepareForRun(java.lang.String, java.lang.String, java.util.Date, java.util.Date)
	 */
	@Override
	public void prepareForRun(String runId, String runName, Date startDate, Date endDate) throws ApplicationException {

		simpleMarketTimer.init(startDate, endDate);
		
		//marketTimer.init(startDate, endDate);
		
		RunLogger.getRunLogger().logPortfolio("Start Date: " + startDate);
		RunLogger.getRunLogger().logPortfolio("End Date: " + endDate);
		RunLogger.getRunLogger().logPortfolio("Initial Cash: " + portfolio.getCashPosition().getCurrentValue().getValue().doubleValue());
		RunLogger.getRunLogger().logPortfolio("Num Positions: " + numOfPositions);
		RunLogger.getRunLogger().logPortfolio("-----------------");
		
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.PortfolioModel#getPortfolioData()
	 */
	@Override
	public PortfolioData getPortfolioData() throws ApplicationException {
		return portfolio;
	}

	@Override
	public List<TransactionData> performPortfolioCheck(Date runDate) throws ApplicationException {
		final List<TransactionData> execs = new ArrayList<TransactionData>();
		
		// Next find market timer for the run-date to find the desired market exposure percentage
		final SimpleMarketTimingModel marketTimeSignal = simpleMarketTimer.getMarketTiming(runDate);
		if(marketTimeSignal == null || marketTimeSignal.getMarketSignalType() == null) {
			return execs;
		}
		

		final Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, Calendar.APRIL);
		cal.set(Calendar.DAY_OF_MONTH, 4);
		cal.set(Calendar.YEAR, 2013);
		if(cal.before(runDate)) {
			System.out.print("");
		}
		
		/// first update portfolio  positions with latest prices
		final HashMap<Long, SymbolModel> symbolMap = updatePortfolioPositionsWithLatestPrices(runDate);
		
		// perform any sells because of stop-loss
		final List<TransactionData> stopLossSellTxs = this.checkAndPerformSellStopLossForPositions(runDate, symbolMap, marketTimeSignal.getMarketSignalType());
		if(stopLossSellTxs != null) {
			execs.addAll(stopLossSellTxs);
		}
		
		logger.info(runDate + " MarketSignal: Buy=" + marketTimeSignal.getMarketSignalType() + "; MarketSignalDate=" + marketTimeSignal.getSignalDate());
			//act on market-time-signal
			final List<TransactionData> mtSellTxs = this.actOnMarketTimeSignal(runDate, symbolMap, marketTimeSignal.getMarketSignalType());
			if(mtSellTxs != null) {
				execs.addAll(mtSellTxs);
			}
		
			// perform any sells because of Technical Analysis
			final List<TransactionData> taSellTxs = this.performSellTechAnalysisForPositions(runDate, symbolMap);
			if(taSellTxs != null) {
				execs.addAll(taSellTxs);
			}
			
			// perform secondary buys to positions only if market is not in downtrend
			final List<TransactionData> posSecondaryBuySellTxs = performSecondaryBuySellChecks(runDate, symbolMap, marketTimeSignal.getMarketSignalType());
			if(posSecondaryBuySellTxs != null) {
				execs.addAll(posSecondaryBuySellTxs);
			}
		
			// if we are over-invested then reset the num-of-positions-open-to-buy
			
			int signalStrengthForSwap = 0;
			float maxPctInvested = 0;
			if(marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.DOWNTREND) {
				maxPctInvested = this.maxPercentInvestedInDowntrend;
				signalStrengthForSwap = 0;
			} else if(marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.CAUTIOUS_UPTREND) {
				maxPctInvested = this.maxPercentInvestedInCautiousUptrend;
				signalStrengthForSwap = 0;
			} else if(marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.UPTREND) {
				maxPctInvested = this.maxPercentInvestedInUptrend;
			}
			
		int numOfPositionsOpenToBuy = 0;
		
		final BigDecimal maxAmtPerPosition = this.getMaxBuyAmountAllowedForPosition();
		BigDecimal cashBal = BigDecimal.ZERO;
		//int minNumOfPositionsOpenToBuy = 0;
		
		
		final double pctInvestedCurrent = this.getPortfolioData().getPercentInvested();
		if(pctInvestedCurrent >= maxPctInvested) {
			numOfPositionsOpenToBuy = 0;
			//minNumOfPositionsOpenToBuy = 0;
		} else {
			if(this.marginPctAllowed > 0) {
				final BigDecimal currPortInvestedValue = this.getPortfolioData().getTotalPortfolioValue().getValue();
				cashBal = currPortInvestedValue.multiply(new BigDecimal((maxPctInvested - pctInvestedCurrent) / 100));
			} else {
				cashBal = this.getPortfolioData().getCashPosition().getCurrentValue().getValue();
			}
		}
		numOfPositionsOpenToBuy = (int) Math.round(cashBal.doubleValue() / maxAmtPerPosition.doubleValue());
		
		final List <SignalData> unProcessedSignals = new ArrayList<SignalData>();
		boolean cannotProcessAnyMoreSignals = false;
		
		int newPositionsBought = 0;
			final List<SignalData> signals = alphaModel.getSignals(runDate);
			if(signals != null) {
				final StringBuffer syms = new StringBuffer("");
				
				for(final SignalData aSignal : signals) {
					if(aSignal == null) {
						continue;
					}
					syms.append(aSignal.getSymbolInfo().getSymbol() + ",");
				}		
				logger.info(runDate + " Got #" + signals.size()  + " signals: " + syms.toString());
		
				for(final SignalData aSignal : signals) {
					if(cannotProcessAnyMoreSignals) {
						unProcessedSignals.add(aSignal);
						continue;
					}
					
					PositionData aPos = portfolio.getPositionForMsId(aSignal.getSymbolInfo().getMsId());
					if(aPos != null) {
						logger.info("Got signal for existing positon : " + aSignal.getSymbolInfo().getSymbol());
						continue;
					}
					aPos = portfolio.getPositionForCusipMatch(aSignal.getSymbolInfo().getCusip());
					if(aPos != null) {
						logger.info("Got signal for existing positon using CUSIP : " + aSignal.getSymbolInfo().getSymbol());
						continue;
					}
					
					if(newPositionsBought == numOfPositionsOpenToBuy || newPositionsBought == 2) {
						if(signalStrengthForSwap > aSignal.getSignalStrength()) {
							RunLogger.getRunLogger().logPortfolio(runDate + " max new positions (" + newPositionsBought + ") reached. No swapping [" + aSignal.getSymbolInfo().getSymbol() + "]  with weak-signal-strength: " + aSignal.getSignalStrength());
							unProcessedSignals.add(aSignal);
							continue;
						}

						RunLogger.getRunLogger().logPortfolio(runDate + " max new positions (" + newPositionsBought + ") reached. Checking for swap-outs for " + aSignal.getSymbolInfo().getSymbol() + "[" + aSignal.getSignalStrength() + "]");
						final List<TransactionData> swapOutTxs = swapOutUnderPerformingPositionForSignal(runDate, aSignal, symbolMap);
						if(swapOutTxs == null || swapOutTxs.size() == 0) {
							RunLogger.getRunLogger().logPortfolio(runDate + " nothing left to swap out for " + aSignal.getSymbolInfo().getSymbol() + "[" + aSignal.getSignalStrength() + "]");
							cannotProcessAnyMoreSignals = true;
							unProcessedSignals.add(aSignal);
							continue;
						} else {
							execs.addAll(swapOutTxs);
							continue;
						}
					} else {
						final CurrencyData maxCashAvailForExecution = getMaxBuyAmountAllowedForExecution(aPos);
						if(maxCashAvailForExecution != null && maxCashAvailForExecution.getValue().compareTo(BigDecimal.ZERO) > 0) {
							final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aSignal.getSymbolInfo().getMsId(), aSignal.getSymbolInfo().getSymbol(), maxCashAvailForExecution, runDate);
							execs.add(trans);
					
							newPositionsBought++;
						} else {
							RunLogger.getRunLogger().logPortfolio(runDate + " cannot buy any-more positons for signal: " + aSignal.getSymbolInfo().getSymbol());
						}
					}
				}
				
			}
			
			if(unProcessedSignals.size() > 0) {
				this.alphaModel.unProcessedSignals(unProcessedSignals, runDate);
			}
		
			RunLogger.getRunLogger().logPortfolio(runDate + " net=" + getPortfolioData().getTotalPortfolioValue().getValue().setScale(2, RoundingMode.FLOOR).doubleValue()  + "; cash=" + getPortfolioData().getCashPosition().getCurrentValue().getValue().doubleValue() + "; %inv=" + getPortfolioData().getPercentInvested() + "%; mkt=" + marketTimeSignal.getMarketSignalType() + "; pos=" + getPortfolioData().getCurrentNumOfPosition() + ": "+  this.portfolio.getPositionsAndPctChg());

		return execs;
	}

	private List<TransactionData> swapOutUnderPerformingPositionForSignal(final Date runDate, final SignalData signal, final HashMap<Long, SymbolModel> symbolMap) throws ApplicationException {
		final List<PositionData> positions = portfolio.getPositions();
		if(positions.size() == 0) {
			return null;
		}
		if(signal.getSymbolInfo().getSymbol().equalsIgnoreCase("cuda")) {
			System.err.println("cuda");
		}
		
		/*
		 * Comparator to sort by % gains.
		 */
		Comparator<PositionData> pctChgSorter = new Comparator<PositionData>() {
			@Override
			public int compare(PositionData arg0, PositionData arg1) {
				return (int) (arg0.getPercentGainLoss() - arg1.getPercentGainLoss());
			}
		};
	
		final List<PositionData> sortedPos = new ArrayList<PositionData>(positions);
		Collections.sort(sortedPos, pctChgSorter);
		
		for(final PositionData aPos: sortedPos) {
			final SymbolModel sym = symbolMap.get(aPos.getMsId());
			if(sym == null || sym.getPriceForDate(runDate) == null) {
				continue;
			}
			if(aPos.getPercentGainLoss() < 0) {
				
				final TransactionData latestBuyTx = aPos.getLatestUnSoldBuyTransaction();
				final List<TransactionData> allTxs = aPos.getTransactions();
				final TransactionData firstTx = allTxs.get(0);
				
				
				final Date latestTxDt = aPos.getLatestTransactionDate(null);
				final int daysDiffFromLatestTx = Math.abs(Days.daysBetween(new DateTime(latestTxDt), new DateTime(runDate)).getDays());
				final int daysDiffFromLatestBuy = Math.abs(Days.daysBetween(new DateTime(latestBuyTx.getTransDt()), new DateTime(runDate)).getDays());
				final int daysDiffFromFirstBuy = Math.abs(Days.daysBetween(new DateTime(firstTx.getTransDt()), new DateTime(runDate)).getDays());
	
				if(daysDiffFromLatestTx < 1) {
					// same day tx not allowed
					continue;
				}
				
				if(daysDiffFromFirstBuy <= 5 || daysDiffFromLatestBuy <= 5){ 
					continue;
				} else if (daysDiffFromFirstBuy < 10 && aPos.getPercentGainLoss() > 0) {
						continue;
				}
				
				RunLogger.getRunLogger().logPortfolio(runDate + " Selling loser due to underperformance: " + aPos.getSymbol() + " " + aPos.getPercentGainLoss() + "% in " + daysDiffFromFirstBuy);
				final List<PositionData> sellPositions = new ArrayList<PositionData>();
				sellPositions.add(aPos);
				final List<TransactionData> swapOutAndInTxs= sellPositionsAndUpdatePortfolioPositions(sellPositions, runDate);
				if(swapOutAndInTxs != null && swapOutAndInTxs.size() > 0) {
					final TransactionData inTx = this.buyPositionAndUpdatePortfolioPositions(signal.getSymbolInfo().getMsId(), signal.getSymbolInfo().getSymbol(), this.getMaxBuyAmountAllowedForExecution(null), runDate); 
					swapOutAndInTxs.add(inTx);
					return swapOutAndInTxs;
				}
			} else {
				// pct-chg > 0 means we exhausted with the losers. we need underperformance logic for swap now
				break;
			}
		}
		
		boolean highConvictionSignal = signal.getSignalStrength() != null && signal.getSignalStrength() >= 100;
		if(!highConvictionSignal) {
			return null;
		}
		// exhausted selling loser. now trying to check for under-performers
		for(final PositionData aPos: sortedPos) {
			final SymbolModel sym = symbolMap.get(aPos.getMsId());
			if(sym == null || sym.getPriceForDate(runDate) == null) {
				continue;
			}
			final TransactionData latestBuyTx = aPos.getLatestTransaction(TransactionType.BUY);
			final List<TransactionData> allTxs = aPos.getTransactions();
			final TransactionData firstTx = allTxs.get(0);
			
			final int daysDiffFromLatestBuy = Math.abs(Days.daysBetween(new DateTime(latestBuyTx.getTransDt()), new DateTime(runDate)).getDays());
			final int daysDiffFromFirstBuy = Math.abs(Days.daysBetween(new DateTime(firstTx.getTransDt()), new DateTime(runDate)).getDays());

			if(daysDiffFromFirstBuy <= 5 || daysDiffFromLatestBuy <= 5){ 
				continue;
			} else if (daysDiffFromFirstBuy < 10 && aPos.getPercentGainLoss() > 0) {
					continue;
			} else if(daysDiffFromFirstBuy < 25 && aPos.getPercentGainLoss() > 0.5) {
				continue;
			} else  if(daysDiffFromFirstBuy < 40 && aPos.getPercentGainLoss() > 1) {
				continue;
			} else  if(daysDiffFromFirstBuy < 60 && aPos.getPercentGainLoss() > 1.5) {
				continue;
			} else if(daysDiffFromFirstBuy < 75 && aPos.getPercentGainLoss() > 2) {
				continue;
			} else if(daysDiffFromFirstBuy < 90 && aPos.getPercentGainLoss() > 2.5) {
				continue;
			} else if(daysDiffFromFirstBuy < 105 && aPos.getPercentGainLoss() > 3) {
				continue;
			} else if(daysDiffFromFirstBuy < 120 && aPos.getPercentGainLoss() > 3.5) {
				continue;
			} else if(daysDiffFromFirstBuy < 135 && aPos.getPercentGainLoss() > 4) {
				continue;
			} else if(daysDiffFromFirstBuy < 150 && aPos.getPercentGainLoss() > 4.5) {
				continue;
			}else if (daysDiffFromFirstBuy >= 150 && aPos.getPercentGainLoss() > 5) {
				continue;
			}
			
			// this check likely will avoid holding a stock that goes sideways/nowhere after breakout 
			if(daysDiffFromFirstBuy >= 7 && aPos.getPercentGainLoss() > 0) {
				final PriceAnalysisData pa = sym.getPriceAnalysisForDate(runDate);
				if(pa == null) {
					continue;
				}
				
				final PriceAnalysisElementData pae1 = pa.getAnalysisFor(ChartElementType.PRICE_MA_20);
				if(pae1 != null) {
					if(pae1.getAnalysisType() == ChartAnalysisType.BREAKING_UP || 
							pae1.getAnalysisType() == ChartAnalysisType.SUPPORT) {
						// if it has 20ema support then we will continue to keep it
						continue;
					} 
				}
				final BigDecimal pctChgFrom20Ema = pa.getElementPctChg(ChartElementType.PRICE_MA_20);
				if(pctChgFrom20Ema.doubleValue() >= -0.75) {
					continue;
				}
				
				final PriceAnalysisElementData pae2 = pa.getAnalysisFor(ChartElementType.PRICE_MA_50);
				if(pae2 != null) {
					if(pae2.getAnalysisType() == ChartAnalysisType.BREAKING_UP || 
							pae2.getAnalysisType() == ChartAnalysisType.SUPPORT) {
						continue;
					} else {
						final BigDecimal pctChgFrom50Sma = pa.getElementPctChg(ChartElementType.PRICE_MA_50);
						if(pctChgFrom50Sma.doubleValue() >= -0.75) {
							continue;
						}
					}
				}
			}
			
			if(aPos.getSymbol().equalsIgnoreCase("sbux")) {
				System.err.print("sbux");
			}
			RunLogger.getRunLogger().logPortfolio(runDate + " Selling due to underperformance: " + aPos.getSymbol() + " " + aPos.getPercentGainLoss() + "% in " + daysDiffFromFirstBuy);
			final List<PositionData> sellPositions = new ArrayList<PositionData>();
			sellPositions.add(aPos);
			final List<TransactionData> swapOutAndInTxs= sellPositionsAndUpdatePortfolioPositions(sellPositions, runDate);
			if(swapOutAndInTxs != null && swapOutAndInTxs.size() > 0) {
				final TransactionData inTx = this.buyPositionAndUpdatePortfolioPositions(signal.getSymbolInfo().getMsId(), signal.getSymbolInfo().getSymbol(), this.getMaxBuyAmountAllowedForExecution(null), runDate); 
				swapOutAndInTxs.add(inTx);
				return swapOutAndInTxs;
			}
			
		}
		
		if(highConvictionSignal) {
			final boolean veryHighConvictionSignal = signal.getSignalStrength() != null && signal.getSignalStrength() >= 150;
			if(veryHighConvictionSignal) {
				/*
				 * Comparator to sort by % gains.
				 */
				Comparator<PositionData> pctChgFrom50smaSorter = new Comparator<PositionData>() {
					@Override
					public int compare(PositionData arg0, PositionData arg1) {
						try {
							final SymbolModel sym1 = symbolMap.get(arg0.getMsId());
							if(sym1 == null || sym1.getPriceForDate(runDate) == null) {
								return Integer.MIN_VALUE;
							}
							final SymbolModel sym2 = symbolMap.get(arg1.getMsId());
							if(sym2 == null || sym2.getPriceForDate(runDate) == null) {
								return Integer.MIN_VALUE;
							}
							
							final PriceAnalysisData pa1 = sym1.getPriceAnalysisForDate(runDate);
							if(pa1 == null) {
								return Integer.MIN_VALUE;
							}
							final PriceAnalysisData pa2 = sym2.getPriceAnalysisForDate(runDate);
							if(pa2 == null) {
								return Integer.MIN_VALUE;
							}
							BigDecimal pae1 = pa1.getElementPctChg(ChartElementType.PRICE_MA_50);
							if(pae1 == null) {
								pae1 = pa1.getElementPctChg(ChartElementType.PRICE_MA_20);
							}
							BigDecimal pae2 = pa2.getElementPctChg(ChartElementType.PRICE_MA_50);
							if(pae2 == null) {
								pae2 = pa2.getElementPctChg(ChartElementType.PRICE_MA_20);
							}
							
							return pae1.compareTo(pae2);
						}catch(Exception ex) {
							return Integer.MIN_VALUE;
						}
					}
				};
			
				final List<PositionData> sortedPos2 = new ArrayList<PositionData>(positions);
				Collections.sort(sortedPos, pctChgFrom50smaSorter);
				
				// highConvictionSignal with strength > 150 will need aggressive sell-swaps
				for(final PositionData aPos: sortedPos2) {
					final SymbolModel sym = symbolMap.get(aPos.getMsId());
					if(sym == null || sym.getPriceForDate(runDate) == null) {
						continue;
					}
					final PriceAnalysisData pa = sym.getPriceAnalysisForDate(runDate);
					if(pa == null) {
						continue;
					}
					
					final BigDecimal pctChgFrom20Ema = pa.getElementPctChg(ChartElementType.PRICE_MA_20);
					if(pctChgFrom20Ema.doubleValue() >= -0.75) {
						continue;
					}
					
					RunLogger.getRunLogger().logPortfolio(runDate + " High Conviction swap due to underperformance: " + aPos.getSymbol() + " " + aPos.getPercentGainLoss() + "%");
					final List<PositionData> sellPositions = new ArrayList<PositionData>();
					sellPositions.add(aPos);
					final List<TransactionData> swapOutAndInTxs= sellPositionsAndUpdatePortfolioPositions(sellPositions, runDate);
					if(swapOutAndInTxs != null && swapOutAndInTxs.size() > 0) {
						final TransactionData inTx = this.buyPositionAndUpdatePortfolioPositions(signal.getSymbolInfo().getMsId(), signal.getSymbolInfo().getSymbol(), this.getMaxBuyAmountAllowedForExecution(null), runDate); 
						swapOutAndInTxs.add(inTx);
						return swapOutAndInTxs;
					}
				}
				
				RunLogger.getRunLogger().logPortfolio(runDate + ": Could not swap a position for very high-conviction-signal: " + signal.getSymbolInfo().getSymbol() + "[" + signal.getSignalStrength() + "]");
			} else { 
				RunLogger.getRunLogger().logPortfolio(runDate + ": Could not swap a position for high-conviction-signal: " + signal.getSymbolInfo().getSymbol() + "[" + signal.getSignalStrength() + "]");
			}
		}
		
		return null;
	}
	
	private BigDecimal getMaxBuyAmountAllowedForPosition() throws ApplicationException {
		final BigDecimal realizedGainLoss = portfolio.getRealizedGainLoss();
		int adjustedRealizedGainLossPct = ((int) (realizedGainLoss.divide(new BigDecimal(initialCash, Helpers.mc)).multiply(new BigDecimal(100)).setScale(0, RoundingMode.DOWN).intValue() / 10)) * 10;
		
		if(prev_adjustedRealizedGainLossPct > adjustedRealizedGainLossPct) {
			//  0.25 threshold is added to prevent frequent rebalancing
			adjustedRealizedGainLossPct = ((int) (realizedGainLoss.divide(new BigDecimal(initialCash, Helpers.mc)).multiply(new BigDecimal(100)).add(new BigDecimal(0.25)).setScale(0, RoundingMode.DOWN).intValue() / 10)) * 10;
		}
		
		BigDecimal cashForCalculation = (new BigDecimal(initialCash)).multiply(new BigDecimal(1 + ((double) adjustedRealizedGainLossPct/100)));
		if(marginPctAllowed > 0) {
			cashForCalculation = cashForCalculation.multiply(new BigDecimal(1 + this.marginPctAllowed/100));
		}
		
		final BigDecimal maxAmtPerPosition = cashForCalculation.divide(new BigDecimal(numOfPositions), Helpers.mc);
		if(Math.abs(adjustedRealizedGainLossPct * 100) >= 10 && prev_adjustedRealizedGainLossPct != adjustedRealizedGainLossPct) {
			RunLogger.getRunLogger().logPortfolio("Realized-Gain-Loss: " + realizedGainLoss.doubleValue() + " adjusted-to-%: " + adjustedRealizedGainLossPct + "% max-amt-per-position is: " +  maxAmtPerPosition.doubleValue());
		}
		if(prev_adjustedRealizedGainLossPct != adjustedRealizedGainLossPct) {
			prev_adjustedRealizedGainLossPct = adjustedRealizedGainLossPct;
		}
		
		return maxAmtPerPosition;
	}
	
	private int prev_adjustedRealizedGainLossPct = 0;
	private CurrencyData getMaxBuyAmountAllowedForExecution(final PositionData aPos) throws ApplicationException {
		
		final BigDecimal maxAmtPerPosition = this.getMaxBuyAmountAllowedForPosition();
		
		final BigDecimal firstPurchaseAmt = maxAmtPerPosition.multiply(new BigDecimal(0.5), Helpers.mc); //50%
		final BigDecimal secondPurchaseAmt = maxAmtPerPosition.multiply(new BigDecimal(0.3), Helpers.mc); //30%
		final BigDecimal thirdPurchaseAmt = maxAmtPerPosition.multiply(new BigDecimal(0.2), Helpers.mc); //20%
		final BigDecimal fourthPurchaseAmt = maxAmtPerPosition.multiply(new BigDecimal(0.15), Helpers.mc); //15%
		final BigDecimal fifthPurchaseAmt = maxAmtPerPosition.multiply(new BigDecimal(0.1), Helpers.mc); //10%
		
		CurrencyData maxCashAvailForExecution = null;
		if(aPos == null) {
			// first time purchase..
			maxCashAvailForExecution = CurrencyData.instantiate(firstPurchaseAmt);
		} else {
			// assumption is that we do buys in 0.5,0.3,0.2 to fill full-max-amt-per-position
			// in future we might be adding 2xtimes full-max-amt-per-position with additional 0.2, 0.2, 0.2, 0.2, 0.2 transactions
			
			final Map<TransactionType, Integer> txCountMap = aPos.getTransactionCount();
			final int buyCount = txCountMap.get(TransactionType.BUY) == null ? 0 : txCountMap.get(TransactionType.BUY);
			final int sellCount = txCountMap.get(TransactionType.SELL) == null ? 0 : txCountMap.get(TransactionType.SELL);
			
			if(buyCount - sellCount <= 0) {
				maxCashAvailForExecution = CurrencyData.instantiate(firstPurchaseAmt);
			} else if (buyCount-sellCount == 1) {
				maxCashAvailForExecution = CurrencyData.instantiate(secondPurchaseAmt);
			} else if (buyCount-sellCount == 2) {
				maxCashAvailForExecution = CurrencyData.instantiate(thirdPurchaseAmt);
			} else if (buyCount-sellCount == 3) {
				maxCashAvailForExecution = CurrencyData.instantiate(fourthPurchaseAmt);
			} else if (buyCount-sellCount >= 4) {
				maxCashAvailForExecution = CurrencyData.instantiate(fifthPurchaseAmt);
			} else if (buyCount-sellCount >= 5) {
				maxCashAvailForExecution = null;
			}
		}
		
		return maxCashAvailForExecution;
	}
	
	/*
	 * Check for Secondary Buy/Sell transactions on existing positions using market-timing conditions 
	 */
	private List<TransactionData> performSecondaryBuySellChecks(final Date runDate, final HashMap<Long, SymbolModel> symbolMap, SimpleMarketSignalType marketSignal) throws ApplicationException {
		final List<PositionData> positions = portfolio.getPositions();
		if(positions == null || positions.size() == 0 || marketSignal == null) {
			return null;
		}
		
		final List<TransactionData> txs = new ArrayList<TransactionData>();
		
		// secondary positions are purchased only if market is NOT in DOWNTREND
		//if(marketSignal != SimpleMarketSignalType.DOWNTREND) {
			for(PositionData aPos: positions) {
				final SymbolModel sym = symbolMap.get(aPos.getMsId());
				if(sym == null || sym.getPriceForDate(runDate) == null) {
					continue;
				}
				
				final CurrencyData maxAllowedAmtForPurchase = this.getMaxBuyAmountAllowedForExecution(aPos);
				if(maxAllowedAmtForPurchase == null || maxAllowedAmtForPurchase.getValue().compareTo(BigDecimal.ZERO) <= 0) {
					// no more purchases allowed-- move on to next position
					continue;
				}
				
				final TransactionData lastBuyTx = aPos.getLatestUnSoldBuyTransaction();
				
				final Date latestBuyTxDt = lastBuyTx.getTransDt();
				final Date latestTxDt = aPos.getLatestTransactionDate(null);

				final int daysSinceLastTx = Math.abs(Days.daysBetween(new DateTime(latestTxDt), new DateTime(runDate)).getDays());
				if(daysSinceLastTx < 5) {
					// two-day rule for secondary-buys
					continue;
				}
				
				final int daysSinceLastBuyTx = Math.abs(Days.daysBetween(new DateTime(latestBuyTxDt), new DateTime(runDate)).getDays());
				if(daysSinceLastBuyTx < 5) {
					// need at least 5 days since last purchase to do any secondary purchases
					continue;
				}
				
				final double pctChg = aPos.getPercentGainLoss();
				final double pctChgSinceLastBuy = Helpers.getPercetChange(sym.getPriceForDate(runDate).getClose(), lastBuyTx.getCostBasisPerShare()).doubleValue();

				boolean useAggressiveBuy = false;
				if(marketSignal == SimpleMarketSignalType.DOWNTREND) {
					if(pctChg >= 15 || pctChgSinceLastBuy >= 15) {
						useAggressiveBuy = true;
					}
				} else {
					if(pctChg >= 10 || pctChgSinceLastBuy >= 10) {
						useAggressiveBuy = true;
					}
				}
				
				final double pctChgInUpTrend = 7.5;
				final double pctChgInCautiousUpTrend = pctChgInUpTrend;
				final double pctChgInDownTrend = 7.5;

				final double pctChgSinceLastBuyInUpTrend = pctChgInUpTrend;
				final double pctChgSinceLastBuyInCautiousUpTrend = pctChgInCautiousUpTrend;
				final double pctChgSinceLastBuyInDownTrend = pctChgInDownTrend;
				
				final PriceAnalysisData pa = sym.getPriceAnalysisForDate(runDate);
				if(pa== null){
					continue;
				}
				final PriceAnalysisElementData pae0= pa.getAnalysisFor(ChartElementType.PRICE_MA_10);
				final double pctChgToUse10emaBuyInUpTrend = 7.5;

				final PriceAnalysisElementData pae1 = pa.getAnalysisFor(ChartElementType.PRICE_MA_20);
				final double pctChgToUse20emaBuyInUpTrend = pctChgInUpTrend;
				final double pctChgToUse20emaBuyInCautiousUpTrend = pctChgInCautiousUpTrend;
				
				final PriceAnalysisElementData pae2= pa.getAnalysisFor(ChartElementType.PRICE_MA_50);
				final double pctChgToUse50smaBuyInUpTrend = pctChgInUpTrend;
				final double pctChgToUse50smaBuyInCautiousUpTrend = pctChgInCautiousUpTrend;
				final double pctChgToUse50smaBuyInDowntrend = pctChgInDownTrend;

				if(marketSignal == SimpleMarketSignalType.UPTREND) {
					if(pctChg < pctChgInUpTrend || pctChgSinceLastBuy < pctChgSinceLastBuyInUpTrend) {
						// need at least 5% gains overall and 2.5% chg from last purchase before proceeding with purchase
						continue;
					}
					
					if(pae0 != null) {
						final ChartAnalysisType cat = pae0.getAnalysisType();
						if(pctChg > pctChgToUse10emaBuyInUpTrend) {
							if( cat == ChartAnalysisType.BREAKING_UP) {
								if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice10dEma()).doubleValue() <= 2) {
									final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
									txs.add(trans);
									continue;
								}
							} else if(useAggressiveBuy && cat == ChartAnalysisType.SUPPORT) {
								// aggressive buys will buy anytime stock is near support of MA
								final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
								if(Helpers.getPercetChange(pa.getPrice().getClose(), prevDayPrice.getClose()).doubleValue() >= -1) {
									// add to buy-list only on a 'relatively' blue-day
									final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
									txs.add(trans);
									continue;
								}
							}
						}
					} 
					if(pctChg >= pctChgToUse20emaBuyInUpTrend && pae1 != null) {
						final ChartAnalysisType cat20 = pae1.getAnalysisType();
							if(cat20 == ChartAnalysisType.BREAKING_UP) {
								if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice20dEma()).doubleValue() <= 2) {
									final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
									txs.add(trans);
									continue;
								}
							} else  if(useAggressiveBuy) {
							    if(cat20 == ChartAnalysisType.SUPPORT) {
									// aggressive buys will buy any time stock is near support of 20 MA
									final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
									if(Helpers.getPercetChange(pa.getPrice().getClose(), prevDayPrice.getClose()).doubleValue() >= -1) {
										// add to buy-list only on a 'relatively' blue-day
										final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
										txs.add(trans);
										continue;
									}
								}
							}
					} 
					if(pctChg >= pctChgToUse50smaBuyInUpTrend && pae2 != null) {
						final ChartAnalysisType cat50 = pae2.getAnalysisType();
						if(cat50 == ChartAnalysisType.BREAKING_UP) {
							if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice50dSma()).doubleValue() <= 2) {
								final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
								txs.add(trans);
								continue;
							}
						} else  if(useAggressiveBuy) {
							if(cat50 == ChartAnalysisType.SUPPORT) {
								// aggressive buys will buy anytime stock is near support of MA
								final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
								if(Helpers.getPercetChange(pa.getPrice().getClose(), prevDayPrice.getClose()).doubleValue() >= -1) {
									// add to buy-list only on a 'relatively' blue-day
									final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
									txs.add(trans);
									continue;
								}
							}
						}
					}

					
				} else if(marketSignal == SimpleMarketSignalType.CAUTIOUS_UPTREND) {
					if(pctChg < pctChgInCautiousUpTrend || pctChgSinceLastBuy < pctChgSinceLastBuyInCautiousUpTrend) {
						// need at least 5% gains overall and 2.5% chg from last purchase before proceeding with purchase
						continue;
					}
					
					if(useAggressiveBuy) {
						// cautious-uptrend allow for buy off 10ema  only if it is breaking up
						final ChartAnalysisType cat10 = pae0 == null ? null : pae0.getAnalysisType();
						if( cat10 == ChartAnalysisType.BREAKING_UP) {
							if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice10dEma()).doubleValue() <= 1.75) {
								final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
								txs.add(trans);
								continue;
							}
						}
					}

					
					if(pctChg >= pctChgToUse20emaBuyInCautiousUpTrend && pae1 != null) {
						final ChartAnalysisType cat20 = pae1.getAnalysisType();
							if(cat20 == ChartAnalysisType.BREAKING_UP) {
								if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice20dEma()).doubleValue() <= 1.75) {
									final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
									txs.add(trans);
									continue;
								}
							} else if(useAggressiveBuy) {
								if(cat20 == ChartAnalysisType.SUPPORT) {
									// aggressive buys will buy anytime stock is near support of MA
									final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
									if(Helpers.getPercetChange(pa.getPrice().getClose(), prevDayPrice.getClose()).doubleValue() >= -1) {
										// add to buy-list only on a 'relatively' blue-day
										final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
										txs.add(trans);
										continue;
									}
								} 
							}
					} 
					if(pctChg >= pctChgToUse50smaBuyInCautiousUpTrend && pae2 != null) {
						final ChartAnalysisType cat50 = pae2.getAnalysisType();
						if(cat50 == ChartAnalysisType.BREAKING_UP) {
							if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice50dSma()).doubleValue() <= 1.75) {
								final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
								txs.add(trans);
								continue;
							}
						} else  if(useAggressiveBuy && cat50 == ChartAnalysisType.SUPPORT) {
							// aggressive buys will buy anytime stock is near support of MA
							final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
							if(Helpers.getPercetChange(pa.getPrice().getClose(), prevDayPrice.getClose()).doubleValue() >= -1) {
								// add to buy-list only on a 'relatively' blue-day
								final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
								txs.add(trans);
								continue;
							}
						}
					}
				} else if(marketSignal == SimpleMarketSignalType.DOWNTREND) {
					if(pctChg < pctChgInDownTrend || pctChgSinceLastBuy < pctChgSinceLastBuyInDownTrend) {
						// need at least 5% gains overall and 2.5% chg from last purchase before proceeding with purchase
						continue;
					}
					
					if(useAggressiveBuy) {
						// downtrend allow for buy off 20ema  only if it is breaking up
						
						final ChartAnalysisType cat10 = pae0 == null ? null : pae0.getAnalysisType();
						if( cat10 == ChartAnalysisType.BREAKING_UP) {
							if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice10dEma()).doubleValue() <= 1.5) {
								final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
								txs.add(trans);
								continue;
							}
						}
						
						final ChartAnalysisType cat20 = pae1 == null ? null : pae1.getAnalysisType();
						if( cat20 == ChartAnalysisType.BREAKING_UP) {
							if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice10dEma()).doubleValue() <= 1.5) {
								final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
								txs.add(trans);
								continue;
							}
						}
					}
					
					if(pctChg >= pctChgToUse50smaBuyInDowntrend && pae2 != null) {
						final ChartAnalysisType cat50 = pae2.getAnalysisType();
						if(cat50 == ChartAnalysisType.BREAKING_UP) {
							if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice50dSma()).doubleValue() <= 1.5) {
								final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
								txs.add(trans);
								continue;
							}
						} else  if(useAggressiveBuy) {
							if(cat50 == ChartAnalysisType.SUPPORT) {
						
								// aggressive buys will buy anytime stock is near support of MA
								final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
								if(Helpers.getPercetChange(pa.getPrice().getClose(), prevDayPrice.getClose()).doubleValue() >= -1) {
									// add to buy-list only on a 'relatively' blue-day
									final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getMsId(), aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
									txs.add(trans);
									continue;
								}
							}
						}
					}
				}
			}
		//}
		
		return txs;
	}
	/*
	* Market-UpTrend- do nothing
	* Market-CautiousUpTrend - Sell all position that are underperforming. 
	* Market-Downtrend - Sell all positions
	* 
	* UnderPerforming: If a position is below its 20dEMA OR position has negative returns of more than -2.5% since last purchase of more than 5 days
	*/
	private List<TransactionData> actOnMarketTimeSignal(Date runDate, HashMap<Long, SymbolModel> symbolMap, SimpleMarketSignalType marketSignal) throws ApplicationException {
		List<PositionData> pos = portfolio.getPositions();
		if(pos.size() == 0 || marketSignal == null) {
			return null;
		}
	
		final List<PositionData> sellPositions = new ArrayList<PositionData>();
		
		/*
		 * Comparator to sort by % gains.
		 */
		Comparator<PositionData> pctChgSorter = new Comparator<PositionData>() {
			@Override
			public int compare(PositionData arg0, PositionData arg1) {
				return (int) (arg0.getPercentGainLoss() - arg1.getPercentGainLoss());
			}
		};
	
		final Set<String> symbolsIdentifiedAsSell = new HashSet<String>();
		
		final List<PositionData> sortedPos = new ArrayList<PositionData>(pos);
		Collections.sort(sortedPos, pctChgSorter);
	
		if(marketSignal == SimpleMarketSignalType.UPTREND) {
			for(PositionData aPos: sortedPos) {
				final SymbolModel sym = symbolMap.get(aPos.getMsId());
				if(sym == null) {
					throw new RuntimeException("sym-model is empty for position symbol-" + aPos.getSymbol());
				}
				
				final PriceAnalysisData pa = sym.getPriceAnalysisForDate(runDate);
				if(pa == null) {
					continue;
				}
				
				// if any of MAs are supporting the price then dont do any sells
				final PriceAnalysisElementData pae10 = pa.getAnalysisFor(ChartElementType.PRICE_MA_10);
				final PriceAnalysisElementData pae20 = pa.getAnalysisFor(ChartElementType.PRICE_MA_20);
				final PriceAnalysisElementData pae50 = pa.getAnalysisFor(ChartElementType.PRICE_MA_50);
				
				if(pae10 != null && (pae10.getAnalysisType() == ChartAnalysisType.BREAKING_UP || pae10.getAnalysisType() == ChartAnalysisType.SUPPORT) ) {
					continue;
				}
				if(pae20 != null && (pae20.getAnalysisType() == ChartAnalysisType.BREAKING_UP || pae20.getAnalysisType() == ChartAnalysisType.SUPPORT) ) {
					continue;
				}
				if(pae50 != null && (pae50.getAnalysisType() == ChartAnalysisType.BREAKING_UP || pae50.getAnalysisType() == ChartAnalysisType.SUPPORT) ) {
					continue;
				}

				final int daysDiffFromLatestBuy = Math.abs(Days.daysBetween(new DateTime(aPos.getLatestTransactionDate(null)), new DateTime(runDate)).getDays());
				final double pctChg = aPos.getPercentGainLoss();
				final BigDecimal pctChgFrom20dEma = pa.getElementPctChg(ChartElementType.PRICE_MA_20);
				if(daysDiffFromLatestBuy >= 5 && pctChg < 0.5 && pctChgFrom20dEma != null && pctChgFrom20dEma.doubleValue() < -2 && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
					final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
					// add to sell-list only on a red-day
					if(prevDayPrice.getClose().doubleValue() >  pa.getPrice().getClose().doubleValue()) {
						RunLogger.getRunLogger().logPortfolio(runDate + " Market-Uptrend-Sell 20EMA: (" + pctChgFrom20dEma.doubleValue() + "%)" + aPos.getQuantity() + " shares of " + aPos.getSymbol() + " for " + aPos.getPercentGainLoss());
						sellPositions.add(aPos);
						symbolsIdentifiedAsSell.add(aPos.getSymbol());
						continue;
					}
				}
				
				BigDecimal pctChgFromMA = pa.getElementPctChg(ChartElementType.PRICE_MA_50);
				if(pctChgFromMA == null) {
					// recent IPOs may not have 50sma established yet (TWTR Jan-2014). So we use 20ema instead
					pctChgFromMA = pa.getElementPctChg(ChartElementType.PRICE_MA_20);
					
					if(pctChgFromMA == null) {
						RunLogger.getRunLogger().logPortfolio(runDate + " : Using 10ema for uptrend-sell checks for symbol:" + aPos.getSymbol());
						// use 10ema if even 20ema is not available.
						pctChgFromMA = pa.getElementPctChg(ChartElementType.PRICE_MA_10);
					} else {
						RunLogger.getRunLogger().logPortfolio(runDate + " : Using 20ema for uptrend-sell checks for symbol:" + aPos.getSymbol());
					}
					
				}
				if(pctChgFromMA != null && pctChgFromMA.doubleValue() <= -2 && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
					final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
					// add to sell-list only on a red-day
					if(prevDayPrice.getClose().doubleValue() >  pa.getPrice().getClose().doubleValue()) {
						RunLogger.getRunLogger().logPortfolio(runDate + "Market-Uptrend-Sell 50EDMA: (" + pctChgFromMA.doubleValue() + "%)" + aPos.getQuantity() + " shares of " + aPos.getSymbol() + " for " + aPos.getPercentGainLoss());
						sellPositions.add(aPos);
						symbolsIdentifiedAsSell.add(aPos.getSymbol());
						continue;
					}
				}
			}
		} else if (marketSignal == SimpleMarketSignalType.DOWNTREND) {
			// sell positions in downtrend

			for(PositionData aPos: sortedPos) {
				final SymbolModel sym = symbolMap.get(aPos.getMsId());
				if(sym == null) {
					throw new RuntimeException("sym-model is empty for position symbol-" + aPos.getSymbol());
				}
				
				final PriceAnalysisData pa = sym.getPriceAnalysisForDate(runDate);
				if(pa== null) {
					continue;
				}
				
				// if any of MAs are supporting the price then dont do any sells
				final PriceAnalysisElementData pae10 = pa.getAnalysisFor(ChartElementType.PRICE_MA_10);
				final PriceAnalysisElementData pae20 = pa.getAnalysisFor(ChartElementType.PRICE_MA_20);
				final PriceAnalysisElementData pae50 = pa.getAnalysisFor(ChartElementType.PRICE_MA_50);
				
				if(pae10 != null && (pae10.getAnalysisType() == ChartAnalysisType.BREAKING_UP || pae10.getAnalysisType() == ChartAnalysisType.SUPPORT) ) {
					continue;
				}
				if(pae20 != null && (pae20.getAnalysisType() == ChartAnalysisType.BREAKING_UP || pae20.getAnalysisType() == ChartAnalysisType.SUPPORT) ) {
					continue;
				}
				if(pae50 != null && (pae50.getAnalysisType() == ChartAnalysisType.BREAKING_UP || pae50.getAnalysisType() == ChartAnalysisType.SUPPORT) ) {
					continue;
				}

				final BigDecimal pctChgFrom20ema = pa.getElementPctChg(ChartElementType.PRICE_MA_20);
				
				if(aPos.getPercentGainLoss() < 2) { // <2%
					if(pctChgFrom20ema != null && pctChgFrom20ema.doubleValue() <= -1.5 && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
						final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
						// add to sell-list only on a red-day
						if(prevDayPrice.getClose().doubleValue() >  pa.getPrice().getClose().doubleValue()) {
							RunLogger.getRunLogger().logPortfolio(runDate + " Market-Downtrend-Sell 20EMA: (" + pctChgFrom20ema.doubleValue() + "%)" + aPos.getQuantity() + " shares of " + aPos.getSymbol() + " for " + aPos.getPercentGainLoss());
							sellPositions.add(aPos);
							symbolsIdentifiedAsSell.add(aPos.getSymbol());
							continue;
						}
					}
				}
				
				BigDecimal pctChgFromMA = pa.getElementPctChg(ChartElementType.PRICE_MA_50);
				if(pctChgFromMA == null) {
					// recent IPOs may not have 50sma established yet (TWTR Jan-2014). So we use 20ema instead
					pctChgFromMA = pa.getElementPctChg(ChartElementType.PRICE_MA_20);
					
					if(pctChgFromMA == null) {
						RunLogger.getRunLogger().logPortfolio(runDate + " : Using 10ema for downtrend-sell checks for symbol:" + aPos.getSymbol());
						// use 10ema if even 20ema is not available.
						pctChgFromMA = pa.getElementPctChg(ChartElementType.PRICE_MA_10);
					} else {
						RunLogger.getRunLogger().logPortfolio(runDate + " : Using 20ema for downtrend-sell checks for symbol:" + aPos.getSymbol());
					}
				}
				
				if(pctChgFromMA != null && pctChgFromMA.doubleValue() <= -1 && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
					final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
					// add to sell-list only on a red-day
					if(prevDayPrice.getClose().doubleValue() >  pa.getPrice().getClose().doubleValue()) {
						RunLogger.getRunLogger().logPortfolio(runDate + "Market-Downtrend-Sell 50EDMA: (" + pctChgFromMA.doubleValue() + "%)" + aPos.getQuantity() + " shares of " + aPos.getSymbol() + " for " + aPos.getPercentGainLoss());
						sellPositions.add(aPos);
						symbolsIdentifiedAsSell.add(aPos.getSymbol());
						continue;
					}
				}
				
				
				if(pctChgFrom20ema.doubleValue() < -0.5) { 
					if(aPos.getPercentGainLoss() <= -1 && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
						final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
						// add to sell-list only on a red-day
						if(prevDayPrice.getClose().doubleValue() >  pa.getPrice().getClose().doubleValue()) {
							RunLogger.getRunLogger().logPortfolio(runDate + "Market-Downtrend-Sell -1%: (" + aPos.getPercentGainLoss() + "%)" + aPos.getQuantity() + " shares of " + aPos.getSymbol() + " for " + aPos.getPercentGainLoss());
							sellPositions.add(aPos);
							symbolsIdentifiedAsSell.add(aPos.getSymbol());
							continue;
						}
					}
				}
			}
		}  else {
			// market-cautious-uptrend.. this is where we do underperforming sells and TA sells
			for(PositionData aPos: sortedPos) {
				final SymbolModel sym = symbolMap.get(aPos.getMsId());
				if(sym == null) {
					throw new RuntimeException("sym-model is empty for position symbol-" + aPos.getSymbol());
				}
				
				final PriceAnalysisData pa = sym.getPriceAnalysisForDate(runDate);
				
				// if any of MAs are supporting the price then dont do any sells
				final PriceAnalysisElementData pae10 = pa.getAnalysisFor(ChartElementType.PRICE_MA_10);
				final PriceAnalysisElementData pae20 = pa.getAnalysisFor(ChartElementType.PRICE_MA_20);
				final PriceAnalysisElementData pae50 = pa.getAnalysisFor(ChartElementType.PRICE_MA_50);
				
				if(pae10 != null && (pae10.getAnalysisType() == ChartAnalysisType.BREAKING_UP || pae10.getAnalysisType() == ChartAnalysisType.SUPPORT) ) {
					continue;
				}
				if(pae20 != null && (pae20.getAnalysisType() == ChartAnalysisType.BREAKING_UP || pae20.getAnalysisType() == ChartAnalysisType.SUPPORT) ) {
					continue;
				}
				if(pae50 != null && (pae50.getAnalysisType() == ChartAnalysisType.BREAKING_UP || pae50.getAnalysisType() == ChartAnalysisType.SUPPORT) ) {
					continue;
				}

				
				//final int daysFromLastTx = Math.abs(Days.daysBetween(new DateTime(runDate), new DateTime(aPos.getLatestTransactionDate(TransactionType.BUY))).getDays());
				final BigDecimal pctChgFrom20ema = pa.getElementPctChg(ChartElementType.PRICE_MA_20);
				
				if(pctChgFrom20ema != null && pctChgFrom20ema.doubleValue() < -0.5) { 
					if(aPos.getPercentGainLoss() <= -3  && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
						final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
						// add to sell-list only on a red-day
						if(prevDayPrice.getClose().doubleValue() >  pa.getPrice().getClose().doubleValue()) {
							RunLogger.getRunLogger().logPortfolio(runDate + " Market-CautiousUp-Sell -3%: (" + aPos.getPercentGainLoss()+ "%)" + aPos.getQuantity() + " shares of " + aPos.getSymbol() + " for " + aPos.getPercentGainLoss());
							sellPositions.add(aPos);
							symbolsIdentifiedAsSell.add(aPos.getSymbol());
							continue;
						}
					}
				}
				
				final int daysDiffFromLatestBuy = Math.abs(Days.daysBetween(new DateTime(aPos.getLatestTransactionDate(null)), new DateTime(runDate)).getDays());
				final double pctChg = aPos.getPercentGainLoss();
				
				if(daysDiffFromLatestBuy >= 5 && pctChg < 1.5 && pctChgFrom20ema != null && pctChgFrom20ema.doubleValue() < -2 && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
					final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
					// add to sell-list only on a red-day
					if(prevDayPrice.getClose().doubleValue() >  pa.getPrice().getClose().doubleValue()) {
						RunLogger.getRunLogger().logPortfolio(runDate + " Market-CautiousUp-Sell 20EMA: (" + pctChgFrom20ema.doubleValue() + "%)" + aPos.getQuantity() + " shares of " + aPos.getSymbol() + " for " + aPos.getPercentGainLoss());
						sellPositions.add(aPos);
						symbolsIdentifiedAsSell.add(aPos.getSymbol());
						continue;
					}
				}
				
				BigDecimal pctChgFromMA = pa.getElementPctChg(ChartElementType.PRICE_MA_50);
				if(pctChgFromMA == null) {
					// recent IPOs may not have 50sma established yet (TWTR Jan-2014). So we use 20ema instead
					pctChgFromMA = pa.getElementPctChg(ChartElementType.PRICE_MA_20);
					
					if(pctChgFromMA == null) {
						RunLogger.getRunLogger().logPortfolio(runDate + " : Using 10ema for cautiousup-sell checks for symbol:" + aPos.getSymbol());
						// use 10ema if even 20ema is not available.
						pctChgFromMA = pa.getElementPctChg(ChartElementType.PRICE_MA_10);
					} else {
						RunLogger.getRunLogger().logPortfolio(runDate + " : Using 20ema for cautiousup-sell checks for symbol:" + aPos.getSymbol());
					}
				}
				if(pctChgFromMA != null && pctChgFromMA.doubleValue() <= -1.5 && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
					final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
					// add to sell-list only on a red-day
					if(prevDayPrice.getClose().doubleValue() >  pa.getPrice().getClose().doubleValue()) {
						RunLogger.getRunLogger().logPortfolio(runDate + " Market-CautiousUp-Sell 50EMA: (" + pctChgFromMA.doubleValue() + "%)" + aPos.getQuantity() + " shares of " + aPos.getSymbol() + " for " + aPos.getPercentGainLoss());
						sellPositions.add(aPos);
						symbolsIdentifiedAsSell.add(aPos.getSymbol());
						continue;
					}
				}
			}
		}

		if(sellPositions.size() > 0) {
			return sellPositionsAndUpdatePortfolioPositions(sellPositions, runDate);
		}			

		return null;
	}
	
	/*
	 * Checks the current positions - identifies any sells
	 */
	private HashMap<Long, SymbolModel> updatePortfolioPositionsWithLatestPrices(Date runDate) throws ApplicationException {
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(runDate);
		startCal.add(Calendar.DATE, -1 * Helpers.PRICE_HISTORY_LOOKUP_DAYSBACK);
		
		portfolio.setDate(runDate);
		
		final HashMap<Long, SymbolModel> symbolMap = new HashMap<Long, SymbolModel>();
		
		final List<PositionData> positions = this.portfolio.getPositions();
		for(final PositionData pos : positions) {
			final SymbolModel posSym = this.wonDAO.getSymbolData(pos.getMsId(), startCal.getTime(), runDate, PeriodicityType.DAILY);
			if(posSym == null) {
				logger.warn(runDate + " Cannot find the symbol data for symbol:" + pos.getSymbol());
				continue;
			}
			symbolMap.put(pos.getMsId(), posSym);
			
			if(posSym.getSymInfo() != null) {
				pos.setCusip(posSym.getSymInfo().getCusip());
				pos.setGicsSubIndCode(posSym.getSymInfo().getGicsSubIndCode());
				pos.setIndCode(posSym.getSymInfo().getIndCode());
			}
			
			BigDecimal latestPrice = null;
			if(posSym.getHeaderInfo() != null) {
				latestPrice = new BigDecimal(posSym.getHeaderInfo().getCurrPrice());
			}
			if(latestPrice == null || latestPrice.doubleValue() <= 0) {
				if(posSym.getPrices() != null && posSym.getPrices().size() > 0) {
					final InstrumentPriceModel latestPriceForPos = posSym.getPriceForDate(runDate);
					if(latestPriceForPos == null || latestPriceForPos.getDateType() != TradeDateType.TRADING_DATE) {
						logger.info(runDate + " - No latest price-analysis data found in symbol-model for position: " + pos.getSymbol());
						continue;
					} else{
						final int daysDiff = Math.abs(Days.daysBetween(new DateTime(latestPriceForPos.getPriceDate()), new DateTime(runDate)).getDays());
						if(daysDiff == 0) {
							latestPrice = latestPriceForPos.getClose();
						 } else {
							 logger.warn(runDate + "- No latest price tick was found for that date for symbol: " + pos.getSymbol());
							 continue;
						 }
					}
				} else {
					logger.warn(runDate + "- No price tick was found for that date for symbol: " + pos.getSymbol());
					continue;
				}
			}
			
			
			if(posSym.getSplit() != null && posSym.getSplit().getSplitFactor() != null) {
				// there is a split today.. apply the split factor to the position
				 pos.applySplit(posSym.getSplit().getSplitFactor());
				 RunLogger.getRunLogger().logPortfolio(runDate + " " + pos.getSymbol() +  ": Applying split factor of: " + posSym.getSplit().getSplitFactor().doubleValue());
			}
			
			pos.setCurrentPrice(latestPrice);
		}
		
		return symbolMap;
	}
	
	private List<TransactionData> performSellTechAnalysisForPositions(final Date tradeDate, final HashMap<Long, SymbolModel> symbolsMap) throws ApplicationException {
		final List<TransactionData> partialSellPositions = new ArrayList<TransactionData>(); 
		// perform loss-sells
		for(final PositionData pos : this.portfolio.getPositions()) {
			final SymbolModel symModel = symbolsMap.get(pos.getMsId());
			if(symModel == null) {
				logger.info(tradeDate + " No symbol-model found for position: " + pos.getSymbol());
				continue;
			}
			
			final List<PriceAnalysisData> paList= symModel.getPriceAnalysisData();
			if(paList == null) {
				logger.info(tradeDate + " No price-analysis data found in symbol-model for position: " + pos.getSymbol());
				continue;
			}
			
			final PriceAnalysisData prevPA= symModel.getPriceAnalysisForNDaysBeforeDate(1, tradeDate);
			final PriceAnalysisData currentPA= symModel.getPriceAnalysisForDate(tradeDate);
			if(prevPA == null || currentPA == null) {
				logger.info(tradeDate + " No latest price-analysis data found in symbol-model for position: " + pos.getSymbol());
				continue;
			}
						
			//perform aggressive sells here
			
			final Map<TransactionType, Integer> txCountMap = pos.getTransactionCount();
			final int buyCount = txCountMap.get(TransactionType.BUY) == null ? 0 : txCountMap.get(TransactionType.BUY);
			final int sellCount = txCountMap.get(TransactionType.SELL) == null ? 0 : txCountMap.get(TransactionType.SELL);
			
			if(buyCount - sellCount <= 1) {
				// we give some room to run for positions with single transactions
				continue;
			} 
			
			final TransactionData latestBuyTx = pos.getLatestUnSoldBuyTransaction();
			//final TransactionData latestSellTx = pos.getLatestTransaction(TransactionType.SELL);
			
			final BigDecimal pctChgFromLatestBuyTx = Helpers.getPercetChange(currentPA.getPrice().getClose(), latestBuyTx.getCostBasisPerShare());
			final BigDecimal pctChg  = Helpers.getPercetChange(currentPA.getPrice().getClose(), prevPA.getPrice().getClose());
			
			if(pctChgFromLatestBuyTx != null &&  pctChgFromLatestBuyTx.doubleValue() <= this.stopLossPct_SoftStop) {
				// sell only if it is not at support of a MA
				final PriceAnalysisElementData pae50 = currentPA.getAnalysisFor(ChartElementType.PRICE_MA_50);
				final PriceAnalysisElementData pae20 = currentPA.getAnalysisFor(ChartElementType.PRICE_MA_20);
				if( (pae50 != null && (pae50.getAnalysisType() != ChartAnalysisType.SUPPORT && pae50.getAnalysisType() != ChartAnalysisType.BREAKING_UP))
						|| (pae20 != null && (pae20.getAnalysisType() != ChartAnalysisType.SUPPORT && pae20.getAnalysisType() != ChartAnalysisType.BREAKING_UP))) {
					// sell only on a red-day
					if(pctChg.doubleValue() < 0) {
						RunLogger.getRunLogger().logPortfolio(tradeDate + " partial-sell due to poor latest-buy (" + pctChgFromLatestBuyTx.doubleValue() + "%): " + latestBuyTx.getQuantity() + " of " + latestBuyTx.getSymbol());
						final TransactionData partialSellTx = this.partialSellPositionAndUpdatePortfolioPositions(latestBuyTx.getMsId(), latestBuyTx.getSymbol(), latestBuyTx.getQuantity(), tradeDate);
						partialSellPositions.add(partialSellTx);
						continue;	
					}
				}
			}
			
			final BigDecimal prevAtr = prevPA.getAtr14d();
			final double closingRange = PriceChartAnalyzer.getClosingRange(currentPA.getPrice(), null);
			
			final BigDecimal pctChgFrom10ema  = currentPA.getElementPctChg(ChartElementType.PRICE_MA_10);
			final BigDecimal volRate = currentPA.getElementPctChg(ChartElementType.VOL);
			
			if(prevAtr != null && currentPA.getPrice().getClose().doubleValue() >= prevPA.getPrice().getClose().doubleValue() + 2 * prevAtr.doubleValue()
					&& closingRange >= 80
					&& pctChgFrom10ema != null && pctChgFrom10ema.doubleValue() > 5
					&& pctChg != null && pctChg.doubleValue() >= 5
					&& (volRate == null || volRate.doubleValue() >= 150)) {
				RunLogger.getRunLogger().logPortfolio(tradeDate + " " +  pos.getSymbol() + " ATR Sell signal. vol-rate: " + currentPA.getElementPctChg(ChartElementType.VOL) + "% ;cls-rng=" + closingRange);
				final TransactionData partialSellTx = this.partialSellPositionAndUpdatePortfolioPositions(latestBuyTx.getMsId(), latestBuyTx.getSymbol(), latestBuyTx.getQuantity(), tradeDate);
				partialSellPositions.add(partialSellTx);
				continue;	
			}
			
			//final BigDecimal pctChgFrom10ema  = currentPA.getElementPctChg(ChartElementType.PRICE_MA_10);
			final BigDecimal pctChgFrom20ema  = currentPA.getElementPctChg(ChartElementType.PRICE_MA_20);
			if(pctChgFrom10ema.doubleValue() >= 15 || pctChgFrom20ema != null && pctChgFrom20ema.doubleValue() >= 20) {
				RunLogger.getRunLogger().logPortfolio(tradeDate + " " +  pos.getSymbol() + " MA extended Sell signal. % from 10ema- " + pctChgFrom10ema + "; % from 20ema: " + pctChgFrom20ema );
				final TransactionData partialSellTx = this.partialSellPositionAndUpdatePortfolioPositions(latestBuyTx.getMsId(), latestBuyTx.getSymbol(), latestBuyTx.getQuantity(), tradeDate);
				partialSellPositions.add(partialSellTx);
				continue;
			}
			
			
			if(closingRange <= 30) {
				// bad close
				if(pctChg.doubleValue() <= -5 && (volRate == null || volRate.doubleValue() >= 200)) {
					// bad close and a high-pct-drop on heavy vol 
					RunLogger.getRunLogger().logPortfolio(tradeDate + " " +  pos.getSymbol() + " Weak Close on high vol. %-chg=" + pctChg.doubleValue() + "; vol-rate: " + currentPA.getElementPctChg(ChartElementType.VOL) + "% ;cls-rng=" + closingRange);
					final TransactionData partialSellTx = this.partialSellPositionAndUpdatePortfolioPositions(latestBuyTx.getMsId(), latestBuyTx.getSymbol(), latestBuyTx.getQuantity(), tradeDate);
					partialSellPositions.add(partialSellTx);
					continue;
				}
				
				final BigDecimal tr = currentPA.getPrice().getHigh().subtract(currentPA.getPrice().getLow());
				if ((volRate == null || volRate.doubleValue() >= 150)
					&& prevAtr.doubleValue() * 2 < tr.doubleValue() ) {
					// heavy vol + high volalite day + bad close
					RunLogger.getRunLogger().logPortfolio(tradeDate + " " +  pos.getSymbol() + " Weak Close on high vol volatile day. tr=" +  tr.doubleValue() + ";atr=" + prevAtr.doubleValue() + "; vol-rate: " + currentPA.getElementPctChg(ChartElementType.VOL) + "% ;cls-rng=" + closingRange);
					final TransactionData partialSellTx = this.partialSellPositionAndUpdatePortfolioPositions(latestBuyTx.getMsId(), latestBuyTx.getSymbol(), latestBuyTx.getQuantity(), tradeDate);
					partialSellPositions.add(partialSellTx);
					continue;
				}
			}
		}
		return partialSellPositions;
	}
	
	private List<TransactionData> checkAndPerformSellStopLossForPositions(final Date tradeDate, final HashMap<Long, SymbolModel> symbolMap, final SimpleMarketSignalType marketSignal) throws ApplicationException {
		final List<PositionData> sellPositions = new ArrayList<PositionData>(); 
		// perform loss-sells
		for(final PositionData pos : this.portfolio.getPositions()) {
			final SymbolModel sym = symbolMap.get(pos.getMsId());
			if(sym.getPriceForDate(tradeDate) == null) {
				continue;
			}
			
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MONTH, Calendar.JANUARY);
			cal.set(Calendar.DAY_OF_MONTH, 16);
			cal.set(Calendar.YEAR, 2014);
			if(tradeDate.after(cal.getTime())) {
				if(pos.getSymbol().equalsIgnoreCase("PFPT") || pos.getSymbol().equalsIgnoreCase("YY")) {
					System.err.print("");
				}
			}
			
			// sell on merger event
			if(sym.isMergerEvent()) {
				RunLogger.getRunLogger().logPortfolio(tradeDate + " Got Acquired or Merged: " + pos.getQuantity() + " shares of " + pos.getSymbol() + " for " + pos.getPercentGainLoss() );
				sellPositions.add(pos);
				continue;
			} 
			
			//NOTE: below assuming no partial SELLs (only partial BUYs)
			final List<TransactionData> sortedTxs = pos.getTransactionsSortedByTradeDate();
			if(sortedTxs == null) {
				continue;
			}
			
			// check the scaled stop-losses
			final Map<TransactionType, Integer> txCountMap = pos.getTransactionCount();
			final int buyCount = txCountMap.get(TransactionType.BUY) == null ? 0 : txCountMap.get(TransactionType.BUY);
			final int sellCount = txCountMap.get(TransactionType.SELL) == null ? 0 : txCountMap.get(TransactionType.SELL);
			
			final int numTxs = buyCount - sellCount;
			
			// scaled-stop-losses ensures profits are protected
			// this is done only for multi-transaction positions
			BigDecimal scaledStopLoss = null;
			if(numTxs > 2) {
				// 3 tx positions have higher scaled-stop-loss
				scaledStopLoss = pos.getCostBasisPerShare().multiply(new BigDecimal(1.01)); 
			} else if (numTxs > 1) {
				// 0.5% above cost-basis-per-shares for 2 tx positions
				scaledStopLoss = pos.getCostBasisPerShare().multiply(new BigDecimal(1.005));
			} 

				if(scaledStopLoss != null && sym.getPriceForDate(tradeDate).getClose().doubleValue() < scaledStopLoss.doubleValue()) {
					RunLogger.getRunLogger().logPortfolio(tradeDate + " StopLoss-ScaledSoftStop SELL: ($" + scaledStopLoss.doubleValue() + ")" + pos.getQuantity() + " @$"  + sym.getPriceForDate(tradeDate).getClose().doubleValue() + " shares of " + pos.getSymbol() + " for " + pos.getPercentGainLoss() );
					sellPositions.add(pos);
					continue;
				}
			
			final double pctChg = pos.getPercentGainLoss();
			if(pctChg <= stopLossPct_HardStop) {
				// this needs to be sold
				RunLogger.getRunLogger().logPortfolio(tradeDate + " StopLoss-HardStop SELL: (" + pctChg + "%) #" + pos.getQuantity() + " shares of " + pos.getSymbol() + " for " + pos.getPercentGainLoss() );
				sellPositions.add(pos);
				continue;
			} else if(pctChg <= stopLossPct_SoftStop) {
				// this needs to be NOT sold only if it DID NOT break 20ema support
				final PriceAnalysisData todaysPA = sym.getPriceAnalysisForDate(tradeDate);
				
				final PriceAnalysisElementData pae0= todaysPA.getAnalysisFor(ChartElementType.PRICE_MA_10);
				final PriceAnalysisElementData pae1 = todaysPA.getAnalysisFor(ChartElementType.PRICE_MA_20); 
				final PriceAnalysisElementData pae2= todaysPA.getAnalysisFor(ChartElementType.PRICE_MA_50);
				if(pae0 != null) {
					ChartAnalysisType cat = pae0.getAnalysisType();
						if(cat == ChartAnalysisType.BREAKING_UP || cat == ChartAnalysisType.SUPPORT) {
							continue;
						}
				}
				if(pae1 != null) {
					ChartAnalysisType cat = pae1.getAnalysisType();
						if(cat == ChartAnalysisType.BREAKING_UP  || cat == ChartAnalysisType.SUPPORT) {
							continue;
						}
				} 
				
				if(pae2 != null) {
					ChartAnalysisType cat = pae2.getAnalysisType();
					if(cat == ChartAnalysisType.BREAKING_UP  || cat == ChartAnalysisType.SUPPORT) {
						continue;
					}
				}
				
				final BigDecimal pctChgFrom20ema = todaysPA.getElementPctChg(ChartElementType.PRICE_MA_20);
				if(pctChgFrom20ema != null && pctChgFrom20ema.doubleValue()  > -0.5) {
					continue;
				}
				
				RunLogger.getRunLogger().logPortfolio(tradeDate + " StopLoss-SoftStop SELL: (" + pctChg + "%)" + pos.getQuantity() + " shares of " + pos.getSymbol() + " for " + pos.getPercentGainLoss() );
				sellPositions.add(pos);
				continue;
			}
		}
		
		if(sellPositions.size() > 0) {
			return sellPositionsAndUpdatePortfolioPositions(sellPositions, tradeDate);
		}
		
		return null;
	}
	
	private List<TransactionData> sellPositionsAndUpdatePortfolioPositions(List<PositionData> sellPositions, Date tradeDate) throws ApplicationException {
		final List<TransactionData> transactions = new ArrayList<TransactionData>();
		
		for(final PositionData aSellPosition : sellPositions) {
			final TransactionData tx = this.execModel.sell(aSellPosition.getMsId(), aSellPosition.getSymbol(), aSellPosition.getQuantity(), tradeDate);
			if(tx != null) {
				transactions.add(tx);
			}

			RunLogger.getRunLogger().logPortfolio((tradeDate + " "  + tx.getTransactionType() + " " + tx.getSymbol() + " @" + tx.getCostBasisPerShare().doubleValue() + " x" + tx.getQuantity() +  " for $" + tx.getTotalAmt().getValue().doubleValue() + " " + aSellPosition.getPercentGainLoss() + "%"));
			portfolio.addTransaction(tx);
		}
		
		return transactions;
	}

	private TransactionData buyPositionAndUpdatePortfolioPositions(long msid, String symbol, CurrencyData maxAmt, Date tradeDate) throws ApplicationException {
			final TransactionData tx = this.execModel.buy(msid, symbol, maxAmt, tradeDate);
			
			final PositionData aPos= portfolio.addTransaction(tx);			
			if(aPos != null) {
				RunLogger.getRunLogger().logPortfolio(tradeDate + " " + tx.getTransactionType() + " " + tx.getQuantity() + " shares of " + tx.getSymbol() + " @" + tx.getCostBasisPerShare().doubleValue() +  " for $" + tx.getTotalAmt().getValue().doubleValue() + ";cost-basis=" + aPos.getCostBasisPerShare() +";CASH-BAL=" + portfolio.getCashPosition().getCurrentValue().getValue().doubleValue());
			}
			return tx;
	}

	private TransactionData partialSellPositionAndUpdatePortfolioPositions(long msid, String symbol, int quantity, Date tradeDate) throws ApplicationException {
		final TransactionData tx = this.execModel.sell(msid, symbol, quantity, tradeDate);
		final PositionData aPos= portfolio.addTransaction(tx);
		if(aPos != null) {
			RunLogger.getRunLogger().logPortfolio(tradeDate + " partial-sell: " + tx.getQuantity() + " shares of " + tx.getSymbol() + " @" + tx.getCostBasisPerShare().doubleValue()  + " for $" + tx.getTotalAmt().getValue().doubleValue() + ";cost-basis=" + aPos.getCostBasisPerShare() + ";CASH-BAL=" + portfolio.getCashPosition().getCurrentValue().getValue().doubleValue());
		}
		return tx;
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

	/**
	 * @return the initialCash
	 */
	public long getInitialCash() {
		return initialCash;
	}

	/**
	 * @param initialCash the initialCash to set
	 */
	public void setInitialCash(long initialCash) {
		this.initialCash = initialCash;
	}

	/**
	 * @return the execModel
	 */
	public ExecutionModel getExecModel() {
		return execModel;
	}

	/**
	 * @return the marketTimer
	 */
	public WONMarketTimer getMarketTimer() {
		return marketTimer;
	}

	/**
	 * @param marketTimer the marketTimer to set
	 */
	public void setMarketTimer(WONMarketTimer marketTimer) {
		this.marketTimer = marketTimer;
	}

	/**
	 * @return the simpleMarketTimer
	 */
	public SimpleWONMarketTimer getSimpleMarketTimer() {
		return simpleMarketTimer;
	}

	/**
	 * @param simpleMarketTimer the simpleMarketTimer to set
	 */
	public void setSimpleMarketTimer(SimpleWONMarketTimer simpleMarketTimer) {
		this.simpleMarketTimer = simpleMarketTimer;
	}
}
