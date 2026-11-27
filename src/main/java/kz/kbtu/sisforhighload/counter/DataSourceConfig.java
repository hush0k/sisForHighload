package kz.kbtu.sisforhighload.counter;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private static final String PRIMARY = "primary";

    private static final String REPLICA = "replica";

    private final TuningProperties tuning;

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        DataSource primary = create(properties, properties.getUrl());
        if (!tuning.hasReplica()) {
            return primary;
        }
        return routing(primary, create(properties, tuning.replicaUrl()));
    }

    private DataSource create(DataSourceProperties properties, String url) {
        if (!tuning.hasPool()) {
            return new DriverManagerDataSource(url, properties.getUsername(), properties.getPassword());
        }
        HikariDataSource pool = new HikariDataSource();
        pool.setJdbcUrl(url);
        pool.setUsername(properties.getUsername());
        pool.setPassword(properties.getPassword());
        pool.setMaximumPoolSize(tuning.poolSize());
        return pool;
    }

    private DataSource routing(DataSource primary, DataSource replica) {
        AbstractRoutingDataSource router = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? REPLICA : PRIMARY;
            }
        };
        router.setTargetDataSources(Map.of(PRIMARY, primary, REPLICA, replica));
        router.setDefaultTargetDataSource(primary);
        router.afterPropertiesSet();
        return new LazyConnectionDataSourceProxy(router);
    }
}
