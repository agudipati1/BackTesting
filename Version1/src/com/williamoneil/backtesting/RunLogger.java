/**
 * 
 */
package com.williamoneil.backtesting;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.williamoneil.ApplicationException;
import com.williamoneil.Constants;
import com.williamoneil.backtesting.data.TransactionType;

/**
 * @author Gudipati
 *
 */
public class RunLogger {

	private static RunLogger _runLogger = null;
	
	private File runLogFolder = null;
	
	private FileWriter runAlphaLogFileWriter = null;
	private FileWriter runPortfolioLogFileWriter = null;
	private FileWriter runExecutionLogFileWriter = null;
	private FileWriter runBTELogFileWriter = null;
	
	private RunLogger(String runLogFolderPath, String runId) throws ApplicationException {
		runLogFolder = new File(runLogFolderPath + System.getProperty("file.separator") + runId);
		
		final File runAlphaLogFile = new File(runLogFolder, "alpha-" + runId + ".log");
		final File runPortfolioLogFile = new File(runLogFolder, "portfolio-" + runId + ".log");
		final File runExecutionLogFile = new File(runLogFolder, "execution-" + runId + ".log");
		final File runBTELogFile = new File(runLogFolder, "bte-" + runId + ".log");
		
		runLogFolder.mkdirs();
		
		try {
			runAlphaLogFileWriter = new FileWriter(runAlphaLogFile, true);
			runPortfolioLogFileWriter = new FileWriter(runPortfolioLogFile, true);
			runExecutionLogFileWriter = new FileWriter(runExecutionLogFile, true);
			runBTELogFileWriter = new FileWriter(runBTELogFile, true);
			
			// BTE logger needs header.. so lets write the header now
			runBTELogFileWriter.write("Date,Action,Symbol,Shares,Price,Cost"+ Constants.LINE_SEPARATOR);
		}catch(Exception ex){
			ex.printStackTrace();
			throw new ApplicationException(ex);
		}
	}
	
	public void logPortfolio(String info) throws ApplicationException {
		try{
			runPortfolioLogFileWriter.write(info + Constants.LINE_SEPARATOR);
			runPortfolioLogFileWriter.flush();
		}catch(Exception ex) {
			throw new ApplicationException(ex);
		}
	}
	
	private static final SimpleDateFormat _sdf = new SimpleDateFormat("MM/dd/YYYY");
	public void logForBTE(Date tradeDt, TransactionType txType, String symbol, int quantity,  BigDecimal price, BigDecimal cost) throws ApplicationException {
		try{
			//"Date,Action,Symbol,Shares,Price,Cost"
			final String info = _sdf.format(tradeDt) + "," + txType.name() + ","+symbol+"," + quantity + "," + price.doubleValue() + "," + cost.doubleValue();
			runBTELogFileWriter.write(info + Constants.LINE_SEPARATOR);
			runBTELogFileWriter.flush();
		}catch(Exception ex) {
			throw new ApplicationException(ex);
		}
	}
	
	public void logExecution(String info) throws ApplicationException {
		try{
			runExecutionLogFileWriter.write(info + Constants.LINE_SEPARATOR);
			runExecutionLogFileWriter.flush();
		}catch(Exception ex) {
			throw new ApplicationException(ex);
		}
	}
	
	public void logAlpha(String info) throws ApplicationException {
		try{
			runAlphaLogFileWriter.write(info + Constants.LINE_SEPARATOR);
			runAlphaLogFileWriter.flush();
		}catch(Exception ex) {
			throw new ApplicationException(ex);
		}
	}
	
	public static void initRunLogger(final String runFolderPath, final String runId, final String runName) throws ApplicationException {
		if(_runLogger == null) {
			_runLogger = new RunLogger(runFolderPath, runId);
		}
	}
	
	public static RunLogger getRunLogger() {
		return _runLogger;
 	}
}
