package nl.nl2312.rxcupboard;

import rx.functions.Action1;

public abstract class OnDatabaseChange<T> implements Action1<DatabaseChange<T>> {

    public void onUpdate(T entity) {}

    public void onInsert(T entity) {}

    public void onDelete(T entity) {}

    @Override
    public void call(DatabaseChange<T> databaseChange) {
        if (databaseChange instanceof DatabaseChange.DatabaseUpdate) {
            onUpdate(databaseChange.entity());
        } else if (databaseChange instanceof DatabaseChange.DatabaseInsert) {
            onInsert(databaseChange.entity());
        } else if (databaseChange instanceof DatabaseChange.DatabaseDelete) {
            onDelete(databaseChange.entity());
        }
    }
}
