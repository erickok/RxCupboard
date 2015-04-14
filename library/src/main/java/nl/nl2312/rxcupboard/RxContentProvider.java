package nl.nl2312.rxcupboard;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;

import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.ProviderCompartment;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;

public class RxContentProvider {

	private final ProviderCompartment provider;
	private final Uri uri;

	RxContentProvider(Cupboard cupboard, Context context, Uri uri) {
		this.provider = cupboard.withContext(context);
		this.uri = uri;
	}

	public <T> void put(T entity) {
		provider.put(uri, entity);
	}

	public <T> Action1<T> put() {
		return new Action1<T>() {
			@Override
			public void call(T t) {
				put(t);
			}
		};
	}

	public <T> int delete(T entity) {
		return provider.delete(uri, entity);
	}

	public <T> Action1<T> delete() {
		return new Action1<T>() {
			@Override
			public void call(T t) {
				delete(t);
			}
		};
	}

	public <T> Observable<T> get(final Class<T> entityClass, final long id) {
		return Observable.defer(new Func0<Observable<T>>() {
			@Override
			public Observable<T> call() {
				Uri getUri = ContentUris.withAppendedId(uri, id);
				return Observable.just(provider.get(getUri, entityClass));
			}
		});
	}

	public <T> Observable<T> query(Class<T> entityClass) {
		return Observable.from(provider.query(uri, entityClass).query());
	}

	public <T> Observable<T> query(Class<T> entityClass, String selection, String... args) {
		return Observable.from(provider.query(uri, entityClass).withSelection(selection, args).query());
	}

	public <T> Observable<T> query(ProviderCompartment.QueryBuilder<T> preparedQuery) {
		return Observable.from(preparedQuery.query());
	}

}
