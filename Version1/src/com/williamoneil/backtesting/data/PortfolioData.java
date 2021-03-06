/**
 * 
 */
package com.williamoneil.backtesting.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.williamoneil.backtesting.util.Helpers;


/**
 * @author Gudipati
 *
 */
public class PortfolioData {

	private Date date = null;
	private List<PositionData> positions = new ArrayList<PositionData>();
	private PositionData cashPosition = null;
	
	private BigDecimal realizedGainLoss = BigDecimal.ZERO;
	
	public Map<String, List<Double>> getPositionsAndPctChg() {
		final Map<String, List<Double>> symSet = new HashMap<String, List<Double>>();
		for(final PositionData pos: positions) {
			final List<Double> l = new ArrayList<Double>();
			l.add(pos.getPercentGainLoss());
			l.add(pos.getCurrentValue().getValue().multiply(Helpers.HUNDRED).divide(this.getTotalPortfolioValue().getValue(), Helpers.mc).doubleValue());
			symSet.put(pos.getSymbol(), l);
		}
		
		return symSet;
	}
	
	public PositionData getPositionForMsId(long msId) {
		for(PositionData aPos : positions) {
			if(aPos.getMsId()== msId) {
				return aPos;
			}
		}
		
		return null;
	}
	
	// Used to identify if we are buying different stock from same company (eg FOXA/FOX etc.,)
	public PositionData getPositionForCusipMatch(String cusip) {
		if(cusip == null || cusip.length() <= 5) {
			return null;
		}
		
		// first 6 in cusip identifies the company
		final String cusipSubStr = cusip.substring(0, 6);
		
		for(PositionData aPos : positions) {
			if(aPos.getCusip() != null && aPos.getCusip().length() > 6 && aPos.getCusip().substring(0, 6).equalsIgnoreCase(cusipSubStr)) {
				return aPos;
			}
		}
		
		return null;
	}
	
	public PositionData addTransaction(TransactionData tx) {
		PositionData position = null;
		
		BigDecimal cashBalAfterTx = null;
		if(tx != null) {
			if(tx.getTransactionType() == TransactionType.BUY) {
				// first find if this is an existing position
				
				for(final PositionData aPos : this.getPositions()) {
					if(aPos.getMsId() == tx.getMsId()) {
						position = aPos;
						break;
					} else {
						continue;
					}
				}
				if(position != null) {
					// this is a buy-tx to add to an existing position
					position.addTransaction(tx);
				} else {
					position = new PositionData();
					position.addTransaction(tx);
					
					this.getPositions().add(position);
				}
				cashBalAfterTx = this.getCashPosition().getCurrentValue().getValue().subtract(tx.getTotalAmt().getValue());
			} else if(tx.getTransactionType() == TransactionType.SELL) {
				//  find the position in portfolio positions list and remove it
				for(final PositionData aPos : this.getPositions()) {
					if(aPos.getMsId() == tx.getMsId()) {
						position = aPos;
						
						// get costbasis before the tx is processed (coz adding tx changes it cost-basis)
						final BigDecimal costBasisPerShare = position.getCostBasisPerShare();
						
						position.addTransaction(tx);
						if(position.getQuantity() == 0) {
							this.getPositions().remove(position);
						}
						
						this.realizedGainLoss = this.realizedGainLoss.add(tx.getCostBasisPerShare().subtract(costBasisPerShare).multiply(new BigDecimal(tx.getQuantity())));
						
						break;
					} else {
						continue;
					}
				}
				
				//add sell proceeds to cash
				cashBalAfterTx = this.getCashPosition().getCurrentValue().getValue().add(tx.getTotalAmt().getValue());
			}
			
			this.getCashPosition().setCurrentPrice(cashBalAfterTx);
		}
		
		return position;
	}
	
	/**
	 * @return the realizedGainLoss
	 */
	public BigDecimal getRealizedGainLoss() {
		return realizedGainLoss;
	}

	public int getCurrentNumOfPosition() {
		return positions.size();
	}
	public int getTotalNumOfPositions(){
		return positions.size();
	}
	public CurrencyData getTotalPortfolioValue(){
		BigDecimal total = BigDecimal.ZERO;
		if(cashPosition != null) total = total.add(cashPosition.getCurrentValue().getValue());
		total = total.add(getInvestedPortfolioValue().getValue());
		return new CurrencyData(total);
	}
	public CurrencyData getInvestedPortfolioValue(){
		BigDecimal total = BigDecimal.ZERO;
		for(final PositionData aPos : positions) {
			total = total.add(aPos.getCurrentValue().getValue());
		}
		return new CurrencyData(total);
	}
	public double getPercentInvested() {
		final BigDecimal pct = getInvestedPortfolioValue().getValue().divide(getTotalPortfolioValue().getValue(), Helpers.mc).multiply(Helpers.HUNDRED);
		return pct.doubleValue();
	}
	
	public double getPercentGainLoss(CurrencyData initialValue){
		final CurrencyData currVal = getTotalPortfolioValue();
		final BigDecimal pct = currVal.getValue().subtract(initialValue.getValue()).divide(initialValue.getValue(), Helpers.mc).multiply(Helpers.HUNDRED);
		return pct.doubleValue();
	}
	
	
	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}
	/**
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}
	/**
	 * @return the positions
	 */
	public List<PositionData> getPositions() {
		return positions;
	}
	/**
	 * @param positions the positions to set
	 */
	public void setPositions(List<PositionData> positions) {
		this.positions = positions;
	}
	/**
	 * @return the cashPosition
	 */
	public PositionData getCashPosition() {
		return cashPosition;
	}
	/**
	 * @param cashPosition the cashPosition to set
	 */
	public void setCashPosition(PositionData cashPosition) {
		this.cashPosition = cashPosition;
	}
}
