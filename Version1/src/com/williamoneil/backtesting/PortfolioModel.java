/**
 * 
 */
package com.williamoneil.backtesting;

import java.util.Date;
import java.util.List;

import com.williamoneil.ApplicationException;
import com.williamoneil.backtesting.data.PortfolioData;
import com.williamoneil.backtesting.data.TransactionData;

/**
 * @author Gudipati
 *
 */
public interface PortfolioModel {

	public void init(final AlphaModel alphaModel, final ExecutionModel executionModel) throws ApplicationException;
	
	/*
	 * Called before the actual run
	 */
	public void prepareForRun(String runId, String runName, Date startDate, Date endDate) throws ApplicationException;	
	
	/*
	 * Return the latest portfolio
	 */
	public PortfolioData getPortfolioData() throws ApplicationException;

	/*
	 * Runs portfolio tasks for a given date. Uses alphamodel to find signals and execution mode to execute on them if needed.
	 * Returns the executed position data containing necessary information about execution
	 */
	public List<TransactionData> performPortfolioCheck(Date runDate) throws ApplicationException;
	
}
