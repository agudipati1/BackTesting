/**
 * 
 */
package com.williamoneil.backtesting.dao;



/**
 * @author Gudipati
 *
 */
public enum PeriodicityType {

	DAILY (1),
	WEEKLY (2),
	MONTHLY (3),
	INTRADAY1MIN (4),
	INTRADAY5MIN (5),
	INTRADAY10MIN (6),
	INTRADAY15MIN (7),
	INTRADAY30MIN (8),
	INTRADAY60MIN (9);
	
	private int dbValue = -1;

	private PeriodicityType(final int dbValue) {
		this.dbValue = dbValue;
	}

	/**
	 * @return the dbValue
	 */
	public int getDbValue() {
		return dbValue;
	}
		
	public static PeriodicityType fromDB(int dbValue) {
		for (final PeriodicityType p : PeriodicityType.values()) {
			if (p.getDbValue() == dbValue) {
				return p;
			}
		}
		return null;
	}

}
