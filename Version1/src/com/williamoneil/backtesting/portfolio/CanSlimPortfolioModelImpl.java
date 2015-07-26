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
	
	private int numOfPositions = 4;
	private long initialCash = 100000;
	private float marginPctAllowed = 50; 
	
	//includes margin assumptions
	private float maxPercentInvestedInUptrend = 100 + marginPctAllowed;
	private float maxPercentInvestedInCautiousUptrend = 75;
	private float maxPercentInvestedInDowntrend = 30;
	
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
		
		/*
		final Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, Calendar.NOVEMBER);
		cal.set(Calendar.DAY_OF_MONTH, 25);
		cal.set(Calendar.YEAR, 2009);
		if(cal.before(runDate)) {
			System.out.print("");
		}
		*/
		
		/// first update portfolio  positions with latest prices
		final HashMap<String, SymbolModel> symbolMap = updatePortfolioPositionsWithLatestPrices(runDate);
		
		// perform any sells because of stop-loss
		final List<TransactionData> stopLossSellTxs = this.checkAndPerformSellStopLossForPositions(runDate, symbolMap, marketTimeSignal.getMarketSignalType());
		if(stopLossSellTxs != null) {
			execs.addAll(stopLossSellTxs);
		}
		// perform any sells because of Technical Analysis
		final List<TransactionData> taSellTxs = this.checkAndPerformSellTechAnalysisForPositions(runDate, symbolMap);
		if(taSellTxs != null) {
			execs.addAll(taSellTxs);
		}
		
		logger.info(runDate + " MarketSignal: Buy=" + marketTimeSignal.getMarketSignalType() + "; MarketSignalDate=" + marketTimeSignal.getSignalDate());
			//act on market-time-signal
			final List<TransactionData> mtSellTxs = this.actOnMarketTimeSignal(runDate, symbolMap, marketTimeSignal.getMarketSignalType());
			if(mtSellTxs != null) {
				execs.addAll(mtSellTxs);
			}
			
			// perform secondary buys to positions only if market is not in downtrend
			final List<TransactionData> posSecondaryBuySellTxs = performSecondaryBuySellChecks(runDate, symbolMap, marketTimeSignal.getMarketSignalType());
			if(posSecondaryBuySellTxs != null) {
				execs.addAll(posSecondaryBuySellTxs);
			}
		
			// if we are over-invested then reset the num-of-positions-open-to-buy
			
			float maxPctInvested = 0;
			if(marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.DOWNTREND) {
				maxPctInvested = this.maxPercentInvestedInDowntrend;
			} else if(marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.CAUTIOUS_UPTREND) {
				maxPctInvested = this.maxPercentInvestedInCautiousUptrend;
			} else if(marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.UPTREND) {
				maxPctInvested = this.maxPercentInvestedInUptrend;
			}
			
		int numOfPositionsOpenToBuy = 0;
		
		final BigDecimal maxAmtPerPosition = this.getMaxBuyAmountAllowedForPosition();
		BigDecimal cashBal = BigDecimal.ZERO;
		int minNumOfPositionsOpenToBuy = 0;
		
		
		final double pctInvestedCurrent = this.getPortfolioData().getPercentInvested();
		if(pctInvestedCurrent >= maxPctInvested) {
			numOfPositionsOpenToBuy = 0;
			minNumOfPositionsOpenToBuy = 0;
		} else {
			if(this.marginPctAllowed > 0) {
				final BigDecimal currPortInvestedValue = this.getPortfolioData().getTotalPortfolioValue().getValue();
				cashBal = currPortInvestedValue.multiply(new BigDecimal((maxPctInvested - pctInvestedCurrent) / 100));
			} else {
				cashBal = this.getPortfolioData().getCashPosition().getCurrentValue().getValue();
			}
		}
		numOfPositionsOpenToBuy = (int) Math.round(cashBal.doubleValue() / maxAmtPerPosition.doubleValue());
		minNumOfPositionsOpenToBuy = numOfPositionsOpenToBuy;
		
		
		/*
		if(cashBal.doubleValue() > 0) {
			if(marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.UPTREND ) {
				numOfPositionsOpenToBuy = 2 * minNumOfPositionsOpenToBuy;
				if(this.marginPctAllowed > 0 && numOfPositionsOpenToBuy > 1) {
					numOfPositionsOpenToBuy = numOfPositionsOpenToBuy - 1;
				}
				//minNumOfPositionsOpenToBuy = minNumOfPositionsOpenToBuy;
			} else if(marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.CAUTIOUS_UPTREND) {
				numOfPositionsOpenToBuy = minNumOfPositionsOpenToBuy;
				minNumOfPositionsOpenToBuy = (int) Math.round(0.5 * minNumOfPositionsOpenToBuy);
			} else if(marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.DOWNTREND) {
				numOfPositionsOpenToBuy = (int) Math.round (0.25 * minNumOfPositionsOpenToBuy);
				minNumOfPositionsOpenToBuy = 0;
			}
		} else {
			RunLogger.getRunLogger().logPortfolio(runDate + ": Cash Bal was negative: " + cashBal.doubleValue() + ";total-positions:" + getPortfolioData().getCurrentNumOfPosition()); 
		}
		*/
		
		
		/*
		int numOfPositionsOpenToBuy = this.numOfPositions - portfolio.getTotalNumOfPositions();
		if(numOfPositionsOpenToBuy < 0) {
			throw new RuntimeException("This just happened. Fix this please");
		} else {
			// if market is in downtrend we dont buy anything more than half the positions
			if(marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.UPTREND 
					|| marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.CAUTIOUS_UPTREND) {
				
			} else if(marketTimeSignal.getMarketSignalType() == SimpleMarketSignalType.DOWNTREND) {
				numOfPositionsOpenToBuy = (Math.round(numOfPositions / 2) - portfolio.getTotalNumOfPositions() + 1);
				if(numOfPositionsOpenToBuy < 0) {
					numOfPositionsOpenToBuy = 0;
				}
			} 
		}
		*/
		

		
		final List <SignalData> unProcessedSignals = new ArrayList<SignalData>();
		final List<SignalData> weakSignals = new ArrayList<SignalData>();
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
					if(!aSignal.isStrongSignal()) {
						// weak signals to be processed only if we exhaust strong-signals
						weakSignals.add(aSignal);
						continue;
					}					
					if(cannotProcessAnyMoreSignals) {
						unProcessedSignals.add(aSignal);
						continue;
					}
					
					final PositionData aPos = portfolio.getPositionForSymbol(aSignal.getSymbolInfo().getSymbol());
					if(aPos != null) {
						logger.info("Got signal for existing positon : " + aSignal.getSymbolInfo().getSymbol());
						continue;
					}
					
					if(newPositionsBought == numOfPositionsOpenToBuy) {
						RunLogger.getRunLogger().logPortfolio(runDate + " max new positions (" + newPositionsBought + ") reached. Checking for swap-outs.");
						
						final List<TransactionData> swapOutTxs = swapOutUnderPerformingPositionForSignal(runDate, aSignal, symbolMap);
						if(swapOutTxs == null || swapOutTxs.size() == 0) {
							RunLogger.getRunLogger().logPortfolio(runDate + " nothing left to swap out");
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
							final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aSignal.getSymbolInfo().getSymbol(), maxCashAvailForExecution, runDate);
							execs.add(trans);
					
							newPositionsBought++;
						} else {
							RunLogger.getRunLogger().logPortfolio(runDate + " cannot buy any-more positons for signal: " + aSignal.getSymbolInfo().getSymbol());
						}
					}
				}
				
				//reset this
				cannotProcessAnyMoreSignals = false;
				
				// we processed all strong signals.. lets see if we still have room for processing weak-signals
				if(newPositionsBought <  minNumOfPositionsOpenToBuy) {
					for(final SignalData aSignal : weakSignals) {
						if(cannotProcessAnyMoreSignals) {
							unProcessedSignals.add(aSignal);
							continue;
						}
						
						final PositionData aPos = portfolio.getPositionForSymbol(aSignal.getSymbolInfo().getSymbol());
						if(aPos != null) {
							logger.info("Got signal for existing positon : " + aSignal.getSymbolInfo().getSymbol());
							continue;
						}
					
						if(newPositionsBought >= minNumOfPositionsOpenToBuy) {
							// NO SWAP OUT logic for weak signals
							cannotProcessAnyMoreSignals = true;
							continue;
						}
						
						final CurrencyData maxCashAvailForExecution = getMaxBuyAmountAllowedForExecution(aPos);
						if(maxCashAvailForExecution != null && maxCashAvailForExecution.getValue().compareTo(BigDecimal.ZERO) > 0) {
							final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aSignal.getSymbolInfo().getSymbol(), maxCashAvailForExecution, runDate);
							execs.add(trans);
							RunLogger.getRunLogger().logPortfolio(runDate + " - bought from weak-signal: " + aSignal.getSymbolInfo().getSymbol());
							newPositionsBought++;
						} else {
							RunLogger.getRunLogger().logPortfolio(runDate + " cannot buy any-more positons for weak-signal: " + aSignal.getSymbolInfo().getSymbol());
						}
					}
				}
			}
			
			if(unProcessedSignals.size() > 0) {
				this.alphaModel.unProcessedSignals(unProcessedSignals, runDate);
			}
		
			RunLogger.getRunLogger().logPortfolio(runDate + " net-val=" + getPortfolioData().getTotalPortfolioValue().getValue().doubleValue()  + "; cashbal=" + getPortfolioData().getCashPosition().getCurrentValue().getValue().doubleValue() + "; num-po=" + getPortfolioData().getCurrentNumOfPosition()  + "; market-dir=" + marketTimeSignal.getMarketSignalType() + "; positions=" + this.portfolio.getPositionsAndPctChg());

		return execs;
	}

	private List<TransactionData> swapOutUnderPerformingPositionForSignal(final Date runDate, final SignalData signal, final HashMap<String, SymbolModel> symbolMap) throws ApplicationException {
		final List<PositionData> positions = portfolio.getPositions();
		if(positions.size() == 0) {
			return null;
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
			final SymbolModel sym = symbolMap.get(aPos.getSymbol());
			if(sym == null || sym.getPriceForDate(runDate) == null) {
				continue;
			}

			final PriceAnalysisData pa = sym.getPriceAnalysisForDate(runDate);
			if(pa == null) {
				continue;
			}
			
			final TransactionData latestBuyTx = aPos.getLatestTransaction(TransactionType.BUY);
			final List<TransactionData> allTxs = aPos.getTransactions();
			final TransactionData firstTx = allTxs.get(0);
			
			final int daysDiffFromLatestBuy = Math.abs(Days.daysBetween(new DateTime(latestBuyTx.getTransDt()), new DateTime(runDate)).getDays());
			final int daysDiffFromFirstBuy = Math.abs(Days.daysBetween(new DateTime(firstTx.getTransDt()), new DateTime(runDate)).getDays());
			/*
			if(daysDiffFromLatestBuy < 10) {
				continue;
			} else if (daysDiffFromFirstBuy < 15 && aPos.getPercentGainLoss() > 0) {
					continue;
			} else if (daysDiffFromFirstBuy < 20 && aPos.getPercentGainLoss() > 0.5) {
				continue;
			} else if (daysDiffFromFirstBuy < 25 && aPos.getPercentGainLoss() > 1) {
				continue;
			} else  if(daysDiffFromFirstBuy < 30 && aPos.getPercentGainLoss() > 1.5) {
				continue;
			} else  if(daysDiffFromFirstBuy < 35 && aPos.getPercentGainLoss() > 2) {
				continue;
			} else  if(daysDiffFromFirstBuy < 40 && aPos.getPercentGainLoss() > 2.5) {
				continue;
			} else  if(daysDiffFromFirstBuy < 45 && aPos.getPercentGainLoss() > 3) {
				continue;
			} else  if(daysDiffFromFirstBuy < 50 && aPos.getPercentGainLoss() > 3.5) {
				continue;
			} else  if(daysDiffFromFirstBuy < 55 && aPos.getPercentGainLoss() > 4) {
				continue;
			} else  if(daysDiffFromFirstBuy < 60 && aPos.getPercentGainLoss() > 4.5) {
				continue;
			} else  if(daysDiffFromFirstBuy < 65 && aPos.getPercentGainLoss() > 5) {
				continue;
			} else  if(daysDiffFromFirstBuy < 70 && aPos.getPercentGainLoss() > 5.5) {
				continue;
			} else if(daysDiffFromFirstBuy < 75 && aPos.getPercentGainLoss() > 6) {
				continue;
			} else if(daysDiffFromFirstBuy < 80 && aPos.getPercentGainLoss() > 6.5) {
				continue;
			} else if(daysDiffFromFirstBuy < 85 && aPos.getPercentGainLoss() > 7) {
				continue;
			} else if(daysDiffFromFirstBuy < 90 && aPos.getPercentGainLoss() > 7.5) {
				continue;
			} else if(daysDiffFromFirstBuy < 95 && aPos.getPercentGainLoss() > 8) {
				continue;
			} else if(daysDiffFromFirstBuy < 100 && aPos.getPercentGainLoss() > 8.5) {
				continue;
			} else if(daysDiffFromFirstBuy < 105 && aPos.getPercentGainLoss() > 9) {
				continue;
			} else if(daysDiffFromFirstBuy < 110 && aPos.getPercentGainLoss() > 9.5) {
				continue;
			} else if(daysDiffFromFirstBuy < 115 && aPos.getPercentGainLoss() > 10) {
				continue;
			} else if(daysDiffFromFirstBuy < 120 && aPos.getPercentGainLoss() > 10.5) {
				continue;				
			}else if (daysDiffFromFirstBuy >= 120 && aPos.getPercentGainLoss() > 11) {
				continue;
			}
			*/
			if(daysDiffFromLatestBuy < 10) {
				continue;
			} else if (daysDiffFromFirstBuy < 15 && aPos.getPercentGainLoss() > 0) {
					continue;
			} else  if(daysDiffFromFirstBuy < 30 && aPos.getPercentGainLoss() > 1) {
				continue;
			} else  if(daysDiffFromFirstBuy < 45 && aPos.getPercentGainLoss() > 2) {
				continue;
			} else  if(daysDiffFromFirstBuy < 60 && aPos.getPercentGainLoss() > 3) {
				continue;
			} else if(daysDiffFromFirstBuy < 75 && aPos.getPercentGainLoss() > 4) {
				continue;
			} else if(daysDiffFromFirstBuy < 90 && aPos.getPercentGainLoss() > 5) {
				continue;
			} else if(daysDiffFromFirstBuy < 105 && aPos.getPercentGainLoss() > 6) {
				continue;
			} else if(daysDiffFromFirstBuy < 120 && aPos.getPercentGainLoss() > 7) {
				continue;
			} else if(daysDiffFromFirstBuy < 135 && aPos.getPercentGainLoss() > 8) {
				continue;
			} else if(daysDiffFromFirstBuy < 150 && aPos.getPercentGainLoss() > 9) {
				continue;
			}else if (daysDiffFromFirstBuy >= 150 && aPos.getPercentGainLoss() > 10) {
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
			
			if(aPos.getSymbol().equalsIgnoreCase("sbux")) {
				System.err.print("sbux");
			}
			RunLogger.getRunLogger().logPortfolio(runDate + " Selling due to underperformance: " + aPos.getSymbol() + " " + aPos.getPercentGainLoss() + "% in " + daysDiffFromFirstBuy);
			final List<PositionData> sellPositions = new ArrayList<PositionData>();
			sellPositions.add(aPos);
			final List<TransactionData> swapOutAndInTxs= sellPositionsAndUpdatePortfolioPositions(sellPositions, runDate);
			if(swapOutAndInTxs != null && swapOutAndInTxs.size() > 0) {
				final TransactionData inTx = this.buyPositionAndUpdatePortfolioPositions(signal.getSymbolInfo().getSymbol(), this.getMaxBuyAmountAllowedForExecution(null), runDate); 
				swapOutAndInTxs.add(inTx);
				return swapOutAndInTxs;
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
			} else if (buyCount-sellCount >= 3) {
				maxCashAvailForExecution = null;
				//maxCashAvailForExecution = CurrencyData.instantiate(this.thirdPurchaseAmt);
			} else if (buyCount >= 8) {
				maxCashAvailForExecution = null;
			}
		}
		
		return maxCashAvailForExecution;
	}
	
	/*
	 * Check for Secondary Buy/Sell transactions on existing positions using market-timing conditions 
	 */
	private List<TransactionData> performSecondaryBuySellChecks(final Date runDate, final HashMap<String, SymbolModel> symbolMap, SimpleMarketSignalType marketSignal) throws ApplicationException {
		final List<PositionData> positions = portfolio.getPositions();
		if(positions == null || positions.size() == 0 || marketSignal == null) {
			return null;
		}
		
		final List<TransactionData> txs = new ArrayList<TransactionData>();
		
		// secondary positions are purchased only if market is NOT in DOWNTREND
		//if(marketSignal != SimpleMarketSignalType.DOWNTREND) {
			for(PositionData aPos: positions) {
				final SymbolModel sym = symbolMap.get(aPos.getSymbol());
				if(sym == null || sym.getPriceForDate(runDate) == null) {
					continue;
				}
				
				final CurrencyData maxAllowedAmtForPurchase = this.getMaxBuyAmountAllowedForExecution(aPos);
				if(maxAllowedAmtForPurchase == null || maxAllowedAmtForPurchase.getValue().compareTo(BigDecimal.ZERO) <= 0) {
					// no more purchases allowed-- move on to next position
					continue;
				}
				
				final Date latestTxDt = aPos.getLatestTransactionDate(null);
				final int daysSinceLastPurchase = Math.abs(Days.daysBetween(new DateTime(latestTxDt), new DateTime(runDate)).getDays());
				if(daysSinceLastPurchase < 5) {
					// need at least 5 days since last purchase to do any secondary purchases
					continue;
				}
				
				final TransactionData lastBuyTx = aPos.getLatestTransaction(TransactionType.BUY);
				
				final double pctChg = aPos.getPercentGainLoss();
				final double pctChgSinceLastBuy = Helpers.getPercetChange(sym.getPriceForDate(runDate).getClose(), lastBuyTx.getCostBasisPerShare()).doubleValue();

				final double pctChgInUpTrend = 3;
				final double pctChgInCautiousUpTrend = 4;
				final double pctChgInDownTrend = 5;

				final double pctChgSinceLastBuyInUpTrend = 1;
				final double pctChgSinceLastBuyInCautiousUpTrend = 1.5;
				final double pctChgSinceLastBuyInDownTrend = 2;
				
				final PriceAnalysisData pa = sym.getPriceAnalysisForDate(runDate);
				if(pa== null){
					continue;
				}
				final PriceAnalysisElementData pae0= pa.getAnalysisFor(ChartElementType.PRICE_MA_10);
				final double pctChgFrom10emaInUpTrend = 7;

				final PriceAnalysisElementData pae1 = pa.getAnalysisFor(ChartElementType.PRICE_MA_20);
				final double pctChgFrom20emaInUpTrend = 5;
				final double pctChgFrom20emaInCautiousUpTrend = 6;
				
				final PriceAnalysisElementData pae2= pa.getAnalysisFor(ChartElementType.PRICE_MA_50);
				final double pctChgFrom50smaInUpTrend = 3;
				final double pctChgFrom50smaInCautiousUpTrend = 4;
				final double pctChgFrom50smaInDowntrend = 5;

				if(marketSignal == SimpleMarketSignalType.UPTREND) {
					if(pctChg < pctChgInUpTrend || pctChgSinceLastBuy < pctChgSinceLastBuyInUpTrend) {
						// need at least 5% gains overall and 2.5% chg from last purchase before proceeding with purchase
						continue;
					}
					
					if(pae0 != null) {
						ChartAnalysisType cat = pae0.getAnalysisType();
							if(pctChg > pctChgFrom10emaInUpTrend && cat == ChartAnalysisType.BREAKING_UP) {
								if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice10dEma()).doubleValue() <= 1.5) {
									final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
									txs.add(trans);
									continue;
								}
							}
					} 
					if(pctChg >= pctChgFrom20emaInUpTrend && pae1 != null) {
						ChartAnalysisType cat = pae1.getAnalysisType();
							if(cat == ChartAnalysisType.BREAKING_UP) {
								if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice20dEma()).doubleValue() <= 1.5) {
									final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
									txs.add(trans);
									continue;
								}
							}
					} 
					if(pctChg >= pctChgFrom50smaInUpTrend && pae2 != null) {
						ChartAnalysisType cat = pae2.getAnalysisType();
						if(cat == ChartAnalysisType.BREAKING_UP) {
							if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice50dSma()).doubleValue() <= 1.5) {
								final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
								txs.add(trans);
								continue;
							}
						}
					}
				} else if(marketSignal == SimpleMarketSignalType.CAUTIOUS_UPTREND) {
					if(pctChg < pctChgInCautiousUpTrend || pctChgSinceLastBuy < pctChgSinceLastBuyInCautiousUpTrend) {
						// need at least 5% gains overall and 2.5% chg from last purchase before proceeding with purchase
						continue;
					}
					
					if(pctChg >= pctChgFrom20emaInCautiousUpTrend && pae1 != null) {
						ChartAnalysisType cat = pae1.getAnalysisType();
							if(cat == ChartAnalysisType.BREAKING_UP) {
								if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice20dEma()).doubleValue() <= 1.5) {
									final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
									txs.add(trans);
									continue;
								}
							}
					} 
					if(pctChg >= pctChgFrom50smaInCautiousUpTrend && pae2 != null) {
						ChartAnalysisType cat = pae2.getAnalysisType();
						if(cat == ChartAnalysisType.BREAKING_UP) {
							if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice50dSma()).doubleValue() <= 1.5) {
								final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
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
					
					if(pctChg >= pctChgFrom50smaInDowntrend && pae2 != null) {
						ChartAnalysisType cat = pae2.getAnalysisType();
						if(cat == ChartAnalysisType.BREAKING_UP) {
							if(Helpers.getPercetChange(pa.getPrice().getClose(), pa.getPrice50dSma()).doubleValue() <= 1.5) {
								final TransactionData trans = buyPositionAndUpdatePortfolioPositions(aPos.getSymbol(), maxAllowedAmtForPurchase, runDate);
								txs.add(trans);
								continue;
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
	private List<TransactionData> actOnMarketTimeSignal(Date runDate, HashMap<String, SymbolModel> symbolMap, SimpleMarketSignalType marketSignal) throws ApplicationException {
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
				final SymbolModel sym = symbolMap.get(aPos.getSymbol());
				if(sym == null) {
					throw new RuntimeException("sym-model is empty for position symbol-" + aPos.getSymbol());
				}
				
				final PriceAnalysisData pa = sym.getPriceAnalysisForDate(runDate);
				if(pa == null) {
					continue;
				}
				
				final int daysDiffFromLatestBuy = Math.abs(Days.daysBetween(new DateTime(aPos.getLatestTransactionDate(null)), new DateTime(runDate)).getDays());
				final double pctChg = aPos.getPercentGainLoss();
				final BigDecimal pctChgFrom20dEma = pa.getElementPctChg(ChartElementType.PRICE_MA_20);
				if(daysDiffFromLatestBuy >= 5 && pctChg < 1 && pctChgFrom20dEma != null && pctChgFrom20dEma.doubleValue() < -2 && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
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
				final SymbolModel sym = symbolMap.get(aPos.getSymbol());
				if(sym == null) {
					throw new RuntimeException("sym-model is empty for position symbol-" + aPos.getSymbol());
				}
				
				final PriceAnalysisData pa = sym.getPriceAnalysisForDate(runDate);
				if(pa== null) {
					continue;
				}
				if(aPos.getPercentGainLoss() < 2) { // <2%
					final BigDecimal pctChgFrom20dEma = pa.getElementPctChg(ChartElementType.PRICE_MA_20);
					if(pctChgFrom20dEma != null && pctChgFrom20dEma.doubleValue() <= -1.5 && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
						final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
						// add to sell-list only on a red-day
						if(prevDayPrice.getClose().doubleValue() >  pa.getPrice().getClose().doubleValue()) {
							RunLogger.getRunLogger().logPortfolio(runDate + " Market-Downtrend-Sell 20EMA: (" + pctChgFrom20dEma.doubleValue() + "%)" + aPos.getQuantity() + " shares of " + aPos.getSymbol() + " for " + aPos.getPercentGainLoss());
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
				
				if(aPos.getPercentGainLoss() <= -3 && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
					final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
					// add to sell-list only on a red-day
					if(prevDayPrice.getClose().doubleValue() >  pa.getPrice().getClose().doubleValue()) {
						RunLogger.getRunLogger().logPortfolio(runDate + "Market-Downtrend-Sell -3%: (" + aPos.getPercentGainLoss() + "%)" + aPos.getQuantity() + " shares of " + aPos.getSymbol() + " for " + aPos.getPercentGainLoss());
						sellPositions.add(aPos);
						symbolsIdentifiedAsSell.add(aPos.getSymbol());
						continue;
					}
				}
			}
		}  else {
			// market-cautious-uptrend.. this is where we do underperforming sells and TA sells
			for(PositionData aPos: sortedPos) {
				final SymbolModel sym = symbolMap.get(aPos.getSymbol());
				if(sym == null) {
					throw new RuntimeException("sym-model is empty for position symbol-" + aPos.getSymbol());
				}
				
				final PriceAnalysisData pa = sym.getPriceAnalysisForDate(runDate);
							
				//final int daysFromLastTx = Math.abs(Days.daysBetween(new DateTime(runDate), new DateTime(aPos.getLatestTransactionDate(TransactionType.BUY))).getDays());
				
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
				
				final int daysDiffFromLatestBuy = Math.abs(Days.daysBetween(new DateTime(aPos.getLatestTransactionDate(null)), new DateTime(runDate)).getDays());
				final double pctChg = aPos.getPercentGainLoss();
				final BigDecimal pctChgFrom20dEma = pa.getElementPctChg(ChartElementType.PRICE_MA_20);
				if(daysDiffFromLatestBuy >= 5 && pctChg < 1.5 && pctChgFrom20dEma != null && pctChgFrom20dEma.doubleValue() < -2 && !symbolsIdentifiedAsSell.contains(aPos.getSymbol())) {
					final InstrumentPriceModel prevDayPrice = sym.getPriceForNDaysBeforeDate(1, runDate);
					// add to sell-list only on a red-day
					if(prevDayPrice.getClose().doubleValue() >  pa.getPrice().getClose().doubleValue()) {
						RunLogger.getRunLogger().logPortfolio(runDate + " Market-CautiousUp-Sell 20EMA: (" + pctChgFrom20dEma.doubleValue() + "%)" + aPos.getQuantity() + " shares of " + aPos.getSymbol() + " for " + aPos.getPercentGainLoss());
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
	private HashMap<String, SymbolModel> updatePortfolioPositionsWithLatestPrices(Date runDate) throws ApplicationException {
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(runDate);
		startCal.add(Calendar.DATE, -1 * Helpers.PRICE_HISTORY_LOOKUP_DAYSBACK);
		
		portfolio.setDate(runDate);
		
		final HashMap<String, SymbolModel> symbolMap = new HashMap<String, SymbolModel>();
		
		final List<PositionData> positions = this.portfolio.getPositions();
		for(final PositionData pos : positions) {
			final SymbolModel posSym = this.wonDAO.getSymbolData(pos.getSymbol(), startCal.getTime(), runDate, PeriodicityType.DAILY);
			if(posSym == null) {
				logger.warn(runDate + " Cannot find the symbol data for symbol:" + pos.getSymbol());
				continue;
			}
			symbolMap.put(pos.getSymbol(), posSym);
			
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
	
	private List<TransactionData> checkAndPerformSellTechAnalysisForPositions(final Date tradeDate, final HashMap<String, SymbolModel> symbolsMap) throws ApplicationException {
		final List<PositionData> sellPositions = new ArrayList<PositionData>(); 
		// perform loss-sells
		for(final PositionData pos : this.portfolio.getPositions()) {
			final SymbolModel symModel = symbolsMap.get(pos.getSymbol());
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
			
			final BigDecimal prevAtr = prevPA.getAtr14d();
			final double closingRange = PriceChartAnalyzer.getClosingRange(currentPA.getPrice(), null);
			if(prevAtr != null && currentPA.getPrice().getClose().doubleValue() >= prevPA.getPrice().getClose().doubleValue() + 2 * prevAtr.doubleValue()) {
				RunLogger.getRunLogger().logPortfolio(tradeDate + " " +  pos.getSymbol() + " ATR Sell signal. atr=" + prevAtr.doubleValue() + "; vol-rate: " + currentPA.getElementPctChg(ChartElementType.VOL) + "% ;cls-rng=" + closingRange);
			}
		}
		
		if(sellPositions.size() > 0) {
			return sellPositionsAndUpdatePortfolioPositions(sellPositions, tradeDate);
		}
		
		return null;
	}
	
	private List<TransactionData> checkAndPerformSellStopLossForPositions(final Date tradeDate, final HashMap<String, SymbolModel> symbolMap, final SimpleMarketSignalType marketSignal) throws ApplicationException {
		final List<PositionData> sellPositions = new ArrayList<PositionData>(); 
		// perform loss-sells
		for(final PositionData pos : this.portfolio.getPositions()) {
			final SymbolModel sym = symbolMap.get(pos.getSymbol());
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
			final int numTxs = sortedTxs.size();
			
			// scaled-stop-losses ensures profits are protected
			// this is done only for multi-transaction positions
			BigDecimal scaledStopLoss = null;
			if(numTxs > 2) {
				// 3 tx positions have higher scaled-stop-loss
				scaledStopLoss = pos.getCostBasisPerShare().multiply(new BigDecimal(1.02)); 
			} else if (numTxs > 1) {
				// 1% above cost-basis-per-shares for 2 tx positions
				scaledStopLoss = pos.getCostBasisPerShare().multiply(new BigDecimal(1.01));
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
			final TransactionData tx = this.execModel.sell(aSellPosition.getSymbol(), aSellPosition.getQuantity(), tradeDate);
			if(tx != null) {
				transactions.add(tx);
			}

			RunLogger.getRunLogger().logPortfolio((tradeDate + " "  + tx.getTransactionType() + " " + tx.getSymbol() + " x" + tx.getQuantity() +  " for $" + tx.getTotalAmt().getValue().doubleValue() + " " + aSellPosition.getPercentGainLoss() + "%"));
			portfolio.addTransaction(tx);
		}
		
		return transactions;
	}

	private TransactionData buyPositionAndUpdatePortfolioPositions(String symbol, CurrencyData maxAmt, Date tradeDate) throws ApplicationException {
			final TransactionData tx = this.execModel.buy(symbol, maxAmt, tradeDate);
			final PositionData aPos= portfolio.addTransaction(tx);
			
			if(aPos != null) {
				RunLogger.getRunLogger().logPortfolio(tradeDate + " BUY: " + tx.getQuantity() + " shares of " + tx.getSymbol() + " cost-basis=" + aPos.getCostBasisPerShare() + " for $" + tx.getTotalAmt().getValue().doubleValue() + " CASH-BAL=" + portfolio.getCashPosition().getCurrentValue().getValue().doubleValue());
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
