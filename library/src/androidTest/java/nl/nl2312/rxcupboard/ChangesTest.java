package nl.nl2312.rxcupboard;

import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.subscriptions.CompositeSubscription;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

public class ChangesTest extends InstrumentationTestCase {

	private static final String TEST_DATABASE = "RxCupboardTest.db";

	private TestDbHelper helper;
	private SQLiteDatabase db;
	private RxCupboard rxCupboard;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		cupboard().register(TestEntity.class);
		cupboard().register(TestEntity2.class);
		getInstrumentation().getContext().deleteDatabase(TEST_DATABASE);
		helper = new TestDbHelper(getInstrumentation().getContext(), TEST_DATABASE);
		db = helper.getWritableDatabase();
		rxCupboard = RxCupboard.with(db);
	}

	public void testAllAndSpecificChanges() {

		// Add observable to all database changes and one for only changes in TestEntity2
		final AtomicInteger changeAllCount = new AtomicInteger();
		final AtomicInteger changeSpecificCount = new AtomicInteger();
		Subscription allChanges = rxCupboard.changes().subscribe(new Action1<DatabaseChange>() {
			@Override
			public void call(DatabaseChange databaseChange) {
				changeAllCount.getAndIncrement();
			}
		});
		Subscription specificChanges = rxCupboard.changes(TestEntity2.class).subscribe(new Action1<DatabaseChange>() {
			@Override
			public void call(DatabaseChange databaseChange) {
				assertTrue(databaseChange.entity() instanceof TestEntity2);
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
		rxCupboard.put(testEntity);
		rxCupboard.put(testEntity2);
		assertEquals(2, changeAllCount.get());
		assertEquals(1, changeSpecificCount.get());

		// Simple update
		long updatedTime = System.currentTimeMillis();
		testEntity.string = "Updated";
		testEntity.time = updatedTime;
		testEntity2.date = new Date(updatedTime);
		rxCupboard.put(testEntity);
		rxCupboard.put(testEntity2);
		assertEquals(4, changeAllCount.get());
		assertEquals(2, changeSpecificCount.get());

		// Simple delete
		rxCupboard.delete(testEntity);
		rxCupboard.delete(testEntity2);
		assertEquals(6, changeAllCount.get());
		assertEquals(3, changeSpecificCount.get());

		// Non-existing delete call causes no changes
		rxCupboard.delete(testEntity);
		rxCupboard.delete(testEntity2);
		assertEquals(6, changeAllCount.get());
		assertEquals(3, changeSpecificCount.get());

		allChanges.unsubscribe();
		specificChanges.unsubscribe();

	}

	public void testChangesSubscription() {

		// Add observable to all database changes
		final AtomicInteger changeCount = new AtomicInteger();
		Subscription changes = rxCupboard.changes().subscribe(new Action1<DatabaseChange>() {
			@Override
			public void call(DatabaseChange databaseChange) {
				changeCount.getAndIncrement();
			}
		});

		final TestEntity2 testEntity2 = new TestEntity2();
		testEntity2.date = new Date(System.currentTimeMillis());

		// Simple insert
		rxCupboard.put(testEntity2);
		assertEquals(1, changeCount.get());

		// Simple update
		testEntity2.date = new Date(System.currentTimeMillis());
		rxCupboard.put(testEntity2);
		assertEquals(2, changeCount.get());

		// Simple delete
		rxCupboard.delete(testEntity2);
		assertEquals(3, changeCount.get());

		// Unsubscribe from changes and repeat operations
		changes.unsubscribe();

		final TestEntity2 pausedEntity2 = new TestEntity2();
		pausedEntity2.date = new Date(System.currentTimeMillis());

		// Simple insert
		rxCupboard.put(pausedEntity2);
		assertEquals(3, changeCount.get());

		// Simple update
		pausedEntity2.date = new Date(System.currentTimeMillis());
		rxCupboard.put(pausedEntity2);
		assertEquals(3, changeCount.get());

		// Simple delete
		rxCupboard.delete(pausedEntity2);
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

		allChanges.add(rxCupboard.changes().subscribe(new Action1<DatabaseChange>() {
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
		allChanges.add(rxCupboard.changes().filter(new Func1<DatabaseChange, Boolean>() {
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
		allChanges.add(rxCupboard.changes().filter(new Func1<DatabaseChange, Boolean>() {
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
		allChanges.add(rxCupboard.changes().filter(new Func1<DatabaseChange, Boolean>() {
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
		rxCupboard.put(testEntity);
		testEntity.string = "Updated";
		testEntity.time = System.currentTimeMillis();
		rxCupboard.put(testEntity);
		rxCupboard.delete(testEntity);

		// Action insert/update/delete
		testEntity._id = null;
		testEntity.string = "Renew";
		testEntity.time = System.currentTimeMillis();
		Observable.just(testEntity).doOnNext(rxCupboard.put()).doOnNext(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				testEntity.string = "Renew updated";
			}
		}).doOnNext(rxCupboard.put()).doOnNext(rxCupboard.delete()).subscribe();

		assertEquals(6, changesCount.intValue());
		assertEquals(2, insertCount.intValue());
		assertEquals(2, updateCount.intValue());
		assertEquals(2, deleteCount.intValue());

		allChanges.clear();

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		db.close();
	}

}
