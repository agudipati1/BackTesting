/**
 * 
 */
package com.williamoneil.backtesting.dao;

/**
 * @author Gudipati
 *
 */
public enum BaseStatusType {

	FORMING (1),
	FAILED (2),
	COMPLETE (3),
	MAXOUT (4);	
	
	private int dbValue = -1;

	private BaseStatusType(final int dbValue) {
		this.dbValue = dbValue;
	}

	/**
	 * @return the dbValue
	 */
	public int getDbValue() {
		return dbValue;
	}
		
	public static BaseStatusType fromDB(int dbValue) {
		for (final BaseStatusType p : BaseStatusType.values()) {
			if (p.getDbValue() == dbValue) {
				return p;
			}
		}
		return null;
	}
}
