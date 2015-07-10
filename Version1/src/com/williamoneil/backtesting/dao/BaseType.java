/**
 * 
 */
package com.williamoneil.backtesting.dao;

/**
 * @author Gudipati
 *
 */
public enum BaseType {

	CUP_HANDLE (1),
	CUP (2),
	SAUCER_HANDLE (3),
	SAUCER (4),
	ASCENDING_BASE (5),
	CONSOLIDATION (6),
	DOUBLE_BOTTOM (7),
	FLAT_BASE (8),
	HIGH_TIGHT_FLAG (9),
	IPO (10);
	
	private int dbValue = -1;

	private BaseType(final int dbValue) {
		this.dbValue = dbValue;
	}

	/**
	 * @return the dbValue
	 */
	public int getDbValue() {
		return dbValue;
	}
		
	public static BaseType fromDB(int dbValue) {
		for (final BaseType p : BaseType.values()) {
			if (p.getDbValue() == dbValue) {
				return p;
			}
		}
		return null;
	}
}
