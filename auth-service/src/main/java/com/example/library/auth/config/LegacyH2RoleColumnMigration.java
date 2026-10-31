package com.example.library.auth.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Types;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyH2RoleColumnMigration implements ApplicationRunner {
    private final DataSource dataSource;

    public LegacyH2RoleColumnMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            if (!"H2".equals(connection.getMetaData().getDatabaseProductName())) {
                return;
            }
            try (var columns = connection.getMetaData().getColumns(
                    connection.getCatalog(), null, "USER_ACCOUNTS", "ROLE")) {
                if (columns.next() && columns.getInt("DATA_TYPE") != Types.VARCHAR) {
                    try (var statement = connection.createStatement()) {
                        statement.execute("ALTER TABLE user_accounts ALTER COLUMN role SET DATA TYPE VARCHAR(16)");
                    }
                }
            }
        }
    }
}
