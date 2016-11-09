package nl.nl2312.rxcupboard;

import io.reactivex.functions.Consumer;

public abstract class OnDatabaseChange<T> implements Consumer<DatabaseChange<T>> {

    public void onUpdate(T entity) {}

    public void onInsert(T entity) {}

    public void onDelete(T entity) {}

    @Override
    public void accept(DatabaseChange<T> databaseChange) throws Exception {
        if (databaseChange instanceof DatabaseChange.DatabaseUpdate) {
            onUpdate(databaseChange.entity());
        } else if (databaseChange instanceof DatabaseChange.DatabaseInsert) {
            onInsert(databaseChange.entity());
        } else if (databaseChange instanceof DatabaseChange.DatabaseDelete) {
            onDelete(databaseChange.entity());
        }
    }

}
