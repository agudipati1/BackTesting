/**
 * 
 */
package com.williamoneil.backtesting.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Gudipati
 *
 */
public class BaseDAOImpl {

	private static final Log _logger = LogFactory.getLog(BaseDAOImpl.class);
	
	private DataSource dataSource = null;

	/**
	 * @param dataSource
	 */
	public BaseDAOImpl() {
	}

	/**
	 * @param dataSource
	 */
	public BaseDAOImpl(DataSource dataSource) {
		this();
		this.dataSource = dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public DataSource getDataSource() {
		return dataSource;
	}
	
	public static void closeResources(Connection conn, Statement stmt) {
		closeStatement(stmt);
		closeConnection(conn);
	}

	public static void closeResources(Statement stmt, ResultSet rs) {
		closeResultSet(rs);
		closeStatement(stmt);
	}
	
	public static void closeResources(Connection conn, Statement stmt, ResultSet rs) {
		closeResultSet(rs);
		closeStatement(stmt);
		closeConnection(conn);
	}
	
	public static void closeConnection(Connection conn) {
		if(conn != null) {
			try {
				conn.close();
			}catch(Exception ex) {
				_logger.debug("Error closing conn. Exception was: " + ex, ex);
			}
		}
	}
	
	public static void closeStatement(Statement stmt) {
		if(stmt != null) {
			try {
				stmt.close();
			}catch(Exception ex) {
				_logger.debug("Error closing stmt. Exception was: " + ex, ex);
			}
		}		
	}
	
	public static void closeResultSet(ResultSet rs) {
		if(rs != null) {
			try {
				rs.close();
			}catch(Exception ex) {
				_logger.debug("Error closing rs. Exception was: " + ex, ex);
			}
		}
	}
}