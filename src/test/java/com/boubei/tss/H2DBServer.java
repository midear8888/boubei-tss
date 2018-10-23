package com.boubei.tss;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.h2.tools.Server;
import org.springframework.stereotype.Component;

@Component
public class H2DBServer {
	static Logger log = Logger.getLogger(H2DBServer.class);

	private Server server;
	Connection conn;

	public String URL = "jdbc:h2:mem:h2db;DB_CLOSE_DELAY=-1;LOCK_MODE=0"; // Connection关闭时不停用H2
	public String user = "sa";
	public String password = "123";
	public String port = "9081";

	public H2DBServer() {
		/* 
    	 * 此时H2数据库只起来了服务，没有实例
    	 */
    	new Thread() {
    		public void run() {
    			log.info("正在启动H2 database, 尝试端口号：" + port); 
    			try {  
    	            server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", port).start();  
    	        } catch (Exception e) {  
    	            log.warn("启动H2（createTcpServer）时出错：" + e.getMessage() );  
    	        } 

    	    	log.info("启动H2 成功 ，端口号：" + port); 
    		}
    	}.start();

		try {
			// 在以URL取得连接以后，数据库实例h2db才创建完成
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection(URL, user, password);
		} catch (Exception e) {
			log.error("建立H2连接时出错：" + e.toString());
		}
	}

	public void stopServer() {
		if (server != null) {
			log.info("正在关闭H2 database...端口号：" + port);

			try {
				conn.close();
			} catch (SQLException e) {
				throw new RuntimeException("关闭H2 database连接出错：" + e.toString(), e);
			}
			server.shutdown();
			server.stop();

			log.info("关闭H2 database成功...端口号：" + port);
		}
	}
}
