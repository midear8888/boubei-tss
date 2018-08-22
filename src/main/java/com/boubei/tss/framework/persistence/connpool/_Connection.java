/* ==================================================================   
 * Created [2007-5-9] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018  
 * ================================================================== 
 */

package com.boubei.tss.framework.persistence.connpool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.ConfigurableContants;
import com.boubei.tss.util.EasyUtils;

/**
 * 管理数据库连接的provider，以及创建或释放掉连接.
 * 如果连接池定义了自己的数据源信息，则采用；否则从默认的系统配置文件里加载。
 */
public class _Connection extends ConfigurableContants {

	protected final static Logger log = Logger.getLogger(_Connection.class);

	private IConnectionProvider provider;
	
	/**
	 * 如果配置的是数据源，则优先从数据源获取连接；否则手动创建一个连接。
	 * propertiesX：可以是个配置文件，或者："org.h2.Driver,jdbc:h2:mem:h2db;DB_CLOSE_DELAY=-1,sa,123";
	 */
	private _Connection(String propertiesX) {
		if(propertiesX.endsWith(".properties")) {
			Properties dbProperties = super.init(propertiesX);
			provider = new DriverManagerConnectionProvider(dbProperties);
		}
		else {
			provider = new DriverManagerConnectionProvider(propertiesX);
		}
	}

	static Map<String, _Connection> _connectionMap = new HashMap<String, _Connection>();
 
    public static _Connection getInstanse(String propertiesFile) {
        propertiesFile = (String) EasyUtils.checkNull(propertiesFile, DEFAULT_PROPERTIES);
        _Connection _connection = _connectionMap.get(propertiesFile);
        if (_connection == null) {
            _connectionMap.put(propertiesFile, _connection = new _Connection(propertiesFile));
        }
        return _connection;
    }

	public Connection getConnection() {
		return provider.getConnection();
	}

	public void releaseConnection(Connection conn) {
		try {
			conn.close();
		} catch (SQLException e) {
			log.fatal("销毁数据库连接时候出错", e);
		}
	}
 
	/*************************************** 获取连接接口  ***************************/
	interface IConnectionProvider {
		Connection getConnection();
	}

	/**
	 * <pre>
	 *  ## JNDI Datasource 
	 *  hibernate.connection.datasource jdbc/tss  
	 *  hibernate.connection.username db2  
	 *  hibernate.connection.password db2  
	 * </pre>
	 */
//	class DatasourceConnectionProvider implements IConnectionProvider { }
 
	class DriverManagerConnectionProvider implements IConnectionProvider {
 
	    String driver, url, user, pwd;
		
		public DriverManagerConnectionProvider(Properties p) {
			driver = p.getProperty("db.connection.driver_class").trim();
			url    = p.getProperty("db.connection.url").trim();
			user   = p.getProperty("db.connection.username").trim();
			pwd    = p.getProperty("db.connection.password").trim();
		}
		
		public DriverManagerConnectionProvider(String config) {
			String[] infos = config.split(",");
			driver = infos[0].trim();
			url    = infos[1].trim();
			user   = infos[2].trim();
			pwd    = infos[3].trim();
		}
		
		public Connection getConnection() {
	        return _Connection.openConnection(driver, url, user, pwd);
		}
	}
	
	public static Connection openConnection(String driver, String url, String user, String pwd) {
		Connection conn = null;
        try {
            Class.forName(driver);
            DriverManager.setLoginTimeout(30);
			conn = DriverManager.getConnection(url, user, pwd);
        } 
        catch (Exception e) {
        	log.error(EX.parse(EX.F_03, url, user, e.getMessage()));
            throw new BusinessException( EX.parse(EX.F_03, driver, user, e.getMessage()) );
        } 
        return conn;
	}
}