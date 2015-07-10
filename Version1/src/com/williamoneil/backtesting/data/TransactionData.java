/**
 * 
 */
package com.williamoneil.backtesting.data;

import java.math.BigDecimal;
import java.util.Date;

import com.williamoneil.backtesting.util.Helpers;

/**
 * @author Gudipati
 *
 */
public class TransactionData {

	private String symbol = null;
	private Date transDt = null;
	private TransactionType transactionType = null;
	private int quantity = 0;
	private CurrencyData totalAmt = null;
	private BigDecimal cost = null;
	
	public void applySplit(BigDecimal splitFactor) {
		if(splitFactor == null || splitFactor.equals(BigDecimal.ZERO)) {
			return;
		}

		quantity = (int) (quantity * splitFactor.doubleValue());
	}
	
	/**
	 * @return the symbol
	 */
	public String getSymbol() {
		return symbol;
	}
	/**
	 * @param symbol the symbol to set
	 */
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	/**
	 * @return the transDt
	 */
	public Date getTransDt() {
		return transDt;
	}
	/**
	 * @param transDt the transDt to set
	 */
	public void setTransDt(Date transDt) {
		this.transDt = transDt;
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
	 * @return the quantity
	 */
	public int getQuantity() {
		return quantity;
	}
	/**
	 * @param quantity the quantity to set
	 */
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	/**
	 * @return the totalAmt
	 */
	public CurrencyData getTotalAmt() {
		return totalAmt;
	}
	/**
	 * @param totalAmt the totalAmt to set
	 */
	public void setTotalAmt(CurrencyData totalAmt) {
		this.totalAmt = totalAmt;
	}
	
	public BigDecimal getCostBasisPerShare() {
		if(quantity == 0) {
			return null;
		}
		return totalAmt.getValue().divide(new BigDecimal(quantity), Helpers.mc);
	}
	
	/**
	 * @return the cost
	 */
	public BigDecimal getCost() {
		return cost;
	}
	/**
	 * @param cost the cost to set
	 */
	public void setCost(BigDecimal cost) {
		this.cost = cost;
	} 
}
