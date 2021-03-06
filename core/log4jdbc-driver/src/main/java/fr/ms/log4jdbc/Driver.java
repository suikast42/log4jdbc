/*
 * This file is part of Log4Jdbc.
 *
 * Log4Jdbc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Log4Jdbc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Log4Jdbc.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package fr.ms.log4jdbc;

import java.sql.Connection;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Logger;

import fr.ms.log4jdbc.context.Log4JdbcContext;
import fr.ms.log4jdbc.context.jdbc.Log4JdbcContextJDBC;
import fr.ms.log4jdbc.proxy.Log4JdbcProxy;
import fr.ms.sql.JdbcDriverManager;
import fr.ms.sql.JdbcDriverManagerFactory;
import fr.ms.util.Service;
import fr.ms.util.logging.LoggerManager;

/**
 *
 * @see <a href="http://marcosemiao4j.wordpress.com">Marco4J</a>
 *
 *
 * @author Marco Semiao
 *
 */
public class Driver implements java.sql.Driver {

	private final static fr.ms.util.logging.Logger LOG = LoggerManager.getLogger(Driver.class);

	private final static String LOG4JDBC_PREFIX = "jdbc:log4";

	private final static JdbcDriverManager driverManager = JdbcDriverManagerFactory.getInstance();

	private final Log4JdbcContext log4JdbcContext = new Log4JdbcContextJDBC();

	private java.sql.Driver driver;

	static {
		driverManager.setLogWriter(LOG.getPrintWriter());
		loadAdditionalDrivers();
		loadDrivers();
	}

	public Connection connect(String url, final Properties info) throws SQLException {
		final java.sql.Driver d = getRealDriver(url);
		if (d == null) {
			return null;
		}

		url = getRealUrl(url);

		final Connection c = d.connect(url, info);

		if (c == null) {
			throw new SQLException("invalid or unknown driver url: " + url);
		}

		final Connection wrap = Log4JdbcProxy.proxyConnection(c, log4JdbcContext, d, url);

		driver = d;
		return wrap;
	}

	public boolean acceptsURL(final String url) throws SQLException {
		final java.sql.Driver d = getRealDriver(url);
		if (d != null) {
			driver = d;
			return true;
		}
		return false;
	}

	public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) throws SQLException {
		final java.sql.Driver d = getRealDriver(url);
		if (d == null) {
			return new DriverPropertyInfo[0];
		}

		driver = d;
		return d.getPropertyInfo(url, info);
	}

	public int getMajorVersion() {
		if (driver == null) {
			return 1;
		}
		return driver.getMajorVersion();
	}

	public int getMinorVersion() {
		if (driver == null) {
			return 0;
		}
		return driver.getMinorVersion();
	}

	public boolean jdbcCompliant() {
		return driver != null && driver.jdbcCompliant();
	}

	public Logger getParentLogger() {
		try {
			return driver.getParentLogger();
		} catch (final Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	private static java.sql.Driver getRealDriver(String url) throws SQLException {
		if (url.startsWith(LOG4JDBC_PREFIX)) {
			url = getRealUrl(url);

			final Enumeration e = driverManager.getDrivers();

			java.sql.Driver d;
			while (e.hasMoreElements()) {
				d = (java.sql.Driver) e.nextElement();

				if (d.acceptsURL(url)) {
					return d;
				}
			}
		}
		return null;
	}

	private static String getRealUrl(final String url) {
		if (url == null) {
			return null;
		}
		if (url.length() < LOG4JDBC_PREFIX.length()) {
			return "";
		}
		return url.substring(LOG4JDBC_PREFIX.length());
	}

	private static void loadAdditionalDrivers() {
		String drivers = System.getProperty("log4jdbc.drivers");

		if (LOG.isInfoEnabled()) {
			LOG.info("Log4Jdbc DriverManager.Initialize: log4jdbc.drivers = " + drivers);
		}

		if (drivers != null) {
			while (drivers.length() != 0) {
				final int x = drivers.indexOf(':');
				String driver;
				if (x < 0) {
					driver = drivers;
					drivers = "";
				} else {
					driver = drivers.substring(0, x);
					drivers = drivers.substring(x + 1);
				}
				if (driver.length() == 0) {
					continue;
				}
				try {
					driver = driver.trim();
					if (LOG.isInfoEnabled()) {
						LOG.info("Log4jdbc DriverManager.Initialize: loading " + driver);
					}
					final Class clazz = Class.forName(driver);
					final java.sql.Driver d = (java.sql.Driver) clazz.newInstance();
					driverManager.registerDriver(d);
				} catch (final Exception ex) {
					throw new RuntimeException("Log4jdbc DriverManager.Initialize: load " + driver + " failed : " + ex);
				}
			}
		}
	}

	private static void loadDrivers() {
		final Iterator providers = Service.providers(java.sql.Driver.class);

		while (providers.hasNext()) {
			final java.sql.Driver d = (java.sql.Driver) providers.next();
			try {
				final Class clazz = d.getClass();
				if (!Driver.class.equals(clazz)) {
					driverManager.registerDriver(d);
				}
			} catch (final Exception ex) {
				throw new RuntimeException("Log4jdbc DriverManager.Initialize: load " + d + " failed : " + ex);
			}
		}

		try {
			driverManager.registerDriver(new Driver());
		} catch (final Exception ex) {
			throw new RuntimeException("Log4jdbc DriverManager.Initialize: load " + Driver.class + " failed : " + ex);
		}
	}
}
