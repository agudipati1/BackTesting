/**
 * 
 */
package com.williamoneil.backtesting;

import java.util.Date;
import java.util.List;

import com.williamoneil.ApplicationException;
import com.williamoneil.backtesting.data.SignalData;

/**
 * @author Gudipati
 *
 */
public interface AlphaModel {

	public void init() throws ApplicationException;
	
	public void prepareForRun(String runId, String runName, Date startDate, Date endDate) throws ApplicationException;
	
	/*
	 * Return a list of signal for a specified date. 
	 * Date is optional - if not specified, current date is used.
	 */
	public List<SignalData> getSignals(final Date date) throws ApplicationException;
	
	/*
	 * this method is expected to be called when the signals are not processed by portfolio-model due to whatever reasons
	 */
	public void unProcessedSignals(final List<SignalData> signals, final Date date) throws ApplicationException;

}
