package fu.swt301.sms.dao;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionProvider {
    Connection getConnection() throws ClassNotFoundException, SQLException;
}
