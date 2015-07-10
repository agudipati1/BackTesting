/**
 * 
 */
package com.williamoneil.backtesting.dao;

/**
 * @author Gudipati
 *
 */
public class SymbolFundamentalsInfoModel {

	private Integer epsRank = null;
	private Integer compRating = null;
	private Integer dgRating = null;
	private Integer adRating = null;
	private Integer rsRank = null;
	private Integer groupRank = null;
	private String smrRating = null;
	private Integer smrRank = null;
	
	private Double latestAnnualEps = null;
	private Double latestAnnualEpsPctChg = null;
	private Double latestAnnualCFS = null;
	private Double latestAnnualPreTaxMargin = null;
	private Double latestAnnualRoE = null;
	private Double latestAnnualSalesPctChg = null;
	
	private Double firstAnnualEpsEst = null;
	private Double firstAnnualEpsPctChg = null;
	
	private Double secondAnnualEpsEst = null;
	private Double secondAnnualEpsPctChg = null;
	
	private Double twoYrEPSGrowthRate = null;
	private Double fourYrEPSGrowthRate = null;
	
	private Double twoYrSalesGrowthRate = null;
	private Double fourYrSalesGrowthRate = null;
	/**
	 * @return the epsRank
	 */
	public Integer getEpsRank() {
		return epsRank;
	}
	/**
	 * @param epsRank the epsRank to set
	 */
	public void setEpsRank(Integer epsRank) {
		this.epsRank = epsRank;
	}
	/**
	 * @return the compRating
	 */
	public Integer getCompRating() {
		return compRating;
	}
	/**
	 * @param compRating the compRating to set
	 */
	public void setCompRating(Integer compRating) {
		this.compRating = compRating;
	}
	/**
	 * @return the dgRating
	 */
	public Integer getDgRating() {
		return dgRating;
	}
	/**
	 * @param dgRating the dgRating to set
	 */
	public void setDgRating(Integer dgRating) {
		this.dgRating = dgRating;
	}
	/**
	 * @return the adRating
	 */
	public Integer getAdRating() {
		return adRating;
	}
	/**
	 * @param adRating the adRating to set
	 */
	public void setAdRating(Integer adRating) {
		this.adRating = adRating;
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
	 * @return the smrRating
	 */
	public String getSmrRating() {
		return smrRating;
	}
	/**
	 * @param smrRating the smrRating to set
	 */
	public void setSmrRating(String smrRating) {
		this.smrRating = smrRating;
	}
	/**
	 * @return the twoYrEPSGrowthRate
	 */
	public Double getTwoYrEPSGrowthRate() {
		return twoYrEPSGrowthRate;
	}
	/**
	 * @param twoYrEPSGrowthRate the twoYrEPSGrowthRate to set
	 */
	public void setTwoYrEPSGrowthRate(Double twoYrEPSGrowthRate) {
		this.twoYrEPSGrowthRate = twoYrEPSGrowthRate;
	}
	/**
	 * @return the fourYrEPSGrowthRate
	 */
	public Double getFourYrEPSGrowthRate() {
		return fourYrEPSGrowthRate;
	}
	/**
	 * @param fourYrEPSGrowthRate the fourYrEPSGrowthRate to set
	 */
	public void setFourYrEPSGrowthRate(Double fourYrEPSGrowthRate) {
		this.fourYrEPSGrowthRate = fourYrEPSGrowthRate;
	}
	/**
	 * @return the twoYrSalesGrowthRate
	 */
	public Double getTwoYrSalesGrowthRate() {
		return twoYrSalesGrowthRate;
	}
	/**
	 * @param twoYrSalesGrowthRate the twoYrSalesGrowthRate to set
	 */
	public void setTwoYrSalesGrowthRate(Double twoYrSalesGrowthRate) {
		this.twoYrSalesGrowthRate = twoYrSalesGrowthRate;
	}
	/**
	 * @return the fourYrSalesGrowthRate
	 */
	public Double getFourYrSalesGrowthRate() {
		return fourYrSalesGrowthRate;
	}
	/**
	 * @param fourYrSalesGrowthRate the fourYrSalesGrowthRate to set
	 */
	public void setFourYrSalesGrowthRate(Double fourYrSalesGrowthRate) {
		this.fourYrSalesGrowthRate = fourYrSalesGrowthRate;
	}
	/**
	 * @return the latestAnnualEps
	 */
	public Double getLatestAnnualEps() {
		return latestAnnualEps;
	}
	/**
	 * @param latestAnnualEps the latestAnnualEps to set
	 */
	public void setLatestAnnualEps(Double latestAnnualEps) {
		this.latestAnnualEps = latestAnnualEps;
	}
	/**
	 * @return the latestAnnualEpsPctChg
	 */
	public Double getLatestAnnualEpsPctChg() {
		return latestAnnualEpsPctChg;
	}
	/**
	 * @param latestAnnualEpsPctChg the latestAnnualEpsPctChg to set
	 */
	public void setLatestAnnualEpsPctChg(Double latestAnnualEpsPctChg) {
		this.latestAnnualEpsPctChg = latestAnnualEpsPctChg;
	}
	/**
	 * @return the latestAnnualCFS
	 */
	public Double getLatestAnnualCFS() {
		return latestAnnualCFS;
	}
	/**
	 * @param latestAnnualCFS the latestAnnualCFS to set
	 */
	public void setLatestAnnualCFS(Double latestAnnualCFS) {
		this.latestAnnualCFS = latestAnnualCFS;
	}
	/**
	 * @return the latestAnnualPreTaxMargin
	 */
	public Double getLatestAnnualPreTaxMargin() {
		return latestAnnualPreTaxMargin;
	}
	/**
	 * @param latestAnnualPreTaxMargin the latestAnnualPreTaxMargin to set
	 */
	public void setLatestAnnualPreTaxMargin(Double latestAnnualPreTaxMargin) {
		this.latestAnnualPreTaxMargin = latestAnnualPreTaxMargin;
	}
	/**
	 * @return the latestAnnualRoE
	 */
	public Double getLatestAnnualRoE() {
		return latestAnnualRoE;
	}
	/**
	 * @param latestAnnualRoE the latestAnnualRoE to set
	 */
	public void setLatestAnnualRoE(Double latestAnnualRoE) {
		this.latestAnnualRoE = latestAnnualRoE;
	}
	/**
	 * @return the latestAnnualSalesPctChg
	 */
	public Double getLatestAnnualSalesPctChg() {
		return latestAnnualSalesPctChg;
	}
	/**
	 * @param latestAnnualSalesPctChg the latestAnnualSalesPctChg to set
	 */
	public void setLatestAnnualSalesPctChg(Double latestAnnualSalesPctChg) {
		this.latestAnnualSalesPctChg = latestAnnualSalesPctChg;
	}
	/**
	 * @return the firstAnnualEpsEst
	 */
	public Double getFirstAnnualEpsEst() {
		return firstAnnualEpsEst;
	}
	/**
	 * @param firstAnnualEpsEst the firstAnnualEpsEst to set
	 */
	public void setFirstAnnualEpsEst(Double firstAnnualEpsEst) {
		this.firstAnnualEpsEst = firstAnnualEpsEst;
	}
	/**
	 * @return the firstAnnualEpsPctChg
	 */
	public Double getFirstAnnualEpsPctChg() {
		return firstAnnualEpsPctChg;
	}
	/**
	 * @param firstAnnualEpsPctChg the firstAnnualEpsPctChg to set
	 */
	public void setFirstAnnualEpsPctChg(Double firstAnnualEpsPctChg) {
		this.firstAnnualEpsPctChg = firstAnnualEpsPctChg;
	}
	/**
	 * @return the secondAnnualEpsEst
	 */
	public Double getSecondAnnualEpsEst() {
		return secondAnnualEpsEst;
	}
	/**
	 * @param secondAnnualEpsEst the secondAnnualEpsEst to set
	 */
	public void setSecondAnnualEpsEst(Double secondAnnualEpsEst) {
		this.secondAnnualEpsEst = secondAnnualEpsEst;
	}
	/**
	 * @return the secondAnnualEpsPctChg
	 */
	public Double getSecondAnnualEpsPctChg() {
		return secondAnnualEpsPctChg;
	}
	/**
	 * @param secondAnnualEpsPctChg the secondAnnualEpsPctChg to set
	 */
	public void setSecondAnnualEpsPctChg(Double secondAnnualEpsPctChg) {
		this.secondAnnualEpsPctChg = secondAnnualEpsPctChg;
	}
	/**
	 * @return the smrRank
	 */
	public Integer getSmrRank() {
		return smrRank;
	}
	/**
	 * @param smrRank the smrRank to set
	 */
	public void setSmrRank(Integer smrRank) {
		this.smrRank = smrRank;
	}
}
