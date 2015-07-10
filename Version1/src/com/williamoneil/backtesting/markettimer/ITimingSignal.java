/**
 * 
 */
package com.williamoneil.backtesting.markettimer;

import java.io.Serializable;

/**
 * @author Gudipati
 *
 */
public interface ITimingSignal extends Serializable {
	
	public String getName();
	
	public String getDescription();
	
	// true = buy; false = sell, NULL = nothing
	public Boolean isBuySignal();
}
