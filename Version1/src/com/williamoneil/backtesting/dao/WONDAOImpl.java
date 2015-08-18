/**
 * 
 */
package com.williamoneil.backtesting.dao;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.williamoneil.ApplicationException;


/**
 * @author Gudipati
 *
 */
public class WONDAOImpl extends BaseDAOImpl {
	private static final Log _logger = LogFactory.getLog(WONDAOImpl.class);
	
	private DataSource lmDataSource = null;
	
	public InstrumentPriceModel getPriceTick(final long msid, PeriodicityType periodicity, final Date dt) throws ApplicationException {
		final SymbolInfoModel info = getSymbolInfoData(msid);
		if(info == null) {
			return null;
		}
		
	      final List<InstrumentPriceModel> prices = this.getPriceHistory(info.getInstrumentId(), dt, dt, periodicity, false, null);
	      if(prices == null || prices.size() == 0) {
	    	  return null;
	      }
	      
	      return prices.get(0);
	 }
	/*
	 public List<InstrumentPriceModel> getRecentPriceHistory(final String symbol, PeriodicityType periodicity) throws ApplicationException {
	    // first get price ticks for analysis
	      final Calendar startDt = Calendar.getInstance();
	      int numDaysBack = 1;
	      if(periodicity == PeriodicityType.DAILY || periodicity == PeriodicityType.WEEKLY) {
	        numDaysBack = 500; //close to year and half
	      }
	      startDt.add(Calendar.DATE, -numDaysBack);
	      
	      // default end-date is todays date
	      final Calendar endDt = Calendar.getInstance();
	      
	      return this.getPriceHistory(symbol, startDt.getTime(), endDt.getTime(), periodicity, false, null);
	    }
	*/

	public SymbolModel getSymbolData(long msid, Date startDt, Date endDt, PeriodicityType periodicity) throws ApplicationException {
		// first get symbol-info for symbol string
		final SymbolInfoModel symInfo = this.getSymbolInfoData(msid);
		if(symInfo == null) {
			return null;
		}
		
		final SymbolModel sym = new SymbolModel();
		sym.setAsOfDate(endDt);
		sym.setSymInfo(symInfo);
		
		sym.setPrices(getPriceHistory(symInfo.getInstrumentId(), startDt, endDt, periodicity, false, null));
		
		sym.setHeaderInfo(getSignalHeader(symInfo.getMsId(), endDt, periodicity));
		
		sym.setSplit(getSplitInfo(symInfo.getInstrumentId(), endDt));
		
		sym.setMergerEvent(getMergerEvent(symInfo.getInstrumentId(), endDt));
		
		return sym;
	}
	
	public boolean getMergerEvent(long osid , Date date) throws ApplicationException {
		Connection conn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;

		try
		{
			conn = super.getDataSource().getConnection();
			stmt = conn.prepareCall("select price from wondb.dbo.APPX_CorpEvents where osid=? and corpActionType='AnnounCEMENT' and date=? and displayValue in ('PM', 'CO', 'TO')");
			stmt.setLong(1, osid);
			stmt.setDate(2, new java.sql.Date(date.getTime()));
			
			rs = stmt.executeQuery();
			if (rs == null) return false;

			if (rs.next()) {
				return true;
			}

		} catch (SQLException sqlEx) {
			_logger.error(sqlEx.getErrorCode(), sqlEx);
			_logger.error("Error in get-merger-info for: " + osid, sqlEx);
			throw new ApplicationException(sqlEx);
		} finally {
			super.closeResources(conn, stmt, rs);
		}

		return false;
	}
	
	
	public SplitModel getSplitInfo(long osid , Date date) throws ApplicationException {
		Connection conn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;

		SplitModel data = null;
		try
		{
			conn = super.getDataSource().getConnection();
			stmt = conn.prepareCall("select sdate, sfac from wondb.dbo.splittable where osid=? and sdate=?");
			stmt.setLong(1, osid);
			stmt.setDate(2, new java.sql.Date(date.getTime()));
			
			rs = stmt.executeQuery();
			if (rs == null) return null;

			if (rs.next()) {
				data = new SplitModel();
				data.setSplitDate(rs.getDate(1));
				data.setSplitFactor(rs.getBigDecimal(2));
				return data;
			}

		} catch (SQLException sqlEx) {
			// _logger.error(sqlEx.getErrorCode(), sqlEx);
			_logger.error("Error in get split-info for osid: " + osid, sqlEx);
			throw new ApplicationException(sqlEx);
		} finally {
			super.closeResources(conn, stmt, rs);
		}

		return data;
	}
	
	public SymbolHeaderInfoModel getSignalHeader(long msId , Date date, PeriodicityType periodicity) throws ApplicationException {
		Connection conn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;

		SymbolHeaderInfoModel data = null;
		try {
			conn = super.getDataSource().getConnection();
			stmt = conn.prepareCall("{call RayHistory.dbo.RAY_GetStockHeaderInfo(?,?,?)}");
			stmt.setLong(1, msId);
			stmt.setInt(2, periodicity.getDbValue());
			stmt.setDate(3, new java.sql.Date(date.getTime()));
			

			rs = stmt.executeQuery();
			if (rs == null) return null;

			
			if (rs.next()) {
				data = new SymbolHeaderInfoModel();
				data.setSymbol(rs.getString(1));
				data.setCoName(rs.getString(2));
				data.setEpsDueDt(rs.getDate(3));
				data.setExchgName(rs.getString(4));
				data.setIndustryName(rs.getString(5));
				if (rs.getObject(6) != null) data.setMktCap(new BigDecimal(rs.getDouble(6) * 1000));
				if (rs.getObject(7) != null) data.setSales(rs.getBigDecimal(7));
				if (rs.getObject(8) != null) data.setSharesOutstnd(rs.getLong(8) * 100);
				if (rs.getObject(9) != null) data.setSharesFloat((long) (rs.getDouble(9) * 100));
				data.setIpoDt(rs.getDate(10));
				//ignore-11
				if (rs.getObject(12) != null) data.setOpenPrice(rs.getDouble(12));
				//ignore-13
				data.setIndustrySym(rs.getString(14));
				if (rs.getObject(15) != null) data.setAvgVol(rs.getLong(15) * 100);
				if (rs.getObject(16) != null) data.setVol(rs.getLong(16) * 100);
				data.setEpsEst(rs.getBigDecimal(17));
				data.setFfo(rs.getBoolean(18));
				data.setNav(rs.getBoolean(19));
				if (rs.getObject(20) != null) data.setPrevPrice(rs.getDouble(20));
				if (rs.getObject(21) != null) data.setCurrPrice(rs.getDouble(21));
				if (rs.getObject(22) != null) data.setGroupRank(rs.getInt(22));
				if (rs.getObject(23) != null) data.setPrevVol(rs.getLong(23) * 100);
				if (rs.getObject(24) != null) data.setSdRank(rs.getInt(24));
				if (rs.getObject(25) != null) data.setRsRank(rs.getInt(25));
				data.setAccDisRtg(rs.getString(26));
			}

		} catch (SQLException sqlEx) {
			// _logger.error(sqlEx.getErrorCode(), sqlEx);
			_logger.error("Error in getting StockHeader for msid-" + msId, sqlEx);
			throw new ApplicationException(sqlEx);
		} finally {
			super.closeResources(conn, stmt, rs);
		}

		return data;
	}
	
	public SymbolFundamentalsModel getSymbolFundamentals(long msId , Date date) throws ApplicationException {
		Connection conn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;

		Calendar startDt = Calendar.getInstance();
		startDt.setTime(date);
		startDt.add(Calendar.DATE, -5*365); // 5 years worth of fundamentals data
		
		SymbolFundamentalsModel data = null;
		try
		{
			conn = super.getDataSource().getConnection();
			stmt = conn.prepareCall("{call RayHistory.Ray.CFB_GetFinalEpsWithEntitlement(?,?,?,?,?,?)}");
			stmt.setLong(1, msId);
			stmt.setDate(2, new java.sql.Date(startDt.getTime().getTime()));
			stmt.setDate(3, new java.sql.Date(date.getTime()));
			stmt.setString(4, "weekly"); // this is not used
			stmt.setBoolean(5, false); //won-data or First call?
			stmt.setBoolean(6, true); // fc-entitled?
			
			rs = stmt.executeQuery();
			if (rs == null) return null;

			data = new SymbolFundamentalsModel();
			data.equals(date);
			
			final List<ReportedFundamentalsModel> reportedFundamentals = new ArrayList<ReportedFundamentalsModel>();
			data.setFundamentals(reportedFundamentals);
			
			while(rs.next()) {
				final ReportedFundamentalsModel fundamentals = new ReportedFundamentalsModel();
				
				fundamentals.setFiscalDate(rs.getDate(1));
				fundamentals.setReportedDate(rs.getDate(2));
				fundamentals.setSales(rs.getBigDecimal(3));
				fundamentals.setEps(rs.getBigDecimal(4));
				// ignore-5
				fundamentals.setAfterTaxMargin(rs.getBigDecimal(6));
				//ignore-7 and 8
				fundamentals.setEpsNonRecurringFlag(rs.getBoolean(9));
				fundamentals.setPeriodFlag(rs.getBoolean(10));
				fundamentals.setPriceOverSales(rs.getBigDecimal(11));
				fundamentals.setGrossMargin(rs.getBigDecimal(12));
				fundamentals.setRoe(rs.getBigDecimal(13));
				fundamentals.setCfOverEPS(rs.getBigDecimal(14));
				fundamentals.setEbitMargin(rs.getBigDecimal(15));
				fundamentals.setPreTaxMargin(rs.getBigDecimal(16));
				fundamentals.setFreeCfOverShares(rs.getBigDecimal(17));
				fundamentals.setEpsPctSurprise(rs.getBigDecimal(18));
				fundamentals.setActualFlag(rs.getBoolean(19));
				fundamentals.setEbita(rs.getBigDecimal(20));
				
				reportedFundamentals.add(fundamentals);
			}

		} catch (SQLException sqlEx) {
			// _logger.error(sqlEx.getErrorCode(), sqlEx);
			_logger.error("Error in get-StockHeader for: " + msId, sqlEx);
			throw new ApplicationException(sqlEx);
		} finally {
			super.closeResources(conn, stmt, rs);
		}

		if(data != null) {
			data.setFundsHoldings(getFundsHoldings(msId, startDt.getTime(), date));
		}
		
		if(data != null) {
			data.setInfo(getFundamentalsInfo(msId, date));
		}
		
		return data;
	}
	
	public SymbolFundamentalsInfoModel getFundamentalsInfo(long msId, Date date) throws ApplicationException {
		final SymbolFundamentalsInfoModel model = new SymbolFundamentalsInfoModel();
				
		Connection conn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;
		final String sql = "{ call RayHistory.dbo.Ray_GetSummaryBlock(?, ?,5) }";
		try{
			conn = super.getDataSource().getConnection();
			stmt = conn.prepareCall(sql);
			stmt.setLong(1, msId);
			stmt.setDate(2, new java.sql.Date(date.getTime()));
			
			boolean hasResults = stmt.execute();
			if(!hasResults) {
				return null;
			}
			
			// first is yearly-earnings-history
			rs = stmt.getResultSet();
			
			Double sales1 = null;
			if(rs.next()) {
				if(rs.getObject(2) != null) sales1 = rs.getDouble(2);
				if(rs.getObject(3) != null) model.setLatestAnnualEps(rs.getDouble(3));
				if(rs.getObject(4) != null) model.setLatestAnnualRoE(rs.getDouble(4));
				if(rs.getObject(5) != null) model.setLatestAnnualPreTaxMargin(rs.getDouble(5));
				if(rs.getObject(6) != null) model.setLatestAnnualCFS(rs.getDouble(6));
			}
			
			// we need the next row to calc the pct-chg
			Double sales2 = null;
			if(rs.next()) {
				if(rs.getObject(2) != null) sales2 = rs.getDouble(2);
				if(sales1 != null && sales2 != null && sales2 > 0) {
					model.setLatestAnnualSalesPctChg(100 * (sales1-sales2)/sales2);
				}
				
				Double eps2 = rs.getObject(3) == null ? null : rs.getDouble(3);
				if(model.getLatestAnnualEps() != null && eps2 != null && eps2 > 0) {
					model.setLatestAnnualEpsPctChg(100 * (model.getLatestAnnualEps() - eps2) / eps2);
				}
			}
			
			rs.close();
			
			//next is yearly-eps-estimates
			if(stmt.getMoreResults()) {
				rs = stmt.getResultSet();
				if(rs.next()) {
					if(rs.getObject(2) != null) model.setDgRating(rs.getInt(2));
					if(rs.getObject(5) != null) model.setAdRating(rs.getInt(5));
					if(rs.getObject(6) != null)model.setRsRank(rs.getInt(6));
					if(rs.getObject(7) != null)model.setGroupRank(rs.getInt(7));
				}
				
				rs.close();
			} else {
				return null;
			}
			
			if(stmt.getMoreResults()) {
				rs = stmt.getResultSet();
				if(rs.next()) {
					if(rs.getObject(8) != null) model.setEpsRank(rs.getInt(8));
				}
				
				rs.close();
			} else {
				return null;
			}
			
			if(stmt.getMoreResults()) {
				rs = stmt.getResultSet();
				if(rs.next()) {
					if(rs.getObject(1) != null)model.setFirstAnnualEpsEst(rs.getDouble(1));
					if(rs.getObject(2) != null)model.setSecondAnnualEpsEst(rs.getDouble(2));

					if(model.getLatestAnnualEps() != null && model.getFirstAnnualEpsEst() != null && model.getFirstAnnualEpsEst() > 0) {
						model.setFirstAnnualEpsPctChg(100 * (model.getFirstAnnualEpsEst() - model.getLatestAnnualEps()) / model.getLatestAnnualEps());
					}
					
					if(model.getFirstAnnualEpsEst() != null &&  model.getLatestAnnualEps() != null && model.getSecondAnnualEpsEst() != null && model.getSecondAnnualEpsEst() > 0) {
						model.setSecondAnnualEpsPctChg(100 * (model.getSecondAnnualEpsEst() - model.getLatestAnnualEps()) / model.getFirstAnnualEpsEst());
					}

					if(rs.getObject(5) != null)model.setTwoYrEPSGrowthRate(rs.getDouble(5));
					if(rs.getObject(6) != null)model.setFourYrEPSGrowthRate(rs.getDouble(6));
					if(rs.getObject(9) != null)model.setTwoYrSalesGrowthRate(rs.getDouble(9));
					if(rs.getObject(10) != null)model.setFourYrSalesGrowthRate(rs.getDouble(10));
					
					model.setSmrRating(rs.getString(11));
					if(rs.getObject(12) != null) model.setCompRating(rs.getInt(12));
				}
				
				rs.close();
			} else {
				return null;
			}
			
			if(stmt.getMoreResults()) {
				rs = stmt.getResultSet();
				if(rs.next()) {
					model.setEpsRank(rs.getInt(8));
				}
				
				rs.close();
			} else {
				return null;
			}
			
		}catch(SQLException sqlex) {
			sqlex.printStackTrace();
			throw new ApplicationException(sqlex);
		} finally {
			if(rs != null) {
				try{
					rs.close();
				}catch(Exception ex) {}
			}
			
			if(stmt != null) {
				try{
					stmt.close();
				}catch(Exception ex) {}
			}
			
			if(conn != null) {
				try{
					conn.close();
				}catch(Exception ex) {}
			}
		}
		
		return model;
	}
	
	
	public List<FundsHoldingsModel> getFundsHoldings(long msId, Date startDt, Date asOfDt) throws ApplicationException {
		Connection conn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;		
		try {
			conn = super.getDataSource().getConnection();
			stmt = conn.prepareCall("{call RayHistory.dbo.Ray_GetHoldingsCountFundView(?,?,?,?)} ");
			stmt.setLong(1, msId);
			stmt.setDate(2, new java.sql.Date(startDt.getTime()));
			stmt.setDate(3, new java.sql.Date(asOfDt.getTime()));
			stmt.setInt(4,1); // not sure what this is for
			
			if(!stmt.execute()) {
			  return null;
			}
			
			rs = stmt.getResultSet();
			final List<FundsHoldingsModel> fundHoldings = new ArrayList<FundsHoldingsModel>();
			while(rs.next()) {
				final FundsHoldingsModel aInfo = new FundsHoldingsModel();
				
				aInfo.setCalendarYear(rs.getInt(1));
				aInfo.setCalendarQtr(rs.getInt(2));
				
				if(rs.getObject(3) != null) aInfo.setNumOfFunds(rs.getInt(3));
				if(rs.getObject(4) != null) aInfo.setNumOfShares(rs.getLong(4));

				fundHoldings.add(aInfo);
			}	
			return fundHoldings;
		} catch(SQLException sqlEx) {
			_logger.error("Error getting fund-holdings for: " + msId,sqlEx);
			throw new ApplicationException(sqlEx);
		} finally {
			super.closeResources(conn, stmt, rs);
		}
	}
	/*
	public List<InstrumentPriceModel> getPriceHistory(String symbol, Date startDate, Date endDate, PeriodicityType periodicity, boolean bQuote, String currencyAbbr) throws ApplicationException {
		// first get symbol-info for symbol string
		final SymbolInfoModel symModel = this.getSymbolInfoData(symbol);
		if(symModel == null) {
			return null;
		}
		
		return getPriceHistory(symModel.getInstrumentId(), startDate, endDate, periodicity, bQuote, currencyAbbr);
	}
	*/
	public List<InstrumentPriceModel> getPriceHistory(long osid , Date startDate, Date endDate, PeriodicityType periodicity, boolean bQuote, String currencyAbbr) throws ApplicationException {
		Connection conn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;

		List<InstrumentPriceModel> ph = null;
		try
		{
			conn = this.getLmDataSource().getConnection();
			stmt = conn.prepareCall("{call wondb.wondata.GetStockDailyPrices(?,?,?,?) }");
			stmt.setLong(1, osid);
			stmt.setDate(2, new java.sql.Date(startDate.getTime()));
			stmt.setTimestamp(3, new java.sql.Timestamp(endDate.getTime()));
			stmt.setString(4, currencyAbbr);

			rs = stmt.executeQuery();
			if (rs == null) return null;

			ph = new ArrayList<InstrumentPriceModel>();
			while (rs.next()) {
				final InstrumentPriceModel ipm = new InstrumentPriceModel();
				ipm.setPriceDate(rs.getTimestamp(1));
				ipm.setHigh(rs.getBigDecimal(2));
				ipm.setLow(rs.getBigDecimal(3));
				ipm.setClose(rs.getBigDecimal(4));
				if (rs.getObject(5) != null) ipm.setVolume(rs.getLong(5));
				String dateType = rs.getString(6);
				if (dateType != null && dateType.equalsIgnoreCase("Holiday"))
					ipm.setDateType(TradeDateType.HOLIDAY);
				else
					ipm.setDateType(TradeDateType.TRADING_DATE);
				
				//if(ipm.getPriceDate().before((endDate))) {
					ph.add(ipm);
				//}
			}
			rs.close();
			
			// Add BATS records.
			if (bQuote) {
				if (stmt.getMoreResults(Statement.CLOSE_CURRENT_RESULT)) {
					rs = stmt.getResultSet();
					if (rs != null) {
						Date tm = new Date(0);
						if (!ph.isEmpty()) tm = ph.get(0).getPriceDate();
						while (rs.next()) {
							final InstrumentPriceModel ipm = new InstrumentPriceModel();
							ipm.setPriceDate(rs.getTimestamp(1));
							if (ipm.getPriceDate().equals(tm)) {
								InstrumentPriceModel ipm0 = ph.get(0);
								ipm0.setHigh(rs.getBigDecimal(2));
								ipm0.setLow(rs.getBigDecimal(3));
								ipm0.setClose(rs.getBigDecimal(4));
							} else if (ipm.getPriceDate().after(tm)) {
								ipm.setHigh(rs.getBigDecimal(2));
								ipm.setLow(rs.getBigDecimal(3));
								ipm.setClose(rs.getBigDecimal(4));
								ipm.setDateType(TradeDateType.TRADING_DATE);
								ph.add(0, ipm);
							}
						}
					}
				}
			}
		} catch (SQLException sqlEx) {
			// _logger.error(sqlEx.getErrorCode(), sqlEx);
			if (sqlEx.getErrorCode() == 0) {
				_logger.info("No records in getPriceHistory for: " + osid);
			} else {
				_logger.error("Error in getPriceHistory for: " + osid, sqlEx);
				throw new ApplicationException(sqlEx);
			}
		} finally {
			super.closeResources(conn, stmt, rs);
		}

		return ph;
	}
	
	public SymbolInfoModel getSymbolInfoDataForSymbol(final String symbol) throws ApplicationException {
		Connection conn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;		
		try {
			conn = super.getDataSource().getConnection();
			stmt = conn.prepareCall("SELECT i.msid, i.InstrumentID, i.InstrumentTypeID, i.symbol, i.name, i.status, i.tradingDateEarliest,i.tradingdatelatest, i.sedol, i.cusip, i.isin,  i.industrygroupcode,  i.GicsSubIndustryCode, i.story FROM Panaray.ref.Instrument i where i.symbol= ? ");
			stmt.setString(1, symbol);
			
			if(!stmt.execute()) {
			  return null;
			}
			
			rs = stmt.getResultSet();
			if(!rs.next()) {
			  return null;
			}
				
			final SymbolInfoModel aInfo = new SymbolInfoModel();
			aInfo.setMsId(rs.getLong(1));
			aInfo.setInstrumentId(rs.getLong(2));
			aInfo.setType(InstrumentType.fromDB(rs.getInt(3)));
			aInfo.setSymbol(rs.getString(4).trim());
			aInfo.setName(rs.getString(5));
			aInfo.setActive(rs.getString(6) != null && rs.getString(6).equalsIgnoreCase("A"));
			aInfo.setHistoryStartDt(rs.getDate(7));
			aInfo.setLastTradeDt(rs.getDate(8));
			aInfo.setSedol(rs.getString(9));
			aInfo.setCusip(rs.getString(10));
			aInfo.setIsin(rs.getString(11));
			aInfo.setGicsSubIndCode(rs.getString(12));
			aInfo.setIndCode(rs.getString(13));
			aInfo.setStory(rs.getString(14));
			
			return aInfo;
		} catch(SQLException sqlEx) {
			_logger.error("Error getting SymbolInfo for sym: " + symbol ,sqlEx);
			throw new ApplicationException(sqlEx);
		} finally {
			super.closeResources(conn, stmt, rs);
		}
	}
	
	public SymbolInfoModel getSymbolInfoData(final long msid) throws ApplicationException {
		Connection conn = null;
		CallableStatement stmt = null;
		ResultSet rs = null;		
		try {
			conn = super.getDataSource().getConnection();
			stmt = conn.prepareCall("SELECT i.msid, i.InstrumentID, i.InstrumentTypeID, i.symbol, i.name, i.status, i.tradingDateEarliest,i.tradingdatelatest, i.sedol, i.cusip, i.isin,  i.industrygroupcode,  i.GicsSubIndustryCode, i.story FROM Panaray.ref.Instrument i where i.msid= ? ");
			stmt.setLong(1, msid);
			
			if(!stmt.execute()) {
			  return null;
			}
			
			rs = stmt.getResultSet();
			if(!rs.next()) {
			  return null;
			}
				
			final SymbolInfoModel aInfo = new SymbolInfoModel();
			aInfo.setMsId(rs.getLong(1));
			aInfo.setInstrumentId(rs.getLong(2));
			aInfo.setType(InstrumentType.fromDB(rs.getInt(3)));
			aInfo.setSymbol(rs.getString(4).trim());
			aInfo.setName(rs.getString(5));
			aInfo.setActive(rs.getString(6) != null && rs.getString(6).equalsIgnoreCase("A"));
			aInfo.setHistoryStartDt(rs.getDate(7));
			aInfo.setLastTradeDt(rs.getDate(8));
			aInfo.setSedol(rs.getString(9));
			aInfo.setCusip(rs.getString(10));
			aInfo.setIsin(rs.getString(11));
			aInfo.setGicsSubIndCode(rs.getString(12));
			aInfo.setIndCode(rs.getString(13));
			aInfo.setStory(rs.getString(14));
			
			return aInfo;
		} catch(SQLException sqlEx) {
			_logger.error("Error getting Symbol for msid: " + msid ,sqlEx);
			throw new ApplicationException(sqlEx);
		} finally {
			super.closeResources(conn, stmt, rs);
		}
	}
	
	public List<BasePatternModel> getBreakOutBasesBetweenDates(final Date startDate, final Date endDate) throws ApplicationException {
		final String sql = "select i.symbol,b.BaseID,b.PeriodicityID,b.BaseStartDate,b.LeftSideHighDate,b.PivotPriceDate,b.PivotDate,b.BaseEndDate,b.BaseLength,b.BaseNumber,b.BaseStage,b.BaseStatusID,b.BaseTypeID,b.PivotPrice,b.instrumentid, i.msid "
					+ " from patternrecdb.dbo.base b, panaraymaster.panaray.ref.instrument i "
					+ " where b.versionid = (select top 1 pv.versionid from PatternRecDB.dbo.ProductVersion pv where pv.ProductCode = 3) "
					+ " and b.pivotdate >= ? and b.pivotDate < ?"
					+ " and b.instrumentid = i.instrumentid and i.instrumenttypeid = 1 and sc.countrycode = 1 ";
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try {
			conn = getLmDataSource().getConnection();
			stmt = conn.prepareStatement(sql);
			stmt.setDate(1, new java.sql.Date(startDate.getTime()));
			stmt.setDate(2, new java.sql.Date(endDate.getTime()));
			rs = stmt.executeQuery();
			
			final List<BasePatternModel> patterns = new  ArrayList<BasePatternModel>();
			while(rs.next()) {
				final BasePatternModel aPattern = new BasePatternModel();
				
				aPattern.setSymbol(rs.getString(1));
				aPattern.setBaseId(rs.getLong(2));
				aPattern.setDaily(rs.getInt(3) == 0);
				aPattern.setBaseStartDt(rs.getDate(4));
				// 5 ignored
				aPattern.setPivotPriceDt(rs.getDate(6));
				aPattern.setPivotDt(rs.getDate(7));
				aPattern.setBaseEndDt(rs.getDate(8));
				aPattern.setBaseLen(rs.getInt(9));
				aPattern.setBaseNum(rs.getInt(10));
				aPattern.setBaseStage(rs.getString(11));
				aPattern.setStatusType(BaseStatusType.fromDB(rs.getInt(12)));
				
				aPattern.setBaseType(BaseType.fromDB(rs.getInt(13)));
				aPattern.setPivotPrice(rs.getBigDecimal(14));
				aPattern.setOsid(rs.getLong(15));
				aPattern.setMsid(rs.getLong(16));
				
				patterns.add(aPattern);
			}
			return patterns;
		} catch(SQLException sqlEx) {
			_logger.error("Error getting breaking out patterns from date: " + startDate + " Error was: "  + sqlEx,sqlEx);
			throw new ApplicationException(sqlEx);
		} finally {
			super.closeResources(conn, stmt, rs);
		}
	}
	
	public BasePatternModel getBreakingOutBasesForOsidAndDate(final long osid, final Date date) throws ApplicationException {
		
		
		final String sql = "select i.symbol,b.BaseID,b.PeriodicityID,b.BaseStartDate,b.LeftSideHighDate,b.PivotPriceDate,b.PivotDate,b.BaseEndDate,b.BaseLength,b.BaseNumber,b.BaseStage,b.BaseStatusID,b.BaseTypeID,b.PivotPrice,b.instrumentid, i.msid"
					+ " from patternrecdb.dbo.base b, panaraymaster.panaray.ref.instrument i "
					+ " where b.versionid = (select top 1 pv.versionid from PatternRecDB.dbo.ProductVersion pv where pv.ProductCode = 3) "
					+ " and b.pivotdate=? and b.PeriodicityID=0 and b.instrumentid = ? "
					+ " and b.instrumentid=i.instrumentid and i.countrycode=1 and i.instrumenttypeid = 1 ";
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try {
			conn = getLmDataSource().getConnection();
			stmt = conn.prepareStatement(sql);
			stmt.setDate(1, new java.sql.Date(date.getTime()));
			stmt.setLong(2, osid);
			rs = stmt.executeQuery();
			
			if(rs.next()) {
				final BasePatternModel aPattern = new BasePatternModel();
				
				aPattern.setSymbol(rs.getString(1));
				aPattern.setBaseId(rs.getLong(2));
				aPattern.setDaily(rs.getInt(3) == 0);
				aPattern.setBaseStartDt(rs.getDate(4));
				// 5 ignored
				aPattern.setPivotPriceDt(rs.getDate(6));
				aPattern.setPivotDt(rs.getDate(7));
				aPattern.setBaseEndDt(rs.getDate(8));
				aPattern.setBaseLen(rs.getInt(9));
				aPattern.setBaseNum(rs.getInt(10));
				aPattern.setBaseStage(rs.getString(11));
				aPattern.setStatusType(BaseStatusType.fromDB(rs.getInt(12)));
				
				aPattern.setBaseType(BaseType.fromDB(rs.getInt(13)));
				aPattern.setPivotPrice(rs.getBigDecimal(14));
				aPattern.setOsid(rs.getLong(15));
				aPattern.setMsid(rs.getLong(16));
				
				return aPattern;
			}
		} catch(SQLException sqlEx) {
			_logger.error("Error getting breaking out patterns for osid+date: " + date + " Error was: "  + sqlEx,sqlEx);
			throw new ApplicationException(sqlEx);
		} finally {
			super.closeResources(conn, stmt, rs);
		}
		
		return null;
	}
	
	public List<BasePatternModel> getBreakingOutBasesForDate(final Date date) throws ApplicationException {
		final String sql = "select  i.symbol,b.BaseID,b.PeriodicityID,b.BaseStartDate,b.LeftSideHighDate,b.PivotPriceDate,b.PivotDate,b.BaseEndDate,b.BaseLength,b.BaseNumber,b.BaseStage,b.BaseStatusID,b.BaseTypeID,b.PivotPrice,b.instrumentid, i.msid "
					+ " from patternrecdb.dbo.base b, panaraymaster.panaray.ref.instrument i "
					+ " where b.versionid = (select top 1 pv.versionid from PatternRecDB.dbo.ProductVersion pv where pv.ProductCode = 3) "
					+ " and b.pivotdate=? and b.PeriodicityID=0 "
					+ " and b.instrumentid=i.instrumentid and i.countrycode=1 and i.instrumenttypeid=1";
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;		
		try {
			conn = getLmDataSource().getConnection();
			stmt = conn.prepareStatement(sql);
			stmt.setDate(1, new java.sql.Date(date.getTime()));
			rs = stmt.executeQuery();
			
			final List<BasePatternModel> patterns = new  ArrayList<BasePatternModel>();
			while(rs.next()) {
				final BasePatternModel aPattern = new BasePatternModel();
				
				aPattern.setSymbol(rs.getString(1));
				aPattern.setBaseId(rs.getLong(2));
				aPattern.setDaily(rs.getInt(3) == 0);
				aPattern.setBaseStartDt(rs.getDate(4));
				// 5 ignored
				aPattern.setPivotPriceDt(rs.getDate(6));
				aPattern.setPivotDt(rs.getDate(7));
				aPattern.setBaseEndDt(rs.getDate(8));
				aPattern.setBaseLen(rs.getInt(9));
				aPattern.setBaseNum(rs.getInt(10));
				aPattern.setBaseStage(rs.getString(11));
				aPattern.setStatusType(BaseStatusType.fromDB(rs.getInt(12)));
				
				aPattern.setBaseType(BaseType.fromDB(rs.getInt(13)));
				aPattern.setPivotPrice(rs.getBigDecimal(14));
				aPattern.setOsid(rs.getInt(15));
				aPattern.setMsid(rs.getLong(16));
				
				patterns.add(aPattern);
			}
			return patterns;
		} catch(SQLException sqlEx) {
			_logger.error("Error getting breaking out patterns for date: " + date + " Error was: "  + sqlEx,sqlEx);
			throw new ApplicationException(sqlEx);
		} finally {
			super.closeResources(conn, stmt, rs);
		}
	}
	
	/**
	 * @return the lmDataSource
	 */
	public DataSource getLmDataSource() {
		return lmDataSource;
	}

	/**
	 * @param lmDataSource the lmDataSource to set
	 */
	public void setLmDataSource(DataSource lmDataSource) {
		this.lmDataSource = lmDataSource;
	}
}
