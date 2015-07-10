/**
 * 
 */
package com.williamoneil.backtesting.data;

/**
 * @author Gudipati
 *
 */
public class ExecutedPositionData {

	private PositionData position = null;
	
	private TransactionType transactionType = null;
	
	private String info = null;
	
	private String warning = null;

	/**
	 * @return the position
	 */
	public PositionData getPosition() {
		return position;
	}

	/**
	 * @param position the position to set
	 */
	public void setPosition(PositionData position) {
		this.position = position;
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
	 * @return the info
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * @param info the info to set
	 */
	public void setInfo(String info) {
		this.info = info;
	}

	/**
	 * @return the warning
	 */
	public String getWarning() {
		return warning;
	}

	/**
	 * @param warning the warning to set
	 */
	public void setWarning(String warning) {
		this.warning = warning;
	}
	
}
