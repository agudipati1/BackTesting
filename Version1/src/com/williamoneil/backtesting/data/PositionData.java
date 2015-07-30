/**
 * 
 */
package com.williamoneil.backtesting.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.williamoneil.backtesting.util.Helpers;


/**
 * @author Gudipati
 *
 */
public class PositionData {

	private long msId = -1;
	private String symbol = null;
	private String cusip = null;
	private String gicsSubIndCode = null;
	private String indCode = null;
	
	private PositionType positionType = null;
	private int quantity = 0;
	private CurrencyData cost = CurrencyData.instantiate(BigDecimal.ZERO);
	private CurrencyData costBasis = CurrencyData.instantiate(BigDecimal.ZERO);;
	private CurrencyData currentValue = CurrencyData.instantiate(BigDecimal.ZERO);
	
	private List<TransactionData> transactions = new ArrayList<TransactionData>();
	
	public static PositionData createCashPosition(final BigDecimal cashBal) {
		final PositionData cashPosition = new PositionData();
		cashPosition.quantity = 1;
		cashPosition.symbol = "$CASH";
		cashPosition.positionType = PositionType.CASH;
		cashPosition.currentValue = CurrencyData.instantiate(cashBal);
		cashPosition.costBasis = CurrencyData.instantiate(cashBal);
		return cashPosition;
	}
	
	public void applySplit(BigDecimal splitFactor) {
		if(splitFactor == null || splitFactor.equals(BigDecimal.ZERO)) {
			return;
		}

		quantity = (int) (quantity * splitFactor.doubleValue());
		
		for(TransactionData tx : transactions) {
			tx.applySplit(splitFactor);
		}
	}
	
	public void setCurrentPrice(BigDecimal price) {
		this.currentValue = CurrencyData.instantiate(price.multiply(new BigDecimal(quantity)).subtract(cost.getValue()));
	}
	
	public List<TransactionData> getTransactionsSortedByTradeDate() {
		if(this.transactions == null || transactions.size() == 0) {
			return null;
		}
		
		/*
		 * Comparator to sort by date
		 */
		final Comparator<TransactionData> txDtSorter = new Comparator<TransactionData>() {
			@Override
			public int compare(TransactionData arg0, TransactionData arg1) {
				return arg1.getTransDt().compareTo(arg0.getTransDt());
			}
		};
	
		final List<TransactionData> sortedTxs = new ArrayList<TransactionData>(transactions);
		Collections.sort(sortedTxs, txDtSorter);
		
		return sortedTxs;
	}
	
	public TransactionData getLatestTransaction(TransactionType txType) {
		final List<TransactionData> sortedTxs = getTransactionsSortedByTradeDate();
		if(sortedTxs == null || sortedTxs.size() == 0) {
			return null;
		}
		
		if(txType == null) {
			return sortedTxs.get(0);
		} else {
			for(TransactionData aTx : sortedTxs) {
				if(aTx.getTransactionType() == txType) {
					return aTx;
				}
			}
		}

		return null;
	}
	
	public Date getLatestTransactionDate(TransactionType txType) {
		final TransactionData latestTx = getLatestTransaction(txType);
		if(latestTx != null) {
			return latestTx.getTransDt();
		}
		
		return null;
	}
	

	public void addTransaction(TransactionData tx) {
		if(this.symbol == null || this.msId == -1) {
			// this is a new position - lets copy the symbol from tx
			this.symbol = tx.getSymbol();
			this.msId = tx.getMsId();
		} else {
			// this is an existing position - so msid in tx should match the symbol in position
			if(this.msId != tx.getMsId()) {
				throw new RuntimeException("Cannot add a transaction for a different symbol (" + tx.getSymbol() + ") to a positon for symbol-" + this.symbol);
			}
		}
		
		if(tx != null) {
			if(tx.getTransactionType() == TransactionType.BUY) {
				quantity = quantity + tx.getQuantity();
				cost = CurrencyData.instantiate(cost.getValue().add(tx.getCost()));
				currentValue = CurrencyData.instantiate(currentValue.getValue().add(tx.getTotalAmt().getValue()));
				costBasis = CurrencyData.instantiate((costBasis.getValue().add(tx.getTotalAmt().getValue())));
			} else if (tx.getTransactionType() == TransactionType.SELL) {
				quantity = quantity - tx.getQuantity();
				cost = CurrencyData.instantiate(cost.getValue().add(tx.getCost()));
				currentValue = CurrencyData.instantiate(currentValue.getValue().subtract(tx.getTotalAmt().getValue()));
			} else {
				throw new RuntimeException("Only buy/sell tx are supported for now..");
			}
			
			transactions.add(tx);
		}
	}
	
	public Map<TransactionType, Integer> getTransactionCount() {
		final Map<TransactionType, Integer> transactionCountMap = new HashMap<TransactionType, Integer>();
		if(this.transactions != null) {
			for(final TransactionData tx : this.transactions) {
				Integer count = transactionCountMap.get(tx.getTransactionType());
				if(count == null){
					count = 0;
				}
				count++;
				
				transactionCountMap.put(tx.getTransactionType(), count);
			}
		}
		
		return transactionCountMap;
	}
	
	public BigDecimal getCostBasisPerShare() {
		if(quantity == 0) {
			return null;
		}
		return costBasis.getValue().divide(new BigDecimal(quantity), Helpers.mc);
	}
	
	public CurrencyData getCostBasis() {
		return costBasis;
	}
	
	public double getPercentGainLoss(){
		if(currentValue == null || costBasis == null || costBasis.equals(BigDecimal.ZERO)) {
			return 0;
		}
		
		final BigDecimal pct = currentValue.getValue().subtract(costBasis.getValue()).divide(costBasis.getValue(), Helpers.mc).multiply(Helpers.HUNDRED);
		return pct.doubleValue();
	}
	
	/**
	 * @return the symbol
	 */
	public String getSymbol() {
		return symbol;
	}

	/**
	 * @return the quantity
	 */
	public int getQuantity() {
		return quantity;
	}

	/**
	 * @return the cost
	 */
	public CurrencyData getCost() {
		return cost;
	}

	/**
	 * @return the currentValue
	 */
	public CurrencyData getCurrentValue() {
		return currentValue;
	}

	/**
	 * @return the positionType
	 */
	public PositionType getPositionType() {
		return positionType;
	}

	/**
	 * @return the transactions
	 */
	public List<TransactionData> getTransactions() {
		return transactions;
	}
	/**
	 * @return the msId
	 */
	public long getMsId() {
		return msId;
	}

	/**
	 * @param msId the msId to set
	 */
	public void setMsId(long msId) {
		this.msId = msId;
	}

	/**
	 * @return the cusip
	 */
	public String getCusip() {
		return cusip;
	}

	/**
	 * @param cusip the cusip to set
	 */
	public void setCusip(String cusip) {
		this.cusip = cusip;
	}

	/**
	 * @return the gicsSubIndCode
	 */
	public String getGicsSubIndCode() {
		return gicsSubIndCode;
	}

	/**
	 * @param gicsSubIndCode the gicsSubIndCode to set
	 */
	public void setGicsSubIndCode(String gicsSubIndCode) {
		this.gicsSubIndCode = gicsSubIndCode;
	}

	/**
	 * @return the indCode
	 */
	public String getIndCode() {
		return indCode;
	}

	/**
	 * @param indCode the indCode to set
	 */
	public void setIndCode(String indCode) {
		this.indCode = indCode;
	}
	
}
