package nl.nl2312.rxcupboard2.sample.ui;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxAdapterView;
import com.jakewharton.rxbinding.widget.RxTextView;

import java.util.List;

import nl.nl2312.rxcupboard2.OnDatabaseChange;
import nl.nl2312.rxcupboard2.RxCupboard;
import nl.nl2312.rxcupboard2.RxDatabase;
import nl.nl2312.rxcupboard2.sample.CupboardDbHelper;
import nl.nl2312.rxcupboard2.sample.R;
import nl.nl2312.rxcupboard2.sample.model.Item;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends Activity {

	private CompositeSubscription subscriptions;
	private ListView itemsList;
	private EditText addEdit;
	private Button addButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		subscriptions = new CompositeSubscription();
		itemsList = (ListView) findViewById(R.id.itemsList);
		addEdit = (EditText) findViewById(R.id.addEdit);
		addButton = (Button) findViewById(R.id.addButton);
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Get handle to your Cupboard database, using the default cupboard() instance
		final SQLiteDatabase db = CupboardDbHelper.getConnection(this);
		final RxDatabase rxCupboard = RxCupboard.withDefault(db);

		// Load all existing items form the database into the list view
		final ItemsAdapter adapter = new ItemsAdapter(this);
		// Note that the underlying Cursor is created on calling query(), but individual Item objects are created when iterating (reactive pull)
		subscriptions.add(onUi(rxCupboard.query(Item.class).toList()).subscribe(new Action1<List<Item>>() {
			@Override
			public void call(List<Item> items) {
				adapter.add(items);
				itemsList.setAdapter(adapter);
			}
		}, toastErrorAction));

		// Add/remove items to/from the list view on any changes in the Item database table
		subscriptions.add(onUi(rxCupboard.changes(Item.class)).subscribe(new OnDatabaseChange<Item>() {
			@Override
			public void onInsert(Item entity) {
				adapter.add(entity);
			}

			@Override
			public void onDelete(Item entity) {
				adapter.remove(entity);
			}
		}, toastErrorAction));

		// Remove an item from the database when it was clicked
		subscriptions.add(onUi(RxAdapterView.itemClicks(itemsList).map(new Func1<Integer, Object>() {
			@Override
			public Object call(Integer position) {
				// Return the object that was clicked
				return adapter.getItem(position);
			}
		})).subscribe(rxCupboard.delete()));

		// Enable the Add button only when text was entered
		subscriptions.add(onUi(RxTextView.textChanges(addEdit).map(new Func1<CharSequence, Boolean>() {
			@Override
			public Boolean call(CharSequence text) {
				// Emit whether there is now any text input
				return !TextUtils.isEmpty(text);
			}
		}).distinctUntilChanged()).subscribe(RxView.enabled(addButton)));

		// Allow adding of items when pressing the Add button
		subscriptions.add(onUi(RxView.clicks(addButton).map(new Func1<Void, String>() {
			@Override
			public String call(Void click) {
				// Get the text to use for the new Item title
				return addEdit.getText().toString();
			}
		}).map(new Func1<String, Item>() {
			@Override
			public Item call(String s) {
				// Create the pojo
				Item item = new Item();
				item.title = s;
				return item;
			}
		})).doOnNext(new Action1<Item>() {
			@Override
			public void call(Item item) {
				// Clear input text
				addEdit.setText(null);
			}
		}).subscribe(rxCupboard.put(), toastErrorAction));
	}

	private Action1<Throwable> toastErrorAction = new Action1<Throwable>() {
		@Override
		public void call(Throwable throwable) {
			Toast.makeText(MainActivity.this, throwable.toString(), Toast.LENGTH_SHORT).show();
		}
	};

	private <T> Observable<T> onUi(Observable<T> source) {
		return source.observeOn(AndroidSchedulers.mainThread());
	}

	@Override
	protected void onStop() {
		super.onStop();
		subscriptions.clear();
	}
}
