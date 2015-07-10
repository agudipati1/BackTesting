/**
 * 
 */
package com.williamoneil.backtesting.data;

import java.util.List;

/**
 * @author Gudipati
 *
 */
public class SignalInfoData implements Comparable<SignalInfoData> {

	private SignalType signalType = null;
	private Float signalStrength = null;
	private List<String> signalDesc = null;
	
	
	/**
	 * @return the signalType
	 */
	public SignalType getSignalType() {
		return signalType;
	}

	/**
	 * @param signalType the signalType to set
	 */
	public void setSignalType(SignalType signalType) {
		this.signalType = signalType;
	}


	/**
	 * @return the signalStrength
	 */
	public Float getSignalStrength() {
		return signalStrength;
	}


	/**
	 * @param signalStrength the signalStrength to set
	 */
	public void setSignalStrength(Float signalStrength) {
		this.signalStrength = signalStrength;
	}


	/**
	 * @return the signalDesc
	 */
	public List<String> getSignalDesc() {
		return signalDesc;
	}


	/**
	 * @param signalDesc the signalDesc to set
	 */
	public void setSignalDesc(List<String> signalDesc) {
		this.signalDesc = signalDesc;
	}


	@Override
	public int compareTo(SignalInfoData arg0) {
		if(arg0 == null || arg0.signalType == null || this.signalType == null ) {
			return 0;
		} else {
			return this.signalType.compareTo(arg0.signalType);
		}
	}
}
