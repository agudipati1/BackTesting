/**
 * 
 */
package com.williamoneil.backtesting;

import java.util.Date;

import com.williamoneil.ApplicationException;

/**
 * @author Gudipati
 *
 */
public interface BackTestingApplication {

	public void init() throws ApplicationException;

	public AlphaModel getAlphaModel();
		
	public PortfolioModel getPortfolioModel();
	
	public ExecutionModel getExecutionModel();
	
	public void runBackTest(final String runId, final String name, final Date startDate, final Date endDate) throws ApplicationException;
	
}
