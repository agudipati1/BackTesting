/**
 * 
 */
package com.williamoneil.backtesting;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.williamoneil.ApplicationException;
import com.williamoneil.Constants;
import com.williamoneil.backtesting.data.PortfolioData;
import com.williamoneil.backtesting.data.PositionData;
import com.williamoneil.backtesting.data.TransactionData;
import com.williamoneil.backtesting.util.Helpers;

/**
 * @author Gudipati
 *
 */
public class BackTestingApplicationImpl implements BackTestingApplication {

	private static final Log logger = LogFactory.getLog(BackTestingApplicationImpl.class);
	
	@Autowired
	private AlphaModel alphaModel = null;
	
	@Autowired
	private PortfolioModel portfolioModel = null;
	
	@Autowired
	private ExecutionModel executionModel = null;
	
	private String runLogFolderPath = null;
	
	
	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.BackTestingApplication#init()
	 */
	@Override
	public void init() throws ApplicationException {
		logger.info("initializing application..");
		
		if(alphaModel == null) {
			throw new ApplicationException("No Alpha Model was set. Application is not wired properly.");
		}
		
		if(portfolioModel == null) {
			throw new ApplicationException("No Portfolio Model was set. Application is not wired properly.");
		}
		
		if(executionModel == null) {
			throw new ApplicationException("No Execution Model was set. Application is not wired properly.");
		}
		
		alphaModel.init();
		executionModel.init();
		portfolioModel.init(alphaModel, executionModel);
		
		if(runLogFolderPath == null || runLogFolderPath.trim().length() == 0) {
			logger.info("No run-folder-path mentioned. Using default:" + Constants.DEFAULT_RUN_FOLDER_PATH);
			
			runLogFolderPath = Constants.DEFAULT_RUN_FOLDER_PATH;
		} else {
			logger.info("Using run-folder-path:" + runLogFolderPath);
			
		}
		
		logger.debug("initialization of application complete.");
	}

	/* (non-Javadoc)
	 * @see com.williamoneil.backtesting.BackTestingApplication#runBackTest(java.lang.String, java.lang.String, java.util.Date, java.util.Date)
	 */
	@Override
	public void runBackTest(String runId, String runName, Date startDate, Date endDate) throws ApplicationException {
		if(endDate == null) {
			endDate = new Date();
		}
		
		if(startDate.after(endDate)) {
			throw new ApplicationException("Invalid start-date passed. It has to be either a past date and before end-date");
		}
		
		prepareForRun(runId, runName, startDate, endDate);
		
		run(runId, runName, startDate, endDate);
	}

	private void run(String runId, String runName, Date startDt, Date endDt) throws ApplicationException {
		
		final Calendar startCal = Calendar.getInstance();
		startCal.setTime(startDt);
		final Calendar endCal = Calendar.getInstance();
		endCal.setTime(endDt);

		final Calendar backTestDtCal = (Calendar) startCal.clone();
		
		int index = 0;
		do {
			backTestDtCal.add(Calendar.DATE, 1);
			index++;
			
			final int dayOfWeek = backTestDtCal.get(Calendar.DAY_OF_WEEK);
			if(dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
				continue;
			} 
			else {
				final List<TransactionData> execPos = portfolioModel.performPortfolioCheck(backTestDtCal.getTime());
				if(execPos != null) {
					//logger.info(backTestDtCal.getTime() + " Executed on " + execPos.size() + " positions.");
				}
			}
			
			//logger.info("Current date: " + backTestDtCal.getTime() + " %-chg: " + this.getPortfolioModel().getPortfolioData().getTotalPortfolioValue() + " with num-pos: " + this.getPortfolioModel().getPortfolioData().getCurrentNumOfPosition());
			
		} while(backTestDtCal.before(endCal));
		
		// final stats at the end of backtest run..
		
		final PortfolioData portfolio = this.getPortfolioModel().getPortfolioData();
		RunLogger.getRunLogger().logPortfolio("");
		RunLogger.getRunLogger().logPortfolio("---- Final Portfolio Holdings as of " + endCal.getTime() + " ----");
		for(final PositionData aPos : portfolio.getPositions()) {
			RunLogger.getRunLogger().logPortfolio(aPos.getPositionType() + " " + aPos.getQuantity() + " of " + aPos.getSymbol() + " with cost basis of " +aPos.getCostBasisPerShare().doubleValue() + " and pct-chg: " + aPos.getPercentGainLoss() + "%" + " with % of portfolio weight=" +  aPos.getCurrentValue().getValue().multiply(Helpers.HUNDRED).divide(portfolio.getTotalPortfolioValue().getValue(), Helpers.mc).doubleValue() + "%");
		}
		RunLogger.getRunLogger().logPortfolio("");
		RunLogger.getRunLogger().logPortfolio("Total Invested Amount = " + portfolio.getInvestedPortfolioValue().getValue().doubleValue()  + " with % of portfolio weight=" +  portfolio.getInvestedPortfolioValue().getValue().multiply(Helpers.HUNDRED).divide(portfolio.getTotalPortfolioValue().getValue(), Helpers.mc).doubleValue() + "%");
		RunLogger.getRunLogger().logPortfolio("Cash Bal = " +  portfolio.getCashPosition().getCurrentValue().getValue().doubleValue() + " with % of portfolio weight=" +  portfolio.getCashPosition().getCurrentValue().getValue().multiply(Helpers.HUNDRED).divide(portfolio.getTotalPortfolioValue().getValue(), Helpers.mc).doubleValue() + "%");
		RunLogger.getRunLogger().logPortfolio("Net Value = " +  portfolio.getTotalPortfolioValue().getValue().doubleValue());
		RunLogger.getRunLogger().logPortfolio("");
		RunLogger.getRunLogger().logPortfolio("---- Run stats ----");
		RunLogger.getRunLogger().logPortfolio("Start-Date: " + startDt);
		RunLogger.getRunLogger().logPortfolio("End-Date: " + endDt);
		RunLogger.getRunLogger().logPortfolio("Total Trading days: " + index);
		RunLogger.getRunLogger().logPortfolio("Net Gain/Loss = " + portfolio.getTotalPortfolioValue().getValue().subtract(portfolio.getCashPosition().getCostBasis().getValue()).doubleValue());
		RunLogger.getRunLogger().logPortfolio("Pct Chg = " + portfolio.getPercentGainLoss(portfolio.getCashPosition().getCostBasis()) + "%");
	}
	
	private void prepareForRun(String runId, String runName, Date startDt, Date endDt) throws ApplicationException {
		RunLogger.initRunLogger(runLogFolderPath, runId, runName);
		
		this.alphaModel.prepareForRun(runId, runName, startDt, endDt);
		this.portfolioModel.prepareForRun(runId, runName, startDt, endDt);
		this.executionModel.prepareForRun(runId, runName, startDt, endDt);
	}
	
	/**
	 * @return the alphaModel
	 */
	public AlphaModel getAlphaModel() {
		return alphaModel;
	}

	/**
	 * @param alphaModel the alphaModel to set
	 */
	public void setAlphaModel(AlphaModel alphaModel) {
		this.alphaModel = alphaModel;
	}

	/**
	 * @return the portfolioModel
	 */
	public PortfolioModel getPortfolioModel() {
		return portfolioModel;
	}

	/**
	 * @param portfolioModel the portfolioModel to set
	 */
	public void setPortfolioModel(PortfolioModel portfolioModel) {
		this.portfolioModel = portfolioModel;
	}

	/**
	 * @return the executionModel
	 */
	public ExecutionModel getExecutionModel() {
		return executionModel;
	}

	/**
	 * @param executionModel the executionModel to set
	 */
	public void setExecutionModel(ExecutionModel executionModel) {
		this.executionModel = executionModel;
	}

}
