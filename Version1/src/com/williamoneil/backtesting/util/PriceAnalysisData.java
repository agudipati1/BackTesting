/**
 * 
 */
package com.williamoneil.backtesting.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.williamoneil.backtesting.dao.InstrumentPriceModel;

/**
 * @author Gudipati
 *
 */
public class PriceAnalysisData {

  private InstrumentPriceModel price = null;
  
  private BigDecimal price10dEma = null; //10d-ema on daily 
  private BigDecimal price20dEma = null; //20d-ema on daily
  private BigDecimal price50dSma = null; //50d-sma on daily
  private BigDecimal price200dSma = null; //200d-sma on daily
  
  private BigDecimal vol50dSma = null; //50d-sma on daily
  
  private BigDecimal atr14d = null; // 14d ATR on daily
  
  private List<PriceAnalysisElementData> analysisElements = null;
  
  public BigDecimal getElementPctChg(final ChartElementType elementType) {
	  switch(elementType) {
	  	case   PRICE:
		  	return null;
		  	
	  	case VOL: {
	  		if(vol50dSma == null) return null;
	  		BigDecimal volPctChg = (Helpers.HUNDRED.multiply(new BigDecimal(price.getVolume()).subtract(vol50dSma))).divide(vol50dSma, Helpers.mc);
	  		return volPctChg;
	  	}
	  	case PRICE_MA_10: {
	  		if(price10dEma == null) return null;
	  		BigDecimal pctChg = (Helpers.HUNDRED.multiply(price.getClose().subtract(price10dEma))).divide(price10dEma, Helpers.mc);
	  		return pctChg;
	  	}
	  	case PRICE_MA_20: {
	  		if(price20dEma == null) return null;
	  		BigDecimal pctChg = (Helpers.HUNDRED.multiply(price.getClose().subtract(price20dEma))).divide(price20dEma, Helpers.mc);
	  		return pctChg;
	  	}
	  	case PRICE_MA_50: {
	  		if(price50dSma == null) return null;
	  		BigDecimal pctChg = (Helpers.HUNDRED.multiply(price.getClose().subtract(price50dSma))).divide(price50dSma, Helpers.mc);
	  		return pctChg;
	  	}
	  	case PRICE_MA_200: {
	  		if(price200dSma == null) return null;
	  		BigDecimal pctChg = (Helpers.HUNDRED.multiply(price.getClose().subtract(price200dSma))).divide(price200dSma, Helpers.mc);
	  		return pctChg;
	  	}
	  	default : return null;
	  }
  }
  
  public PriceAnalysisElementData getAnalysisFor(final ChartElementType chartElementType) {
    if(analysisElements == null || chartElementType == null) {
      return null;
    }
    
    for(final PriceAnalysisElementData pae : analysisElements) {
      if(pae != null && pae.getElementType() != null && chartElementType == pae.getElementType()) {
        return pae;
      }
    }
    
    return null;
  }
  
  /**
   * @return the price
   */
  public InstrumentPriceModel getPrice() {
    return price;
  }

  /**
   * @param price the price to set
   */
  public void setPrice(InstrumentPriceModel price) {
    this.price = price;
  }

  /**
   * @return the price10dEma
   */
  public BigDecimal getPrice10dEma() {
    return price10dEma;
  }

  /**
   * @param price10dEma the price10dEma to set
   */
  public void setPrice10dEma(BigDecimal price10dEma) {
    this.price10dEma = price10dEma;
  }

  /**
   * @return the price20dEma
   */
  public BigDecimal getPrice20dEma() {
    return price20dEma;
  }

  /**
   * @param price20dEma the price20dEma to set
   */
  public void setPrice20dEma(BigDecimal price20dEma) {
    this.price20dEma = price20dEma;
  }

  /**
   * @return the price50dSma
   */
  public BigDecimal getPrice50dSma() {
    return price50dSma;
  }

  /**
   * @param price50dSma the price50dSma to set
   */
  public void setPrice50dSma(BigDecimal price50dSma) {
    this.price50dSma = price50dSma;
  }

  /**
   * @return the price200dSma
   */
  public BigDecimal getPrice200dSma() {
    return price200dSma;
  }

  /**
   * @param price200dSma the price200dSma to set
   */
  public void setPrice200dSma(BigDecimal price200dSma) {
    this.price200dSma = price200dSma;
  }
  
  /**
   * @return the vol50dSma
   */
  public BigDecimal getVol50dSma() {
    return vol50dSma;
  }

  /**
   * @param vol50dSma the vol50dSma to set
   */
  public void setVol50dSma(BigDecimal vol50dSma) {
    this.vol50dSma = vol50dSma;
  }

	/**
	 * @return the atr14d
	 */
	public BigDecimal getAtr14d() {
		return atr14d;
	}

	/**
	 * @param atr14d
	 *            the atr14d to set
	 */
	public void setAtr14d(BigDecimal atr14d) {
		this.atr14d = atr14d;
	}

/**
   * @return the analysisTypes
   */
  public List<PriceAnalysisElementData> getPriceAnalysisElements() {
    if(analysisElements == null) {
      analysisElements = new ArrayList<PriceAnalysisElementData>();
    }
    return analysisElements;
  }

  /**
   * @param analysisTypes the analysisTypes to set
   */
  public void setPriceAnalysisElements(List<PriceAnalysisElementData> analysisElements) {
    this.analysisElements = analysisElements;
  }
}




