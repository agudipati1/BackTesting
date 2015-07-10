/**
 * 
 */
package com.williamoneil.backtesting.execution;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import com.williamoneil.ApplicationException;
import com.williamoneil.backtesting.ExecutionModel;
import com.williamoneil.backtesting.RunLogger;
import com.williamoneil.backtesting.dao.InstrumentPriceModel;
import com.williamoneil.backtesting.dao.PeriodicityType;
import com.williamoneil.backtesting.dao.TradeDateType;
import com.williamoneil.backtesting.dao.WONDAOImpl;
import com.williamoneil.backtesting.data.CurrencyData;
import com.williamoneil.backtesting.data.TransactionData;
import com.williamoneil.backtesting.data.TransactionType;
import com.williamoneil.backtesting.util.Helpers;

/**
 * @author Gudipati
 *
 */
public class SimpleExecutionModelImpl implements ExecutionModel {

	@Autowired
	private WONDAOImpl wonDAO = null;
	
	private double costPerTx = 7.00;
	private double priceSlipFactorPct = 0.0;
	
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.ExecutionModel#init()
	 */
	@Override
	public void init() throws ApplicationException {
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.ExecutionModel#execute(java.lang.String, com.williamoneil.backtesting.data.CurrencyData, java.util.Date, com.williamoneil.backtesting.data.TransactionType)
	 */
	@Override
	public TransactionData buy(String symbol, CurrencyData maxCashAvailForExecution, Date tradeDate) throws ApplicationException {
		if(symbol == null || maxCashAvailForExecution == null || maxCashAvailForExecution.getValue().doubleValue() <= 0) {
			return null;
		}

		final InstrumentPriceModel priceTick = wonDAO.getPriceTick(symbol, PeriodicityType.DAILY, tradeDate);
		if(priceTick == null) {
			throw new ApplicationException("No price tick available to perform BUY transaction for: " + symbol  + " on " + tradeDate);
		}
		if(priceTick.getDateType() == TradeDateType.HOLIDAY || priceTick.getDateType() == TradeDateType.MARKET_CLOSE) {
			throw new ApplicationException("BUY tx requested on holiday or market-close for: " + symbol  + " on " + tradeDate);
		}
		
		final BigDecimal txPrice = priceTick.getClose().multiply(new BigDecimal(1 + priceSlipFactorPct));
		
		int quantity = maxCashAvailForExecution.getValue().divide(txPrice, Helpers.mc).intValue();
		
		//  make sure we pick the right quantity to trade so our total tx amt will be less than max-cash-avail-for-execution
		while(((quantity * txPrice.doubleValue()) + costPerTx) > maxCashAvailForExecution.getValue().doubleValue()) {
			quantity = quantity -1;
		}
		
		if(quantity <= 0) {
			return null;
		}
		
		final BigDecimal totalAmt = txPrice.multiply(new BigDecimal(quantity));
				
		TransactionData tx = new TransactionData();
		tx.setSymbol(symbol);
		tx.setQuantity(quantity);
		tx.setTransDt(tradeDate);
		tx.setCost(new BigDecimal(costPerTx));
		tx.setTransactionType(TransactionType.BUY);
		tx.setTotalAmt(CurrencyData.instantiate(totalAmt));
		
		RunLogger.getRunLogger().logExecution(tradeDate + " BUY " + " - " + symbol + " - " + tx.getQuantity() + " @" + txPrice.doubleValue() + " Total=" + totalAmt.doubleValue()) ;
		RunLogger.getRunLogger().logForBTE(tradeDate, tx.getTransactionType(), symbol, tx.getQuantity(), txPrice, tx.getCost());
		return tx;
	}
	
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.ExecutionModel#sell(java.lang.String, int, java.util.Date)
	 */
	@Override
	public TransactionData sell(String symbol, int quantity, Date tradeDate) throws ApplicationException {
		if(symbol == null || quantity <= 0) {
			return null;
		}

		final InstrumentPriceModel priceTick = wonDAO.getPriceTick(symbol, PeriodicityType.DAILY, tradeDate);
		if(priceTick == null) {
			throw new ApplicationException("No price tick was found to do SELL transaction for: " + symbol  + " on " + tradeDate);
		}
		if(priceTick.getDateType() == TradeDateType.HOLIDAY || priceTick.getDateType() == TradeDateType.MARKET_CLOSE) {
			throw new ApplicationException("SELL tx requested on holiday or market-close for: " + symbol  + " on " + tradeDate);
		}
		
		final BigDecimal txPrice = priceTick.getClose().multiply(new BigDecimal(1 - priceSlipFactorPct));
		
		final BigDecimal totalAmt = txPrice.multiply(new BigDecimal(quantity));

		TransactionData tx = new TransactionData();
		tx.setSymbol(symbol);
		tx.setQuantity(quantity);
		tx.setTransDt(tradeDate);
		tx.setCost(new BigDecimal(costPerTx));
		tx.setTransactionType(TransactionType.SELL);
		tx.setTotalAmt(CurrencyData.instantiate(totalAmt));
		
		RunLogger.getRunLogger().logExecution(tradeDate + " SELL " + " - " + symbol + " - " + tx.getQuantity() + " @" + txPrice.doubleValue() + " Total=" + totalAmt.doubleValue()) ;
		RunLogger.getRunLogger().logForBTE(tradeDate, tx.getTransactionType(), symbol, tx.getQuantity(), txPrice, tx.getCost());
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

	@Override
	public void prepareForRun(String runId, String runName, Date startDate,
			Date endDate) throws ApplicationException {		
	}
	
}
