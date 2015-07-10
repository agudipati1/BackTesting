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
public class ReportedFundamentalsModel {
	Date fiscalDate = null;
	Date reportedDate = null;
	BigDecimal sales = null;
	BigDecimal eps = null;

	boolean epsNonRecurringFlag = false;
	boolean periodFlag = false;

	BigDecimal priceOverSales = null;
	BigDecimal roe = null;

	BigDecimal grossMargin = null;
	BigDecimal afterTaxMargin = null;
	BigDecimal ebitMargin = null;
	BigDecimal preTaxMargin = null;
	BigDecimal freeCfOverShares = null;
	BigDecimal cfOverEPS = null;
	BigDecimal epsPctSurprise = null;

	boolean actualFlag = false;

	BigDecimal ebita = null;

	/**
	 * @return the fiscalDate
	 */
	public Date getFiscalDate() {
		return fiscalDate;
	}

	/**
	 * @param fiscalDate
	 *            the fiscalDate to set
	 */
	public void setFiscalDate(Date fiscalDate) {
		this.fiscalDate = fiscalDate;
	}

	/**
	 * @return the reportedDate
	 */
	public Date getReportedDate() {
		return reportedDate;
	}

	/**
	 * @param reportedDate
	 *            the reportedDate to set
	 */
	public void setReportedDate(Date reportedDate) {
		this.reportedDate = reportedDate;
	}

	/**
	 * @return the sales
	 */
	public BigDecimal getSales() {
		return sales;
	}

	/**
	 * @param sales
	 *            the sales to set
	 */
	public void setSales(BigDecimal sales) {
		this.sales = sales;
	}

	/**
	 * @return the eps
	 */
	public BigDecimal getEps() {
		return eps;
	}

	/**
	 * @param eps
	 *            the eps to set
	 */
	public void setEps(BigDecimal eps) {
		this.eps = eps;
	}

	/**
	 * @return the epsNonRecurringFlag
	 */
	public boolean isEpsNonRecurringFlag() {
		return epsNonRecurringFlag;
	}

	/**
	 * @param epsNonRecurringFlag
	 *            the epsNonRecurringFlag to set
	 */
	public void setEpsNonRecurringFlag(boolean epsNonRecurringFlag) {
		this.epsNonRecurringFlag = epsNonRecurringFlag;
	}

	/**
	 * @return the periodFlag
	 */
	public boolean isPeriodFlag() {
		return periodFlag;
	}

	/**
	 * @param periodFlag
	 *            the periodFlag to set
	 */
	public void setPeriodFlag(boolean periodFlag) {
		this.periodFlag = periodFlag;
	}

	/**
	 * @return the priceOverSales
	 */
	public BigDecimal getPriceOverSales() {
		return priceOverSales;
	}

	/**
	 * @param priceOverSales
	 *            the priceOverSales to set
	 */
	public void setPriceOverSales(BigDecimal priceOverSales) {
		this.priceOverSales = priceOverSales;
	}

	/**
	 * @return the roe
	 */
	public BigDecimal getRoe() {
		return roe;
	}

	/**
	 * @param roe
	 *            the roe to set
	 */
	public void setRoe(BigDecimal roe) {
		this.roe = roe;
	}

	/**
	 * @return the grossMargin
	 */
	public BigDecimal getGrossMargin() {
		return grossMargin;
	}

	/**
	 * @param grossMargin
	 *            the grossMargin to set
	 */
	public void setGrossMargin(BigDecimal grossMargin) {
		this.grossMargin = grossMargin;
	}

	/**
	 * @return the afterTaxMargin
	 */
	public BigDecimal getAfterTaxMargin() {
		return afterTaxMargin;
	}

	/**
	 * @param afterTaxMargin
	 *            the afterTaxMargin to set
	 */
	public void setAfterTaxMargin(BigDecimal afterTaxMargin) {
		this.afterTaxMargin = afterTaxMargin;
	}

	/**
	 * @return the ebitMargin
	 */
	public BigDecimal getEbitMargin() {
		return ebitMargin;
	}

	/**
	 * @param ebitMargin
	 *            the ebitMargin to set
	 */
	public void setEbitMargin(BigDecimal ebitMargin) {
		this.ebitMargin = ebitMargin;
	}

	/**
	 * @return the preTaxMargin
	 */
	public BigDecimal getPreTaxMargin() {
		return preTaxMargin;
	}

	/**
	 * @param preTaxMargin
	 *            the preTaxMargin to set
	 */
	public void setPreTaxMargin(BigDecimal preTaxMargin) {
		this.preTaxMargin = preTaxMargin;
	}

	/**
	 * @return the freeCfOverShares
	 */
	public BigDecimal getFreeCfOverShares() {
		return freeCfOverShares;
	}

	/**
	 * @param freeCfOverShares
	 *            the freeCfOverShares to set
	 */
	public void setFreeCfOverShares(BigDecimal freeCfOverShares) {
		this.freeCfOverShares = freeCfOverShares;
	}

	/**
	 * @return the cfOverEPS
	 */
	public BigDecimal getCfOverEPS() {
		return cfOverEPS;
	}

	/**
	 * @param cfOverEPS
	 *            the cfOverEPS to set
	 */
	public void setCfOverEPS(BigDecimal cfOverEPS) {
		this.cfOverEPS = cfOverEPS;
	}

	/**
	 * @return the epsPctSurprise
	 */
	public BigDecimal getEpsPctSurprise() {
		return epsPctSurprise;
	}

	/**
	 * @param epsPctSurprise
	 *            the epsPctSurprise to set
	 */
	public void setEpsPctSurprise(BigDecimal epsPctSurprise) {
		this.epsPctSurprise = epsPctSurprise;
	}

	/**
	 * @return the actualFlag
	 */
	public boolean isActualFlag() {
		return actualFlag;
	}

	/**
	 * @param actualFlag
	 *            the actualFlag to set
	 */
	public void setActualFlag(boolean actualFlag) {
		this.actualFlag = actualFlag;
	}

	/**
	 * @return the ebita
	 */
	public BigDecimal getEbita() {
		return ebita;
	}

	/**
	 * @param ebita
	 *            the ebita to set
	 */
	public void setEbita(BigDecimal ebita) {
		this.ebita = ebita;
	}
}
