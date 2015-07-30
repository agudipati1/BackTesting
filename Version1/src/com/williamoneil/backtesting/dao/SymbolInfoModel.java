package com.williamoneil.backtesting.dao;

import java.sql.Date;

public class SymbolInfoModel {

	private long msId;
	private long instrumentId;
	private String symbol;
	private String name;
	private InstrumentType type;
	private Date historyStartDt;
	private Date lastTradeDt;
	private String story;
	private boolean active;
	private String gicsSubIndCode = null;
	private String indCode = null;
	private String sedol = null;
	private String isin = null;
	private String cusip = null;
	
	
	
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

	/**
	 * @return the sedol
	 */
	public String getSedol() {
		return sedol;
	}

	/**
	 * @param sedol the sedol to set
	 */
	public void setSedol(String sedol) {
		this.sedol = sedol;
	}

	/**
	 * @return the isin
	 */
	public String getIsin() {
		return isin;
	}

	/**
	 * @param isin the isin to set
	 */
	public void setIsin(String isin) {
		this.isin = isin;
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
	 * @return the story
	 */
	public String getStory() {
		return story;
	}

	/**
	 * @param story the story to set
	 */
	public void setStory(String story) {
		this.story = story;
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
	public long getMsId() {
		return msId;
	}

	/**
	 * @param msId
	 *            the msId to set
	 */
	public void setMsId(long msId) {
		this.msId = msId;
	}

	/**
	 * @return the instrumentId
	 */
	public long getInstrumentId() {
		return instrumentId;
	}

	/**
	 * @param instrumentId
	 *            the instrumentId to set
	 */
	public void setInstrumentId(long instrumentId) {
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
