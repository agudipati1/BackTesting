package com.williamoneil.backtesting.dao;

import java.sql.Date;

public class SymbolInfoModel {

	private int msId;
	private int instrumentId;
	private String symbol;
	private String name;
	private InstrumentType type;
	private Date historyStartDt;
	private Date lastTradeDt;
	private String exchange;
	private boolean active;
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the type
	 */
	public InstrumentType getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(InstrumentType type) {
		this.type = type;
	}

	/**
	 * @return the exchange
	 */
	public String getExchange() {
		return exchange;
	}

	/**
	 * @param exchange the exchange to set
	 */
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active
	 *            the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @return the msId
	 */
	public int getMsId() {
		return msId;
	}

	/**
	 * @param msId
	 *            the msId to set
	 */
	public void setMsId(int msId) {
		this.msId = msId;
	}

	/**
	 * @return the instrumentId
	 */
	public int getInstrumentId() {
		return instrumentId;
	}

	/**
	 * @param instrumentId
	 *            the instrumentId to set
	 */
	public void setInstrumentId(int instrumentId) {
		this.instrumentId = instrumentId;
	}

	/**
	 * @return the symbol
	 */
	public String getSymbol() {
		return symbol;
	}

	/**
	 * @param symbol
	 *            the symbol to set
	 */
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	/**
	 * @return the historyStartDt
	 */
	public Date getHistoryStartDt() {
		return historyStartDt;
	}

	/**
	 * @param historyStartDt
	 *            the historyStartDt to set
	 */
	public void setHistoryStartDt(Date historyStartDt) {
		this.historyStartDt = historyStartDt;
	}

	/**
	 * @return the lastTradeDt
	 */
	public Date getLastTradeDt() {
		return lastTradeDt;
	}

	/**
	 * @param lastTradeDt
	 *            the lastTradeDt to set
	 */
	public void setLastTradeDt(Date lastTradeDt) {
		this.lastTradeDt = lastTradeDt;
	}
}
