package nl.nl2312.rxcupboard2;

import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CupboardBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ChangesTest {

	private static final String TEST_DATABASE = "RxCupboardTest.db";

	private SQLiteDatabase db;
	private RxDatabase rxDatabase;

	@Before
	public void setUp() throws Exception {
		Cupboard cupboard = new CupboardBuilder().build();
		cupboard.register(TestEntity.class);
		cupboard.register(TestEntity2.class);
		InstrumentationRegistry.getTargetContext().deleteDatabase(TEST_DATABASE);
		db = new TestDbHelper(InstrumentationRegistry.getTargetContext(), cupboard, TEST_DATABASE).getWritableDatabase();
		rxDatabase = RxCupboard.with(cupboard, db);
	}

	@Test
	public void changes_allAndSpecific() {

		// Add observable to all database changes and one for only changes in TestEntity2
		final AtomicInteger changeAllCount = new AtomicInteger();
		final AtomicInteger changeSpecificCount = new AtomicInteger();
		Disposable allChanges = rxDatabase.changes().subscribe(new Consumer<DatabaseChange>() {
			@Override
			public void accept(DatabaseChange databaseChange) throws Exception {
				assertTrue(databaseChange.entity() != null);
				changeAllCount.getAndIncrement();
			}
		});
		Disposable specificChanges = rxDatabase.changes(TestEntity2.class).subscribe(new Consumer<DatabaseChange<TestEntity2>>() {
			@Override
			public void accept(DatabaseChange<TestEntity2> databaseChange) throws Exception {
				assertTrue(databaseChange.entity() != null);
				changeSpecificCount.getAndIncrement();
			}
		});

		long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = time;
		final TestEntity2 testEntity2 = new TestEntity2();
		testEntity2.date = new Date(time);

		// Simple insert
		rxDatabase.putDirect(testEntity);
		rxDatabase.putDirect(testEntity2);
		assertEquals(2, changeAllCount.get());
		assertEquals(1, changeSpecificCount.get());

		// Simple update
		long updatedTime = System.currentTimeMillis();
		testEntity.string = "Updated";
		testEntity.time = updatedTime;
		testEntity2.date = new Date(updatedTime);
		rxDatabase.putDirect(testEntity);
		rxDatabase.putDirect(testEntity2);
		assertEquals(4, changeAllCount.get());
		assertEquals(2, changeSpecificCount.get());

		// Simple delete
		rxDatabase.deleteDirect(testEntity);
		rxDatabase.deleteDirect(testEntity2);
		assertEquals(6, changeAllCount.get());
		assertEquals(3, changeSpecificCount.get());

		// Non-existing delete call causes no changes
		rxDatabase.deleteDirect(testEntity);
		rxDatabase.deleteDirect(testEntity2);
		assertEquals(6, changeAllCount.get());
		assertEquals(3, changeSpecificCount.get());

		allChanges.dispose();
		specificChanges.dispose();

	}

	@Test
	public void changes_onSubscribed() {

		// Add observable to all database changes
		final AtomicInteger changeCount = new AtomicInteger();
		Disposable changes = rxDatabase.changes().subscribe(new Consumer<DatabaseChange>() {
			@Override
			public void accept(DatabaseChange databaseChange) throws Exception {
				changeCount.getAndIncrement();
			}
		});

		final TestEntity2 testEntity2 = new TestEntity2();
		testEntity2.date = new Date(System.currentTimeMillis());

		// Simple insert
		rxDatabase.putDirect(testEntity2);
		assertEquals(1, changeCount.get());

		// Simple update
		testEntity2.date = new Date(System.currentTimeMillis());
		rxDatabase.putDirect(testEntity2);
		assertEquals(2, changeCount.get());

		// Simple delete
		rxDatabase.deleteDirect(testEntity2);
		assertEquals(3, changeCount.get());

		// Unsubscribe from changes and repeat operations
		changes.dispose();

		final TestEntity2 pausedEntity2 = new TestEntity2();
		pausedEntity2.date = new Date(System.currentTimeMillis());

		// Simple insert
		rxDatabase.putDirect(pausedEntity2);
		assertEquals(3, changeCount.get());

		// Simple update
		pausedEntity2.date = new Date(System.currentTimeMillis());
		rxDatabase.putDirect(pausedEntity2);
		assertEquals(3, changeCount.get());

		// Simple delete
		rxDatabase.deleteDirect(pausedEntity2);
		assertEquals(3, changeCount.get());

	}

	@Test
	public void changes_content() {

		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = System.currentTimeMillis();

		// Count the different types of changes
		final AtomicInteger changesCount = new AtomicInteger();
		final AtomicInteger insertCount = new AtomicInteger();
		final AtomicInteger updateCount = new AtomicInteger();
		final AtomicInteger deleteCount = new AtomicInteger();
		CompositeDisposable allChanges = new CompositeDisposable();

		allChanges.add(rxDatabase.changes().subscribe(new Consumer<DatabaseChange>() {
			@Override
			public void accept(DatabaseChange databaseChange) throws Exception {
				changesCount.getAndIncrement();
				assertTrue(databaseChange.entity() instanceof TestEntity);
				TestEntity changed = (TestEntity) databaseChange.entity();
				assertNotNull(changed._id);
				assertEquals(testEntity, changed);
				assertEquals(testEntity.string, changed.string);
				assertEquals(testEntity.time, changed.time);
			}
		}));
		allChanges.add(rxDatabase.changes().filter(new Predicate<DatabaseChange>() {
			@Override
			public boolean test(DatabaseChange databaseChange) throws Exception {
				return databaseChange instanceof DatabaseChange.DatabaseInsert;
			}
		}).subscribe(new Consumer<DatabaseChange>() {
			@Override
			public void accept(DatabaseChange databaseChange) throws Exception {
				insertCount.getAndIncrement();
			}
		}));
		allChanges.add(rxDatabase.changes().filter(new Predicate<DatabaseChange>() {
			@Override
			public boolean test(DatabaseChange databaseChange) throws Exception {
				return databaseChange instanceof DatabaseChange.DatabaseUpdate;
			}
		}).subscribe(new Consumer<DatabaseChange>() {
			@Override
			public void accept(DatabaseChange databaseChange) throws Exception {
				updateCount.getAndIncrement();
			}
		}));
		allChanges.add(rxDatabase.changes().filter(new Predicate<DatabaseChange>() {
			@Override
			public boolean test(DatabaseChange databaseChange) throws Exception {
				return databaseChange instanceof DatabaseChange.DatabaseDelete;
			}
		}).subscribe(new Consumer<DatabaseChange>() {
			@Override
			public void accept(DatabaseChange databaseChange) throws Exception {
				deleteCount.getAndIncrement();
			}
		}));

		// Simple insert/update/delete
		rxDatabase.putDirect(testEntity);
		testEntity.string = "Updated";
		testEntity.time = System.currentTimeMillis();
		rxDatabase.putDirect(testEntity);
		rxDatabase.deleteDirect(testEntity);

		// Action insert/update/delete
		testEntity._id = null;
		testEntity.string = "Renew";
		testEntity.time = System.currentTimeMillis();
		Flowable.just(testEntity).doOnNext(rxDatabase.put()).doOnNext(new Consumer<TestEntity>() {
			@Override
			public void accept(TestEntity testEntity) throws Exception {
				testEntity.string = "Renew updated";
			}
		}).doOnNext(rxDatabase.put()).doOnNext(rxDatabase.delete()).subscribe();

		assertEquals(6, changesCount.intValue());
		assertEquals(2, insertCount.intValue());
		assertEquals(2, updateCount.intValue());
		assertEquals(2, deleteCount.intValue());

		allChanges.clear();

	}

	@Test
	public void changes_onDatabaseChangeAction() {

		// Observe all database changes using the OnDatabaseChange default action
		final AtomicInteger insertCount = new AtomicInteger();
		final AtomicInteger updateCount = new AtomicInteger();
		final AtomicInteger deleteCount = new AtomicInteger();
		Disposable changes = rxDatabase.changes(TestEntity.class).subscribe(new OnDatabaseChange<TestEntity>() {
			@Override
			public void onInsert(TestEntity entity) {
				assertTrue(entity != null);
				insertCount.getAndIncrement();
			}

			@Override
			public void onUpdate(TestEntity entity) {
				assertTrue(entity != null);
				updateCount.getAndIncrement();
			}

			@Override
			public void onDelete(TestEntity entity) {
				assertTrue(entity != null);
				deleteCount.getAndIncrement();
			}
		});

		long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = time;

		// Simple insert
		rxDatabase.putDirect(testEntity);
		assertEquals(1, insertCount.get());
		assertEquals(0, updateCount.get());
		assertEquals(0, deleteCount.get());

		// Simple update
		long updatedTime = System.currentTimeMillis();
		testEntity.string = "Updated";
		testEntity.time = updatedTime;
		rxDatabase.putDirect(testEntity);
		assertEquals(1, insertCount.get());
		assertEquals(1, updateCount.get());
		assertEquals(0, deleteCount.get());

		// Simple delete
		rxDatabase.deleteDirect(testEntity);
		assertEquals(1, insertCount.get());
		assertEquals(1, updateCount.get());
		assertEquals(1, deleteCount.get());

		// Non-existing delete call causes no changes
		rxDatabase.deleteDirect(testEntity);
		assertEquals(1, insertCount.get());
		assertEquals(1, updateCount.get());
		assertEquals(1, deleteCount.get());

		changes.dispose();

	}

	@Test
	public void inserts_allAndSpecific() {

		// Add observable to all database inserts and one for only inserts in TestEntity2
		final AtomicInteger insertAllCount = new AtomicInteger();
		final AtomicInteger insertSpecificCount = new AtomicInteger();
		Disposable allInserts = rxDatabase.inserts().subscribe(new Consumer<DatabaseChange.DatabaseInsert>() {
			@Override
			public void accept(DatabaseChange.DatabaseInsert databaseInsert) throws Exception {
				assertTrue(databaseInsert.entity() != null);
				insertAllCount.getAndIncrement();
			}
		});
		Disposable specificInserts = rxDatabase.inserts(TestEntity2.class).subscribe(new Consumer<DatabaseChange.DatabaseInsert<TestEntity2>>() {
			@Override
			public void accept(DatabaseChange.DatabaseInsert<TestEntity2> databaseInsert) throws Exception {
				assertTrue(databaseInsert.entity() != null);
				insertSpecificCount.getAndIncrement();
			}
		});

		long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = time;
		final TestEntity2 testEntity2 = new TestEntity2();
		testEntity2.date = new Date(time);

		// Simple insert
		rxDatabase.putDirect(testEntity);
		rxDatabase.putDirect(testEntity2);
		assertEquals(2, insertAllCount.get());
		assertEquals(1, insertSpecificCount.get());

		// Simple update (no change in insert counts)
		long updatedTime = System.currentTimeMillis();
		testEntity.string = "Updated";
		testEntity.time = updatedTime;
		testEntity2.date = new Date(updatedTime);
		rxDatabase.putDirect(testEntity);
		rxDatabase.putDirect(testEntity2);
		assertEquals(2, insertAllCount.get());
		assertEquals(1, insertSpecificCount.get());

		// Simple delete (no change in insert counts)
		rxDatabase.deleteDirect(testEntity);
		rxDatabase.deleteDirect(testEntity2);
		assertEquals(2, insertAllCount.get());
		assertEquals(1, insertSpecificCount.get());

		allInserts.dispose();
		specificInserts.dispose();

	}

	@Test
	public void updates_allAndSpecific() {

		// Add observable to all database inserts and one for only inserts in TestEntity2
		final AtomicInteger updateAllCount = new AtomicInteger();
		final AtomicInteger updateSpecificCount = new AtomicInteger();
		Disposable allUpdates = rxDatabase.updates().subscribe(new Consumer<DatabaseChange.DatabaseUpdate>() {
			@Override
			public void accept(DatabaseChange.DatabaseUpdate databaseUpdate) throws Exception {
				assertTrue(databaseUpdate.entity() != null);
				updateAllCount.getAndIncrement();
			}
		});
		Disposable specificUpdates = rxDatabase.updates(TestEntity2.class).subscribe(new Consumer<DatabaseChange.DatabaseUpdate<TestEntity2>>() {
			@Override
			public void accept(DatabaseChange.DatabaseUpdate<TestEntity2> databaseUpdate) throws Exception {
				assertTrue(databaseUpdate.entity() != null);
				updateSpecificCount.getAndIncrement();
			}
		});

		long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = time;
		final TestEntity2 testEntity2 = new TestEntity2();
		testEntity2.date = new Date(time);

		// Simple insert
		rxDatabase.putDirect(testEntity);
		rxDatabase.putDirect(testEntity2);
		assertEquals(0, updateAllCount.get());
		assertEquals(0, updateSpecificCount.get());

		// Simple update (no change in insert counts)
		long updatedTime = System.currentTimeMillis();
		testEntity.string = "Updated";
		testEntity.time = updatedTime;
		testEntity2.date = new Date(updatedTime);
		rxDatabase.putDirect(testEntity);
		rxDatabase.putDirect(testEntity2);
		assertEquals(2, updateAllCount.get());
		assertEquals(1, updateSpecificCount.get());

		// Simple delete (no change in insert counts)
		rxDatabase.deleteDirect(testEntity);
		rxDatabase.deleteDirect(testEntity2);
		assertEquals(2, updateAllCount.get());
		assertEquals(1, updateSpecificCount.get());

		allUpdates.dispose();
		specificUpdates.dispose();

	}

	@Test
	public void deletes_allAndSpecific() {

		// Add observable to all database deletes and one for only deletes in TestEntity2
		final AtomicInteger deleteAllCount = new AtomicInteger();
		final AtomicInteger deleteSpecificCount = new AtomicInteger();
		Disposable allDeletes = rxDatabase.deletes().subscribe(new Consumer<DatabaseChange.DatabaseDelete>() {
			@Override
			public void accept(DatabaseChange.DatabaseDelete databaseDelete) throws Exception {
				assertTrue(databaseDelete.entity() != null);
				deleteAllCount.getAndIncrement();
			}
		});
		Disposable specificDeletes = rxDatabase.deletes(TestEntity2.class).subscribe(new Consumer<DatabaseChange.DatabaseDelete<TestEntity2>>() {
			@Override
			public void accept(DatabaseChange.DatabaseDelete<TestEntity2> databaseDelete) throws Exception {
				assertTrue(databaseDelete.entity() != null);
				deleteSpecificCount.getAndIncrement();
			}
		});

		long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = time;
		final TestEntity2 testEntity2 = new TestEntity2();
		testEntity2.date = new Date(time);

		// Simple insert
		rxDatabase.putDirect(testEntity);
		rxDatabase.putDirect(testEntity2);
		assertEquals(0, deleteAllCount.get());
		assertEquals(0, deleteSpecificCount.get());

		// Simple update (no change in insert counts)
		long updatedTime = System.currentTimeMillis();
		testEntity.string = "Updated";
		testEntity.time = updatedTime;
		testEntity2.date = new Date(updatedTime);
		rxDatabase.putDirect(testEntity);
		rxDatabase.putDirect(testEntity2);
		assertEquals(0, deleteAllCount.get());
		assertEquals(0, deleteSpecificCount.get());

		// Simple delete (no change in insert counts)
		rxDatabase.deleteDirect(testEntity);
		rxDatabase.deleteDirect(testEntity2);
		assertEquals(2, deleteAllCount.get());
		assertEquals(1, deleteSpecificCount.get());

		// Non-existing delete call causes no changes
		rxDatabase.deleteDirect(testEntity);
		rxDatabase.deleteDirect(testEntity2);
		assertEquals(2, deleteAllCount.get());
		assertEquals(1, deleteSpecificCount.get());

		allDeletes.dispose();
		specificDeletes.dispose();

	}

	@After
	public void tearDown() throws Exception {
		db.close();
	}

}
