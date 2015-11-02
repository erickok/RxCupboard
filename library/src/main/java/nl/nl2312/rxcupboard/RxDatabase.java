package nl.nl2312.rxcupboard;

import android.database.sqlite.SQLiteDatabase;

import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.DatabaseCompartment;
import nl.qbusict.cupboard.convert.EntityConverter;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

public class RxDatabase {

	private final Cupboard cupboard;
	private final DatabaseCompartment dc;
	private final SQLiteDatabase db;
	private final PublishSubject<DatabaseChange> triggers = PublishSubject.create();

	RxDatabase(Cupboard cupboard, DatabaseCompartment dc, SQLiteDatabase db) {
		this.cupboard = cupboard;
		this.dc = dc;
		this.db = db;
	}

	public Observable<DatabaseChange> changes() {
		return triggers.asObservable();
	}

	public <T> Observable<DatabaseChange<T>> changes(final Class<T> entityClass) {
		return triggers.filter(new Func1<DatabaseChange, Boolean>() {
			@Override
			public Boolean call(DatabaseChange event) {
				// Only let through change events for a specific table/class
				return entityClass.isAssignableFrom(event.entityClass());
			}
		}).map(new Func1<DatabaseChange, DatabaseChange<T>>() {
			@Override
			public DatabaseChange<T> call(DatabaseChange raw) {
				// Cast as we are now sure to have only DatabaseChange events of type T
				//noinspection unchecked
				return raw;
			}
		}).asObservable();
	}

	@SuppressWarnings("unchecked") // Cupboard EntityConverter type is lost as it only accepts Class<?>
	public <T> long put(T entity) {
		EntityConverter<T> entityConverter = cupboard.getEntityConverter((Class<T>) entity.getClass());
		Long existing = entityConverter.getId(entity);
		long inserted = dc.put(entity);
		if (existing == null) {
			triggers.onNext(DatabaseChange.insert(entity));
			return inserted;
		} else {
			triggers.onNext(DatabaseChange.update(entity));
			return existing;
		}
	}

	public <T> Observable<T> putRx(final T entity) {
		return Observable.defer(new Func0<Observable<T>>() {
			@Override
			public Observable<T> call() {
				put(entity);
				return Observable.just(entity);
			}
		});
	}

	public <T> Action1<T> put() {
		return new Action1<T>() {
			@Override
			public void call(T t) {
				put(t);
			}
		};
	}

	public <T> boolean delete(T entity) {
		boolean result = dc.delete(entity);
		if (result) {
			triggers.onNext(DatabaseChange.delete(entity));
		}
		return result;
	}

	public <T> Observable<T> deleteRx(final T entity) {
		return Observable.defer(new Func0<Observable<T>>() {
			@Override
			public Observable<T> call() {
				delete(entity);
				return Observable.just(entity);
			}
		});
	}

	public <T> boolean delete(Class<T> entityClass, long id) {
		boolean result;
		if (triggers.hasObservers()) {
			// We have subscribers to database change events, so we need to look up the item to report it back
			T entity = dc.get(entityClass, id);
			result = dc.delete(entity);
			if (result) {
				triggers.onNext(DatabaseChange.delete(entity));
			}
		} else {
			// Straightforward delete without change propagation
			result = dc.delete(entityClass, id);
		}
		return result;
	}

	public <T> Action1<T> delete() {
		return new Action1<T>() {
			@Override
			public void call(T t) {
				delete(t);
			}
		};
	}

	public <T> Action1<Long> delete(final Class<T> entityClass) {
		return new Action1<Long>() {
			@Override
			public void call(Long t) {
				delete(entityClass, t);
			}
		};
	}

	public <T> Observable<T> get(final Class<T> entityClass, final long id) {
		return Observable.defer(new Func0<Observable<T>>() {
			@Override
			public Observable<T> call() {
				return Observable.just(dc.get(entityClass, id));
			}
		});
	}

	public <T> Observable<T> query(Class<T> entityClass) {
		return Observable.from(dc.query(entityClass).query());
	}

	public <T> Observable<T> query(Class<T> entityClass, String selection, String... args) {
		return Observable.from(dc.query(entityClass).withSelection(selection, args).query());
	}

	public <T> Observable<T> query(DatabaseCompartment.QueryBuilder<T> preparedQuery) {
		return Observable.from(preparedQuery.query());
	}

	public <T> Observable<Long> count(final Class<T> entityClass) {
		return Observable.defer(new Func0<Observable<Long>>() {
			@Override
			public Observable<Long> call() {
				String table = cupboard.getTable(entityClass);
				return Observable.just(db.compileStatement("select count(*) from " + table).simpleQueryForLong());
			}
		});
	}

}
