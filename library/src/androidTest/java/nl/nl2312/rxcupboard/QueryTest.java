package nl.nl2312.rxcupboard;

import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.StrictMode;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CupboardBuilder;

@RunWith(AndroidJUnit4.class)
public class QueryTest {

	private static final String TEST_DATABASE = "RxCupboardTest.db";

	private Cupboard cupboard;
	private SQLiteDatabase db;
	private RxDatabase rxDatabase;
	private BiFunction<Integer, TestEntity, Integer> accumulator;

	@Before
	public void setUp() throws Exception {
		cupboard = new CupboardBuilder().build();
		cupboard.register(TestEntity.class);
		InstrumentationRegistry.getTargetContext().deleteDatabase(TEST_DATABASE);
		db = new TestDbHelper(InstrumentationRegistry.getTargetContext(), cupboard, TEST_DATABASE).getWritableDatabase();
		rxDatabase = RxCupboard.with(cupboard, db);

		// Enable strict mode to test if the underlying database cursors are correctly closed
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().penaltyLog().penaltyDeath().build());
		}

		// Insert 10 rows in the TestEntity table
		Flowable.range(1, 10)
				.map(new Function<Integer, TestEntity>() {
					@Override
					public TestEntity apply(Integer integer) throws Exception {
						TestEntity testEntity = new TestEntity();
						testEntity._id = integer.longValue();
						testEntity.string = "Test";
						testEntity.time = integer.longValue();
						return testEntity;
					}
				})
				.test();

		accumulator = new BiFunction<Integer, TestEntity, Integer>() {
			@Override
			public Integer apply(Integer count, TestEntity testEntity) throws Exception {
				return count + 1;
			}
		};
	}

	@Test
	public void testQueryAll() {

		// Emit all 10 items from the TestEntity table
		rxDatabase.query(TestEntity.class)
				.test()
				.assertValueCount(10)
				.assertValue(new Predicate<TestEntity>() {
					@Override
					public boolean test(TestEntity testEntity) throws Exception {
						return testEntity._id != null &&
								testEntity.string.equals("Test") &&
								testEntity.time == testEntity._id;
					}
				});

	}

	@Test
	public void testQuerySelection() {

		// Emit the 5 items from the TestEntity table with id < 5
		rxDatabase.query(TestEntity.class, "_id < ?", Integer.toString(5))
				.test()
				.assertValueCount(5)
				.assertValue(new Predicate<TestEntity>() {
					@Override
					public boolean test(TestEntity testEntity) throws Exception {
						return testEntity._id != null &&
								testEntity._id < 5 &&
								testEntity.string.equals("Test") &&
								testEntity.time == testEntity._id;
					}
				});

	}

	@Test
	public void testQueryBuilder() {

		// Emit the 5 items from the TestEntity table with id < 5 using the Cupboard QueryBuilder
		rxDatabase.query(rxDatabase.buildQuery(TestEntity.class).withSelection("_id < ?", Integer.toString(5)))
				.test()
				.assertValueCount(5)
				.assertValue(new Predicate<TestEntity>() {
					@Override
					public boolean test(TestEntity testEntity) throws Exception {
						return testEntity._id != null &&
								testEntity._id < 5 &&
								testEntity.string.equals("Test") &&
								testEntity.time == testEntity._id;
					}
				});

	}

	@Test
	public void testReactivePull() {

		// Normally there are 10 items
		rxDatabase.query(TestEntity.class)
				.scan(0, accumulator)
				.test()
				.assertValueCount(10);

		// Ask for only 5 elements of the 10 in the table
		rxDatabase.query(TestEntity.class)
				.scan(0, accumulator)
				.take(5)
				.test()
				.assertValues(1, 2, 3, 4, 5);

	}

	@After
	public void tearDown() throws Exception {
		db.close();
	}

}
