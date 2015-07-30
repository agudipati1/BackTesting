/**
 * 
 */
package com.williamoneil.backtesting;

import java.util.Date;

import com.williamoneil.ApplicationException;
import com.williamoneil.backtesting.data.CurrencyData;
import com.williamoneil.backtesting.data.TransactionData;

/**
 * @author Gudipati
 *
 */
public interface ExecutionModel {
	public void init() throws ApplicationException;
	
	public void prepareForRun(String runId, String runName, Date startDate, Date endDate) throws ApplicationException;
	
	public TransactionData buy(long msId, String symbol, CurrencyData maxCashAvailForExecution, Date tradeDt) throws ApplicationException;
	
	public TransactionData sell(long msId, String symbol,  int quantity, Date tradeDate) throws ApplicationException;
}
