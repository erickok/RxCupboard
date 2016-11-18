package nl.nl2312.rxcupboard2;

public abstract class DatabaseChange<T> {

	public static <T> DatabaseInsert<T> insert(final T entity) {
		DatabaseInsert<T> insert = new DatabaseInsert<>();
		insert.entity = entity;
		return insert;
	}

	public static <T> DatabaseUpdate<T> update(final T entity) {
		DatabaseUpdate<T> update = new DatabaseUpdate<>();
		update.entity = entity;
		return update;
	}

	public static <T> DatabaseDelete<T> delete(final T entity) {
		DatabaseDelete<T> delete = new DatabaseDelete<>();
		delete.entity = entity;
		return delete;
	}

	T entity;

	public Class<?> entityClass() {
		return entity.getClass();
	}

	public T entity() {
		return entity;
	}

	public static final class DatabaseInsert<T> extends DatabaseChange<T> {}

	public static final class DatabaseUpdate<T> extends DatabaseChange<T> {}

	public static final class DatabaseDelete<T> extends DatabaseChange<T> {}

}
