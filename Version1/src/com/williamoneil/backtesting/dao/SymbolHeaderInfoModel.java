/**
 * 
 */
package com.williamoneil.backtesting.dao;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Gudipati
 *
 */
public class SymbolHeaderInfoModel {

	private PeriodicityType periodicity = null;
	private String symbol = null;
	private String coName = null;
	private Date epsDueDt = null;
	private String exchgName = null;
	private String industryName = null;
	private BigDecimal mktCap = null;
	private BigDecimal sales = null;
	private Long sharesOutstnd = null;
	private Long sharesFloat = null;
	private Date ipoDt = null;
	private Double openPrice = null;
	private String industrySym = null;
	private Long avgVol = null;
	private Long vol = null;
	private BigDecimal epsEst = null;
	private boolean ffo = false;
	private boolean nav = false;
	private double prevPrice = 0;
	private double currPrice = 0;
	private Integer groupRank = null;
	private Long prevVol = null;
	private Integer sdRank = null;
	private Integer rsRank = null;
	private String accDisRtg = null;
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
	 * @return the coName
	 */
	public String getCoName() {
		return coName;
	}
	/**
	 * @param coName the coName to set
	 */
	public void setCoName(String coName) {
		this.coName = coName;
	}
	/**
	 * @return the epsDueDt
	 */
	public Date getEpsDueDt() {
		return epsDueDt;
	}
	/**
	 * @param epsDueDt the epsDueDt to set
	 */
	public void setEpsDueDt(Date epsDueDt) {
		this.epsDueDt = epsDueDt;
	}
	/**
	 * @return the exchgName
	 */
	public String getExchgName() {
		return exchgName;
	}
	/**
	 * @param exchgName the exchgName to set
	 */
	public void setExchgName(String exchgName) {
		this.exchgName = exchgName;
	}
	/**
	 * @return the industryName
	 */
	public String getIndustryName() {
		return industryName;
	}
	/**
	 * @param industryName the industryName to set
	 */
	public void setIndustryName(String industryName) {
		this.industryName = industryName;
	}
	/**
	 * @return the mktCap
	 */
	public BigDecimal getMktCap() {
		return mktCap;
	}
	/**
	 * @param mktCap the mktCap to set
	 */
	public void setMktCap(BigDecimal mktCap) {
		this.mktCap = mktCap;
	}
	/**
	 * @return the sales
	 */
	public BigDecimal getSales() {
		return sales;
	}
	/**
	 * @param sales the sales to set
	 */
	public void setSales(BigDecimal sales) {
		this.sales = sales;
	}
	/**
	 * @return the sharesOutstnd
	 */
	public Long getSharesOutstnd() {
		return sharesOutstnd;
	}
	/**
	 * @param sharesOutstnd the sharesOutstnd to set
	 */
	public void setSharesOutstnd(Long sharesOutstnd) {
		this.sharesOutstnd = sharesOutstnd;
	}
	/**
	 * @return the sharesFloat
	 */
	public Long getSharesFloat() {
		return sharesFloat;
	}
	/**
	 * @param sharesFloat the sharesFloat to set
	 */
	public void setSharesFloat(Long sharesFloat) {
		this.sharesFloat = sharesFloat;
	}
	/**
	 * @return the ipoDt
	 */
	public Date getIpoDt() {
		return ipoDt;
	}
	/**
	 * @param ipoDt the ipoDt to set
	 */
	public void setIpoDt(Date ipoDt) {
		this.ipoDt = ipoDt;
	}
	/**
	 * @return the openPrice
	 */
	public Double getOpenPrice() {
		return openPrice;
	}
	/**
	 * @param openPrice the openPrice to set
	 */
	public void setOpenPrice(Double openPrice) {
		this.openPrice = openPrice;
	}
	/**
	 * @return the industrySym
	 */
	public String getIndustrySym() {
		return industrySym;
	}
	/**
	 * @param industrySym the industrySym to set
	 */
	public void setIndustrySym(String industrySym) {
		this.industrySym = industrySym;
	}
	/**
	 * @return the avgVol
	 */
	public Long getAvgVol() {
		return avgVol;
	}
	/**
	 * @param avgVol the avgVol to set
	 */
	public void setAvgVol(Long avgVol) {
		this.avgVol = avgVol;
	}
	/**
	 * @return the vol
	 */
	public Long getVol() {
		return vol;
	}
	/**
	 * @param vol the vol to set
	 */
	public void setVol(Long vol) {
		this.vol = vol;
	}
	/**
	 * @return the epsEst
	 */
	public BigDecimal getEpsEst() {
		return epsEst;
	}
	/**
	 * @param epsEst the epsEst to set
	 */
	public void setEpsEst(BigDecimal epsEst) {
		this.epsEst = epsEst;
	}
	/**
	 * @return the ffo
	 */
	public boolean isFfo() {
		return ffo;
	}
	/**
	 * @param ffo the ffo to set
	 */
	public void setFfo(boolean ffo) {
		this.ffo = ffo;
	}
	/**
	 * @return the nav
	 */
	public boolean isNav() {
		return nav;
	}
	/**
	 * @param nav the nav to set
	 */
	public void setNav(boolean nav) {
		this.nav = nav;
	}
	/**
	 * @return the prevPrice
	 */
	public double getPrevPrice() {
		return prevPrice;
	}
	/**
	 * @param prevPrice the prevPrice to set
	 */
	public void setPrevPrice(double prevPrice) {
		this.prevPrice = prevPrice;
	}
	/**
	 * @return the currPrice
	 */
	public double getCurrPrice() {
		return currPrice;
	}
	/**
	 * @param currPrice the currPrice to set
	 */
	public void setCurrPrice(double currPrice) {
		this.currPrice = currPrice;
	}
	/**
	 * @return the groupRank
	 */
	public Integer getGroupRank() {
		return groupRank;
	}
	/**
	 * @param groupRank the groupRank to set
	 */
	public void setGroupRank(Integer groupRank) {
		this.groupRank = groupRank;
	}
	/**
	 * @return the prevVol
	 */
	public Long getPrevVol() {
		return prevVol;
	}
	/**
	 * @param prevVol the prevVol to set
	 */
	public void setPrevVol(Long prevVol) {
		this.prevVol = prevVol;
	}
	/**
	 * @return the sdRank
	 */
	public Integer getSdRank() {
		return sdRank;
	}
	/**
	 * @param sdRank the sdRank to set
	 */
	public void setSdRank(Integer sdRank) {
		this.sdRank = sdRank;
	}
	/**
	 * @return the rsRank
	 */
	public Integer getRsRank() {
		return rsRank;
	}
	/**
	 * @param rsRank the rsRank to set
	 */
	public void setRsRank(Integer rsRank) {
		this.rsRank = rsRank;
	}
	/**
	 * @return the accDisRtg
	 */
	public String getAccDisRtg() {
		return accDisRtg;
	}
	/**
	 * @param accDisRtg the accDisRtg to set
	 */
	public void setAccDisRtg(String accDisRtg) {
		this.accDisRtg = accDisRtg;
	}
	/**
	 * @return the periodicity
	 */
	public PeriodicityType getPeriodicity() {
		return periodicity;
	}
	/**
	 * @param periodicity the periodicity to set
	 */
	public void setPeriodicity(PeriodicityType periodicity) {
		this.periodicity = periodicity;
	}
}
