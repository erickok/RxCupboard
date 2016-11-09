package nl.nl2312.rxcupboard;

import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CupboardBuilder;

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

	/*@Test
	public void testAllAndSpecificChanges() {

		long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = time;
		final TestEntity2 testEntity2 = new TestEntity2();
		testEntity2.date = new Date(time);

		// Simple insert
		rxDatabase.put(testEntity);
		rxDatabase.put(testEntity2);
		assertEquals(2, changeAllCount.get());
		assertEquals(1, changeSpecificCount.get());

		// Simple update
		long updatedTime = System.currentTimeMillis();
		testEntity.string = "Updated";
		testEntity.time = updatedTime;
		testEntity2.date = new Date(updatedTime);
		rxDatabase.put(testEntity);
		rxDatabase.put(testEntity2);
		assertEquals(4, changeAllCount.get());
		assertEquals(2, changeSpecificCount.get());

		// Simple delete
		rxDatabase.delete(testEntity);
		rxDatabase.delete(testEntity2);
		assertEquals(6, changeAllCount.get());
		assertEquals(3, changeSpecificCount.get());

		// Non-existing delete call causes no changes
		rxDatabase.delete(testEntity);
		rxDatabase.delete(testEntity2);
		assertEquals(6, changeAllCount.get());
		assertEquals(3, changeSpecificCount.get());

		allChanges.unsubscribe();
		specificChanges.unsubscribe();

	}

	public void testChangesSubscription() {

		// Add observable to all database changes
		final AtomicInteger changeCount = new AtomicInteger();
		Subscription changes = rxDatabase.changes().subscribe(new Action1<DatabaseChange>() {
			@Override
			public void call(DatabaseChange databaseChange) {
				changeCount.getAndIncrement();
			}
		});

		final TestEntity2 testEntity2 = new TestEntity2();
		testEntity2.date = new Date(System.currentTimeMillis());

		// Simple insert
		rxDatabase.put(testEntity2);
		assertEquals(1, changeCount.get());

		// Simple update
		testEntity2.date = new Date(System.currentTimeMillis());
		rxDatabase.put(testEntity2);
		assertEquals(2, changeCount.get());

		// Simple delete
		rxDatabase.delete(testEntity2);
		assertEquals(3, changeCount.get());

		// Unsubscribe from changes and repeat operations
		changes.unsubscribe();

		final TestEntity2 pausedEntity2 = new TestEntity2();
		pausedEntity2.date = new Date(System.currentTimeMillis());

		// Simple insert
		rxDatabase.put(pausedEntity2);
		assertEquals(3, changeCount.get());

		// Simple update
		pausedEntity2.date = new Date(System.currentTimeMillis());
		rxDatabase.put(pausedEntity2);
		assertEquals(3, changeCount.get());

		// Simple delete
		rxDatabase.delete(pausedEntity2);
		assertEquals(3, changeCount.get());

	}

	public void testChangedContent() {

		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = System.currentTimeMillis();

		// Count the different types of changes
		final AtomicInteger changesCount = new AtomicInteger();
		final AtomicInteger insertCount = new AtomicInteger();
		final AtomicInteger updateCount = new AtomicInteger();
		final AtomicInteger deleteCount = new AtomicInteger();
		CompositeSubscription allChanges = new CompositeSubscription();

		allChanges.add(rxDatabase.changes().subscribe(new Action1<DatabaseChange>() {
			@Override
			public void call(DatabaseChange databaseChange) {
				changesCount.getAndIncrement();
				assertTrue(databaseChange.entity() instanceof TestEntity);
				TestEntity changed = (TestEntity) databaseChange.entity();
				assertNotNull(changed._id);
				assertEquals(testEntity, changed);
				assertEquals(testEntity.string, changed.string);
				assertEquals(testEntity.time, changed.time);
			}
		}));
		allChanges.add(rxDatabase.changes().filter(new Func1<DatabaseChange, Boolean>() {
			@Override
			public Boolean call(DatabaseChange databaseChange) {
				return databaseChange instanceof DatabaseChange.DatabaseInsert;
			}
		}).subscribe(new Action1<DatabaseChange>() {
			@Override
			public void call(DatabaseChange databaseChange) {
				insertCount.getAndIncrement();
			}
		}));
		allChanges.add(rxDatabase.changes().filter(new Func1<DatabaseChange, Boolean>() {
			@Override
			public Boolean call(DatabaseChange databaseChange) {
				return databaseChange instanceof DatabaseChange.DatabaseUpdate;
			}
		}).subscribe(new Action1<DatabaseChange>() {
			@Override
			public void call(DatabaseChange databaseChange) {
				updateCount.getAndIncrement();
			}
		}));
		allChanges.add(rxDatabase.changes().filter(new Func1<DatabaseChange, Boolean>() {
			@Override
			public Boolean call(DatabaseChange databaseChange) {
				return databaseChange instanceof DatabaseChange.DatabaseDelete;
			}
		}).subscribe(new Action1<DatabaseChange>() {
			@Override
			public void call(DatabaseChange databaseChange) {
				deleteCount.getAndIncrement();
			}
		}));

		// Simple insert/update/delete
		rxDatabase.put(testEntity);
		testEntity.string = "Updated";
		testEntity.time = System.currentTimeMillis();
		rxDatabase.put(testEntity);
		rxDatabase.delete(testEntity);

		// Action insert/update/delete
		testEntity._id = null;
		testEntity.string = "Renew";
		testEntity.time = System.currentTimeMillis();
		Observable.just(testEntity).doOnNext(rxDatabase.put()).doOnNext(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				testEntity.string = "Renew updated";
			}
		}).doOnNext(rxDatabase.put()).doOnNext(rxDatabase.delete()).subscribe();

		assertEquals(6, changesCount.intValue());
		assertEquals(2, insertCount.intValue());
		assertEquals(2, updateCount.intValue());
		assertEquals(2, deleteCount.intValue());

		allChanges.clear();

	}

	public void testOnDatabaseChangeAction() {

		// Observe all database changes using the OnDatabaseChange default action
		final AtomicInteger insertCount = new AtomicInteger();
		final AtomicInteger updateCount = new AtomicInteger();
		final AtomicInteger deleteCount = new AtomicInteger();
		Subscription changes = rxDatabase.changes(TestEntity.class).subscribe(new OnDatabaseChange<TestEntity>() {
			@Override
			public void onInsert(TestEntity entity) {
				insertCount.getAndIncrement();
			}

			@Override
			public void onUpdate(TestEntity entity) {
				updateCount.getAndIncrement();
			}

			@Override
			public void onDelete(TestEntity entity) {
				deleteCount.getAndIncrement();
			}
		});

		long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = time;

		// Simple insert
		rxDatabase.put(testEntity);
		assertEquals(1, insertCount.get());
		assertEquals(0, updateCount.get());
		assertEquals(0, deleteCount.get());

		// Simple update
		long updatedTime = System.currentTimeMillis();
		testEntity.string = "Updated";
		testEntity.time = updatedTime;
		rxDatabase.put(testEntity);
		assertEquals(1, insertCount.get());
		assertEquals(1, updateCount.get());
		assertEquals(0, deleteCount.get());

		// Simple delete
		rxDatabase.delete(testEntity);
		assertEquals(1, insertCount.get());
		assertEquals(1, updateCount.get());
		assertEquals(1, deleteCount.get());

		// Non-existing delete call causes no changes
		rxDatabase.delete(testEntity);
		assertEquals(1, insertCount.get());
		assertEquals(1, updateCount.get());
		assertEquals(1, deleteCount.get());

		changes.unsubscribe();

	}*/

	@After
	public void tearDown() throws Exception {
		db.close();
	}

}
