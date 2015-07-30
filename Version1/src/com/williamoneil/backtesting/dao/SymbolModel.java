/**
 * 
 */
package com.williamoneil.backtesting.dao;

import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Days;

import com.williamoneil.ApplicationException;
import com.williamoneil.backtesting.util.PriceAnalysisData;
import com.williamoneil.backtesting.util.PriceChartAnalyzer;

/**
 * @author Gudipati
 *
 */
public class SymbolModel {

	private Date asOfDate = null;
	private SymbolInfoModel symInfo = null;
	private SymbolHeaderInfoModel headerInfo = null;
	private List<InstrumentPriceModel> prices = null;
	
	// this only represents the split information for the requested date
	private SplitModel split = null;
	
	// this only represents the merger information for the requested date
	private boolean mergerEvent = false;
	
	private List<PriceAnalysisData> paList = null;
	
	public boolean isRecentIPO(final Date fromDate) {
		if(headerInfo != null && headerInfo.getIpoDt() != null) {
			final int ipoDaysAgo = Math.abs(Days.daysBetween(new DateTime(headerInfo.getIpoDt()), new DateTime(fromDate)).getDays());
			if(ipoDaysAgo <= 365) {
				return true;
			}
		}
		
		return false;
	}
	
	//get the analyze the prices
	public List<PriceAnalysisData> getPriceAnalysisData() throws ApplicationException {
		if(paList == null) {
			paList = PriceChartAnalyzer.analyzeDailyPrices(prices);
		}
		
		return paList;
	}
	
	public InstrumentPriceModel getPriceForNDaysBeforeDate(final int numDaysBack, final Date tradeDt) {
		if(prices == null || prices.size() == 0) {
			return null;
		}
		
		int index = 0;
		Integer foundIndex = null;
		Integer daysDiff = null;
		for(final InstrumentPriceModel ipm : prices) {
			final int daysDiffTemp = Math.abs(Days.daysBetween(new DateTime(ipm.getPriceDate()), new DateTime(tradeDt)).getDays());
			if(daysDiffTemp == 0) {
				foundIndex = ++index;
				break;
			} else {
				if(daysDiff == null) {
					daysDiff = daysDiffTemp;
					continue;
				} else if(Math.abs(daysDiff) < Math.abs(daysDiffTemp)){
					// prices are ordered and the date diff is growing - so no need to exhaust search remaining dates because date specified will never match
					return null;
				}
			}
			
			index++;
		}
		
		int i = 0;
		while(foundIndex != null && foundIndex-Math.abs(numDaysBack)-i > 0) {
			final InstrumentPriceModel ipm =  prices.get(foundIndex - Math.abs(numDaysBack) - i);
			if(ipm.getDateType() == TradeDateType.TRADING_DATE) {
				return ipm;
			}
			i++;
		}
		
		return null;
	}
	
	public InstrumentPriceModel getPriceForDate(final Date tradeDt) {
		if(prices == null || prices.size() == 0) {
			return null;
		}
		
		Integer daysDiff = null;
		for(final InstrumentPriceModel ipm : prices) {
			final int daysDiffTemp = Math.abs(Days.daysBetween(new DateTime(ipm.getPriceDate()), new DateTime(tradeDt)).getDays());
			if(daysDiffTemp == 0) {
				return ipm;
			} else {
				if(daysDiff == null) {
					daysDiff = daysDiffTemp;
					continue;
				} else if(Math.abs(daysDiff) < Math.abs(daysDiffTemp)){
					// prices are ordered and the date diff is growing - so no need to exhaust search remaining dates because date specified will never match
					return null;
				}
			}
		}
		
		return null;
	}
	
	public PriceAnalysisData getPriceAnalysisForNDaysBeforeDate(final int numDaysBack, final Date tradeDt) throws ApplicationException {
		//  make the call to get-PA data to ensure we have created the analysis-data 
		this.getPriceAnalysisData();
		if(paList == null || paList.size() == 0) {
			return null;
		}
		
		int index = 0;
		Integer foundIndex = null;
		Integer daysDiff = null;
		for(final PriceAnalysisData pa : paList) {
			final int daysDiffTemp = Math.abs(Days.daysBetween(new DateTime(pa.getPrice().getPriceDate()), new DateTime(tradeDt)).getDays());
			if(daysDiffTemp == 0) {
				foundIndex = ++index;
				break;
			} else {
				if(daysDiff == null) {
					daysDiff = daysDiffTemp;
					continue;
				} else if(Math.abs(daysDiff) < Math.abs(daysDiffTemp)){
					// prices are ordered and the date diff is growing - so no need to exhaust search remaining dates because date specified will never match
					return null;
				}
			}
			
			index++;
		}
		
		int i = 0;
		while(foundIndex != null && foundIndex-Math.abs(numDaysBack)-i > 0) {
			final PriceAnalysisData ipm =  paList.get(foundIndex - Math.abs(numDaysBack) - i);
			if(ipm.getPrice().getDateType() == TradeDateType.TRADING_DATE) {
				return ipm;
			}
			i++;
		}
		
		return null;
	}
	
	public PriceAnalysisData getPriceAnalysisForDate(final Date tradeDt) throws ApplicationException {
		//  make the call to get-PA data to ensure we have created the analysis-data 
		this.getPriceAnalysisData();
		if(paList == null || paList.size() == 0) {
			return null;
		}
		
		Integer daysDiff = null;
		for(final PriceAnalysisData pa : paList) {
			final int daysDiffTemp = Math.abs(Days.daysBetween(new DateTime(pa.getPrice().getPriceDate()), new DateTime(tradeDt)).getDays());
			if(daysDiffTemp == 0) {
				return pa;
			} else {
				if(daysDiff == null) {
					daysDiff = daysDiffTemp;
					continue;
				} else if(Math.abs(daysDiff) < Math.abs(daysDiffTemp)){
					// prices are ordered and the date diff is growing - so no need to exhaust search remaining dates because date specified will never match
					return null;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * @return the symInfo
	 */
	public SymbolInfoModel getSymInfo() {
		return symInfo;
	}
	/**
	 * @param symInfo the symInfo to set
	 */
	public void setSymInfo(SymbolInfoModel symInfo) {
		this.symInfo = symInfo;
	}
	/**
	 * @return the headerInfo
	 */
	public SymbolHeaderInfoModel getHeaderInfo() {
		return headerInfo;
	}
	/**
	 * @param headerInfo the headerInfo to set
	 */
	public void setHeaderInfo(SymbolHeaderInfoModel headerInfo) {
		this.headerInfo = headerInfo;
	}
	/**
	 * @return the prices
	 */
	public List<InstrumentPriceModel> getPrices() {
		return prices;
	}
	/**
	 * @param prices the prices to set
	 */
	public void setPrices(List<InstrumentPriceModel> prices) {
		this.prices = prices;
	}

	/**
	 * @return the split
	 */
	public SplitModel getSplit() {
		return split;
	}

	/**
	 * @param split the split to set
	 */
	public void setSplit(SplitModel split) {
		this.split = split;
	}

	/**
	 * @return the asOfDate
	 */
	public Date getAsOfDate() {
		return asOfDate;
	}

	/**
	 * @param asOfDate the asOfDate to set
	 */
	public void setAsOfDate(Date asOfDate) {
		this.asOfDate = asOfDate;
	}

	/**
	 * @return the mergerEvent
	 */
	public boolean isMergerEvent() {
		return mergerEvent;
	}

	/**
	 * @param mergerEvent the mergerEvent to set
	 */
	public void setMergerEvent(boolean mergerEvent) {
		this.mergerEvent = mergerEvent;
	}
}
