/**
 * 
 */
package com.williamoneil.backtesting;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.williamoneil.ApplicationException;
import com.williamoneil.Constants;


/**
 * @author Gudipati
 *
 */
public abstract class Main {

	private static final Log logger = LogFactory.getLog(Main.class);
	
	
	private static final SimpleDateFormat _sdf = new SimpleDateFormat("yyyy-MM-dd");
	static {
		_sdf.setLenient(false);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {	
		TimeZone.setDefault(TimeZone.getTimeZone("GMT+00:00"));
		
		// FIRST - verify arguments passed
		if(args == null || args.length < 3) {
			System.err.println("Missing all the requirement arguments. Arguments required are - runId, runName, start_date_YYYY-MM-DD, end_date_YYYY-MM-DD (optional)");
			System.exit(-1);
		}
		
		final String runId = args[0].trim();
		final String runName = args[1].trim();
		final String startDtStr = args[2].trim();
		final String endDtStr = (args.length >= 4) ? args[3].trim() : null;
		
		final Date startDt = _sdf.parse(startDtStr);
		final Date endDt = (endDtStr == null || endDtStr.trim().length() == 0) ? null : _sdf.parse(endDtStr);
		
		//Second - lets configure the app classes
		// get default app-config-path from system properties
		final String appConfigPath = System.getProperty(Constants.APP_CONFIG_PROPERTY_KEY, Constants.APP_CONFIG_FILE_DEFAULT);
		
		logger.debug("Using application-config file at: " + appConfigPath);
		
		AbstractApplicationContext ctx = null;
		// load the app-config file.
		try{
			ctx = new ClassPathXmlApplicationContext(new String []{appConfigPath});

			PropertyPlaceholderConfigurer configurer = (PropertyPlaceholderConfigurer)ctx.getBean("placeholderConfig");
			if(configurer != null) {
				logger.debug("Replacing bean-properties from properties file.");
				configurer.postProcessBeanFactory(ctx.getBeanFactory());
			}
		}catch(Exception ex) {
			logger.fatal("Error encountered while loading application config.", ex);

			throw new ApplicationException(ex);
		}
		
		BackTestingApplication app = null;
		try {
			app = ctx.getBean("appMain", BackTestingApplication.class);
			if(app == null) {
				throw new ApplicationException("No bean for 'appMain' was configured in app-config file.");
			}
			
			app.init();
			
		}catch(ApplicationException appex) {
			logger.fatal("Application error encountered while starting the application.", appex);
			throw appex;
		}catch(Exception ex) {
			logger.fatal("Unexpected error encountered while starting the application.", ex);
			throw new ApplicationException(ex);
		}
		
		
		app.runBackTest(runId, runName, startDt, endDt);
		
		/*
		final StockIdeasHelper ideasHelper = app.getStockIdeasHelper();
		final VettedStockIdeasModel vettedIdeas = ideasHelper.getVettedStockIdeas();
		if(vettedIdeas != null) {
			final List<VettedStockIdeaModel> buyIdeas = vettedIdeas.getBuyList();
			if(buyIdeas != null && buyIdeas.size() > 0) {
				System.err.println("\r\nBuy Ideas:");
				System.err.println("-------------");
				
				for(final VettedStockIdeaModel aIdea: buyIdeas) {
					if(aIdea == null || aIdea.getStockIdea() == null) {
						continue;
					}
					
					System.err.println(aIdea.getStockIdea().getSymbol() + "," + aIdea.getNegativeObservations());
				}
			}
			
			
			final List<VettedStockIdeaModel> readyIdeas = vettedIdeas.getReadyList();
			if(readyIdeas != null && readyIdeas.size() > 0) {
				System.err.println("\r\nReady Ideas:");
				System.err.println("-------------");
				
				for(final VettedStockIdeaModel aIdea: readyIdeas) {
					if(aIdea == null || aIdea.getStockIdea() == null) {
						continue;
					}
					
					System.err.println(aIdea.getStockIdea().getSymbol() + "," + aIdea.getNegativeObservations());
				}
			}
			
			final List<VettedStockIdeaModel> notReadyIdeas = vettedIdeas.getNotReadyList();
			if(notReadyIdeas != null && notReadyIdeas.size() > 0) {
				System.err.println("\r\nNot-Ready Ideas:");
				System.err.println("-------------");
				
				for(final VettedStockIdeaModel aIdea: notReadyIdeas) {
					if(aIdea == null || aIdea.getStockIdea() == null) {
						continue;
					}
					
					System.err.println(aIdea.getStockIdea().getSymbol() + "," + aIdea.getNegativeObservations());
				}
			}
		}
		
		final MarketTimingModel currentMarketTiming = app.getMarketTimer().getCurrentMarketTiming();
		
		//System.err.println(currentMarketTiming);
		// now process the arguments passed to invoke the portoflio app accordingly
		
		//app.getPortfolioHelper().actOnMarketTiming(currentMarketTiming);
		*/
	}
}
