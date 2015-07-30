package com.williamoneil.backtesting.dao;

import java.math.BigDecimal;
import java.util.Date;



public class BasePatternModel {
	private long osid = -1;
	private long msid = -1;
	
	private long baseId = 0;
	private BaseType baseType = null;
	private boolean isDaily = true;
	private Date baseStartDt = null;
	private Date baseEndDt = null;
	private Date pivotPriceDt = null;
	private int baseLen = 0;
	private int baseNum = 0;
	private String baseStage = null;
	private BaseStatusType statusType = null;
	private BigDecimal pivotPrice = null;
	private String symbol = null;
	private Date pivotDt = null;
	
	/**
	 * @return the osid
	 */
	public long getOsid() {
		return osid;
	}
	/**
	 * @param osid the osid to set
	 */
	public void setOsid(long osid) {
		this.osid = osid;
	}
	
	/**
	 * @return the msid
	 */
	public long getMsid() {
		return msid;
	}
	/**
	 * @param msid the msid to set
	 */
	public void setMsid(long msid) {
		this.msid = msid;
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
	 * @return the baseId
	 */
	public long getBaseId() {
		return baseId;
	}
	/**
	 * @param baseId the baseId to set
	 */
	public void setBaseId(long baseId) {
		this.baseId = baseId;
	}
	/**
	 * @return the baseType
	 */
	public BaseType getBaseType() {
		return baseType;
	}
	/**
	 * @param baseType the baseType to set
	 */
	public void setBaseType(BaseType baseType) {
		this.baseType = baseType;
	}
	/**
	 * @return the isDaily
	 */
	public boolean isDaily() {
		return isDaily;
	}
	/**
	 * @param isDaily the isDaily to set
	 */
	public void setDaily(boolean isDaily) {
		this.isDaily = isDaily;
	}
	/**
	 * @return the baseStartDt
	 */
	public Date getBaseStartDt() {
		return baseStartDt;
	}
	/**
	 * @param baseStartDt the baseStartDt to set
	 */
	public void setBaseStartDt(Date baseStartDt) {
		this.baseStartDt = baseStartDt;
	}
	/**
	 * @return the baseEndDt
	 */
	public Date getBaseEndDt() {
		return baseEndDt;
	}
	/**
	 * @param baseEndDt the baseEndDt to set
	 */
	public void setBaseEndDt(Date baseEndDt) {
		this.baseEndDt = baseEndDt;
	}
	/**
	 * @return the pivotPriceDt
	 */
	public Date getPivotPriceDt() {
		return pivotPriceDt;
	}
	/**
	 * @param pivotPriceDt the pivotPriceDt to set
	 */
	public void setPivotPriceDt(Date pivotPriceDt) {
		this.pivotPriceDt = pivotPriceDt;
	}
	/**
	 * @return the baseLen
	 */
	public int getBaseLen() {
		return baseLen;
	}
	/**
	 * @param baseLen the baseLen to set
	 */
	public void setBaseLen(int baseLen) {
		this.baseLen = baseLen;
	}
	/**
	 * @return the baseNum
	 */
	public int getBaseNum() {
		return baseNum;
	}
	/**
	 * @param baseNum the baseNum to set
	 */
	public void setBaseNum(int baseNum) {
		this.baseNum = baseNum;
	}
	/**
	 * @return the baseStage
	 */
	public String getBaseStage() {
		return baseStage;
	}
	/**
	 * @param baseStage the baseStage to set
	 */
	public void setBaseStage(String baseStage) {
		this.baseStage = baseStage;
	}
	/**
	 * @return the statusType
	 */
	public BaseStatusType getStatusType() {
		return statusType;
	}
	/**
	 * @param statusType the statusType to set
	 */
	public void setStatusType(BaseStatusType statusType) {
		this.statusType = statusType;
	}
	/**
	 * @return the pivotPrice
	 */
	public BigDecimal getPivotPrice() {
		return pivotPrice;
	}
	/**
	 * @param pivotPrice the pivotPrice to set
	 */
	public void setPivotPrice(BigDecimal pivotPrice) {
		this.pivotPrice = pivotPrice;
	}
	/**
	 * @return the pivotDt
	 */
	public Date getPivotDt() {
		return pivotDt;
	}
	/**
	 * @param pivotDt the pivotDt to set
	 */
	public void setPivotDt(Date pivotDt) {
		this.pivotDt = pivotDt;
	}

	
}
