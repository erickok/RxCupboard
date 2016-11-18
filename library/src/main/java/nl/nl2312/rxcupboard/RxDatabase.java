package nl.nl2312.rxcupboard;

import android.database.sqlite.SQLiteDatabase;

import org.reactivestreams.Publisher;

import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Single;
import io.reactivex.annotations.Experimental;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.processors.PublishProcessor;
import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.DatabaseCompartment;
import nl.qbusict.cupboard.QueryResultIterable;
import nl.qbusict.cupboard.convert.EntityConverter;

public class RxDatabase {

	private final Cupboard cupboard;
	private final DatabaseCompartment dc;
	private final SQLiteDatabase db;
	private final PublishProcessor<DatabaseChange> triggers = PublishProcessor.create();

	RxDatabase(Cupboard cupboard, DatabaseCompartment dc, SQLiteDatabase db) {
		this.cupboard = cupboard;
		this.dc = dc;
		this.db = db;
	}

	public Flowable<DatabaseChange> changes() {
		return triggers.hide();
	}

	public <T> Flowable<DatabaseChange<T>> changes(final Class<T> entityClass) {
		return triggers.filter(isEventOf(entityClass)).map(new Function<DatabaseChange, DatabaseChange<T>>() {
			@Override
			public DatabaseChange<T> apply(DatabaseChange raw) throws Exception {
				// Cast as we are now sure to have only DatabaseChange events of type T
				//noinspection unchecked
				return raw;
			}
		}).hide();
	}

	public Flowable<DatabaseChange.DatabaseInsert> inserts() {
		return triggers.ofType(DatabaseChange.DatabaseInsert.class).hide();
	}

	public <T> Flowable<DatabaseChange.DatabaseInsert<T>> inserts(final Class<T> entityClass) {
		return triggers.filter(isEventOf(entityClass)).ofType(DatabaseChange.DatabaseInsert.class).map(new Function<DatabaseChange
				.DatabaseInsert, DatabaseChange.DatabaseInsert<T>>() {
			@Override
			public DatabaseChange.DatabaseInsert<T> apply(DatabaseChange.DatabaseInsert raw) throws Exception {
				// Cast as we are now sure to have only DatabaseChange events of type T
				//noinspection unchecked
				return raw;
			}
		}).hide();
	}

	public Flowable<DatabaseChange.DatabaseUpdate> updates() {
		return triggers.ofType(DatabaseChange.DatabaseUpdate.class).hide();
	}

	public <T> Flowable<DatabaseChange.DatabaseUpdate<T>> updates(final Class<T> entityClass) {
		return triggers.filter(isEventOf(entityClass)).ofType(DatabaseChange.DatabaseUpdate.class).map(new Function<DatabaseChange
				.DatabaseUpdate, DatabaseChange.DatabaseUpdate<T>>() {
			@Override
			public DatabaseChange.DatabaseUpdate<T> apply(DatabaseChange.DatabaseUpdate raw) throws Exception {
				// Cast as we are now sure to have only DatabaseChange events of type T
				//noinspection unchecked
				return raw;
			}
		}).hide();
	}

	public Flowable<DatabaseChange.DatabaseDelete> deletes() {
		return triggers.ofType(DatabaseChange.DatabaseDelete.class).hide();
	}

	public <T> Flowable<DatabaseChange.DatabaseDelete<T>> deletes(final Class<T> entityClass) {
		return triggers.filter(isEventOf(entityClass)).ofType(DatabaseChange.DatabaseDelete.class).map(new Function<DatabaseChange
				.DatabaseDelete, DatabaseChange.DatabaseDelete<T>>() {
			@Override
			public DatabaseChange.DatabaseDelete<T> apply(DatabaseChange.DatabaseDelete raw) throws Exception {
				// Cast as we are now sure to have only DatabaseChange events of type T
				//noinspection unchecked
				return raw;
			}
		}).hide();
	}

	private <T> Predicate<DatabaseChange> isEventOf(final Class<T> entityClass) {
		return new Predicate<DatabaseChange>() {
			@Override
			public boolean test(DatabaseChange event) throws Exception {
				// Only let through change events for a specific table/class
				return entityClass.isAssignableFrom(event.entityClass());
			}
		};
	}


	@SuppressWarnings("unchecked") // Cupboard EntityConverter type is lost as it only accepts Class<?>
	@Deprecated
	public <T> long putDirect(T entity) {
		EntityConverter<T> entityConverter = cupboard.getEntityConverter((Class<T>) entity.getClass());
		Long existing = entityConverter.getId(entity);
		long inserted = dc.put(entity);
		if (existing == null) {
			if (triggers.hasSubscribers()) {
				triggers.onNext(DatabaseChange.insert(entity));
			}
			return inserted;
		} else {
			if (triggers.hasSubscribers()) {
				triggers.onNext(DatabaseChange.update(entity));
			}
			return existing;
		}
	}

	public <T> Single<T> put(final T entity) {
		return Single.fromCallable(new Callable<T>() {
			@Override
			public T call() throws Exception {
				putDirect(entity);
				return entity;
			}
		});
	}

	public <T> Consumer<T> put() {
		return new Consumer<T>() {
			@Override
			public void accept(T t) throws Exception {
				putDirect(t);
			}
		};
	}

	@Deprecated
	public <T> boolean deleteDirect(T entity) {
		boolean result = dc.delete(entity);
		if (result && triggers.hasSubscribers()) {
			triggers.onNext(DatabaseChange.delete(entity));
		}
		return result;
	}

	public <T> Single<T> delete(final T entity) {
		return Single.fromCallable(new Callable<T>() {
			@Override
			public T call() throws Exception {
				deleteDirect(entity);
				return entity;
			}
		});
	}

	@Deprecated
	public <T> boolean deleteDirect(Class<T> entityClass, long id) {
		boolean result;
		if (triggers.hasSubscribers()) {
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

	public <T> Single<Boolean> delete(final Class<T> entityClass, final long id) {
		return Single.fromCallable(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return deleteDirect(entityClass, id);
			}
		});
	}

	@Experimental
	public <T> Single<Long> delete(final Class<T> entityClass, final String selection, final String... selectionArgs) {
		if (triggers.hasSubscribers()) {
			return query(entityClass, selection, selectionArgs)
					.doOnNext(this.<T>delete())
					.count();
		} else {
			return Single.fromCallable(new Callable<Long>() {
				@Override
				public Long call() throws Exception {
					return (long) dc.delete(entityClass, selection, selectionArgs);
				}
			});
		}
	}

	@Experimental
	public <T> Single<Long> deleteAll(final Class<T> entityClass) {
		return delete(entityClass, "");
	}

	public <T> Consumer<T> delete() {
		return new Consumer<T>() {
			@Override
			public void accept(T t) throws Exception {
				deleteDirect(t);
			}
		};
	}

	public <T> Single<T> get(final Class<T> entityClass, final long id) {
		return Single.fromCallable(new Callable<T>() {
			@Override
			public T call() throws Exception {
				return dc.get(entityClass, id);
			}
		});
	}

	public <T> Flowable<T> query(Class<T> entityClass) {
		QueryResultIterable<T> iterable = dc.query(entityClass).query();
		return Flowable.fromIterable(iterable).compose(autoClose(iterable));
	}

	public <T> Flowable<T> query(Class<T> entityClass, String selection, String... args) {
		QueryResultIterable<T> iterable = dc.query(entityClass).withSelection(selection, args).query();
		return Flowable.fromIterable(iterable).compose(autoClose(iterable));
	}

	public <T> Flowable<T> query(DatabaseCompartment.QueryBuilder<T> preparedQuery) {
		QueryResultIterable<T> iterable = preparedQuery.query();
		return Flowable.fromIterable(iterable).compose(autoClose(iterable));
	}

	private <T> FlowableTransformer<T, T> autoClose(final QueryResultIterable<T> iterable) {
		return new FlowableTransformer<T, T>() {
			@Override
			public Publisher<T> apply(Flowable<T> upstream) {
				return upstream.doOnTerminate(new Action() {
					@Override
					public void run() throws Exception {
						// Stream terminates (completed or on error): close the cursor
						iterable.close();
					}
				}).doOnCancel(new Action() {
					@Override
					public void run() throws Exception {
						// Cancelled subscription (manual unsubscribe or via some operator such as take()): close the cursor
						iterable.close();
					}
				});
			}
		};
	}

	public <T> DatabaseCompartment.QueryBuilder<T> buildQuery(Class<T> entityClass) {
		return dc.query(entityClass);
	}

	public <T> Single<Long> count(final Class<T> entityClass) {
		return Single.fromCallable(new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				String table = cupboard.getTable(entityClass);
				return db.compileStatement("select count(*) from " + table).simpleQueryForLong();
			}
		});
	}

}
