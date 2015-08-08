/**
 * 
 */
package com.williamoneil.backtesting.data;

import java.util.Date;

import com.williamoneil.backtesting.dao.SymbolInfoModel;

/**
 * @author Gudipati
 *
 */
public class SignalData {

	private SymbolInfoModel symbolInfo = null; 
	private Date primarySignalDate = null;
	private Date signalDate = null;
	private TransactionType transactionType = null;
	private Float signalStrength = null;
	
	/**
	 * @return the symbol
	 */
	public SymbolInfoModel getSymbolInfo() {
		return symbolInfo;
	}

	/**
	 * @param symbol the symbol to set
	 */
	public void setSymbolInfo(SymbolInfoModel symbol) {
		this.symbolInfo = symbol;
	}

	/**
	 * @return the signalDate
	 */
	public Date getSignalDate() {
		return signalDate;
	}

	/**
	 * @param signalDate the signalDate to set
	 */
	public void setSignalDate(Date signalDate) {
		this.signalDate = signalDate;
	}

	/**
	 * @return the transactionType
	 */
	public TransactionType getTransactionType() {
		return transactionType;
	}

	/**
	 * @param transactionType the transactionType to set
	 */
	public void setTransactionType(TransactionType transactionType) {
		this.transactionType = transactionType;
	}

	/**
	 * @return the signalStrength
	 */
	public Float getSignalStrength() {
		return signalStrength;
	}

	/**
	 * @param signalStrength the signalStrength to set
	 */
	public void setSignalStrength(Float signalStrength) {
		this.signalStrength = signalStrength;
	}

	/**
	 * @return the primarySignalDate
	 */
	public Date getPrimarySignalDate() {
		return primarySignalDate;
	}

	/**
	 * @param primarySignalDate the primarySignalDate to set
	 */
	public void setPrimarySignalDate(Date primarySignalDate) {
		this.primarySignalDate = primarySignalDate;
	}
}
