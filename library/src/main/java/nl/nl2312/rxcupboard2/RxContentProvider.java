package nl.nl2312.rxcupboard2;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;

import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.ProviderCompartment;

public class RxContentProvider {

	private final ProviderCompartment provider;
	private final Uri uri;

	RxContentProvider(Cupboard cupboard, Context context, Uri uri) {
		this.provider = cupboard.withContext(context);
		this.uri = uri;
	}

	public <T> Flowable<T> put(T entity) {
		provider.put(uri, entity);
		return Flowable.just(entity);
	}

	public <T> Consumer<T> put() {
		return new Consumer<T>() {
			@Override
			public void accept(T t) throws Exception {
				put(t);
			}
		};
	}

	public <T> Flowable<T> delete(T entity) {
		provider.delete(uri, entity);
		return Flowable.just(entity);
	}

	public <T> Flowable<Integer> deleteCount(T entity) {
		return Flowable.just(provider.delete(uri, entity));
	}

	public <T> Consumer<T> delete() {
		return new Consumer<T>() {
			@Override
			public void accept(T t) throws Exception {
				delete(t);
			}
		};
	}

	public <T> Flowable<T> get(final Class<T> entityClass, final long id) {
		return Flowable.fromCallable(new Callable<T>() {
			@Override
			public T call() throws Exception {
				Uri getUri = ContentUris.withAppendedId(uri, id);
				return provider.get(getUri, entityClass);
			}
		});
	}

	public <T> Flowable<T> query(Class<T> entityClass) {
		return Flowable.fromIterable(provider.query(uri, entityClass).query());
	}

	public <T> Flowable<T> query(Class<T> entityClass, String selection, String... args) {
		return Flowable.fromIterable(provider.query(uri, entityClass).withSelection(selection, args).query());
	}

	public <T> Flowable<T> query(ProviderCompartment.QueryBuilder<T> preparedQuery) {
		return Flowable.fromIterable(preparedQuery.query());
	}

}
