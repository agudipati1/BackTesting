/**
 * 
 */
package com.williamoneil.backtesting.dao;



/**
 * @author Gudipati
 *
 */
public enum InstrumentType {

	STOCK (1), 
	INDEXANDMI(14),
	INDUSTRYGROUP (17),
	CURRENCY (19),
	MUTUALFUND (100),
	INDEXMI (104),
	INDEXMA (105),
	MUTUALFUNDFAMILY (118),
	SECTOR (119),
	MUTUALFUNDINDEX (120),
	NOCHARTABLE (121);
    
	private int dbValue = -1;

	private InstrumentType(int dbValue) {
		this.dbValue = dbValue;
	}

	/**
	 * @return the dbValue
	 */
	public int getDbValue() {
		return dbValue;
	}
	
	public static InstrumentType fromDB(int dbValue) {

		for (InstrumentType p : InstrumentType.values()) {
			if (p.getDbValue() == dbValue) {
				return p;
			}
		}
		return null;
	}
}
