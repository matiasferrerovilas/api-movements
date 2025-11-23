package api.expenses.expenses.config

import jakarta.annotation.PostConstruct
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.DirectoryResourceAccessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration

import javax.sql.DataSource
import java.sql.Connection

@TestConfiguration
class LiquibaseConfig {

    @Autowired
    DataSource dataSource

    @PostConstruct
    def initializeDatabase() {
        Connection connection = dataSource.getConnection();
        Liquibase liquibase = new Liquibase(
                "src/main/resources/db/changelog/changelog.yaml",
                new DirectoryResourceAccessor(new File(".")),
                new JdbcConnection(connection)
        );
        liquibase.dropAll()
        liquibase.update(new Contexts())
    }
}
