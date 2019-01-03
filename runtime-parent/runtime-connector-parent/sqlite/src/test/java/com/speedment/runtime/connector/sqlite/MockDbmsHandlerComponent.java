package com.speedment.runtime.connector.sqlite;

import com.speedment.runtime.connector.sqlite.internal.SqliteDbmsType;
import com.speedment.runtime.core.component.DbmsHandlerComponent;
import com.speedment.runtime.core.db.DbmsType;

import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * @author Emil Forslund
 * @since  3.1.10
 */
public class MockDbmsHandlerComponent implements DbmsHandlerComponent {

    private DbmsType dbmsType;

    @Override
    public void install(DbmsType dbmsType) {
        if (dbmsType instanceof SqliteDbmsType) {
            this.dbmsType = dbmsType;
        } else {
            throw new IllegalArgumentException(format(
                "Expected only the %s to be installed, but got '%s'.",
                SqliteDbmsType.class.getSimpleName(),
                dbmsType.getClass().getSimpleName()));
        }
    }

    @Override
    public Stream<DbmsType> supportedDbmsTypes() {
        return Stream.of(dbmsType);
    }

    @Override
    public Optional<DbmsType> findByName(String dbmsTypeName) {
        if (dbmsType == null) {
            throw new IllegalStateException("No DbmsTypes installed yet.");
        } else if (dbmsType.getName().equals(dbmsTypeName)) {
            return Optional.of(dbmsType);
        } else {
            throw new IllegalArgumentException(format(
                "DbmsType '%s' not installed.", dbmsTypeName));
        }
    }
}
