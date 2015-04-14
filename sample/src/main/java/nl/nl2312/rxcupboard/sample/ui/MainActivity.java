package nl.nl2312.rxcupboard.sample.ui;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import nl.nl2312.rxcupboard.DatabaseChange;
import nl.nl2312.rxcupboard.RxCupboard;
import nl.nl2312.rxcupboard.RxDatabase;
import nl.nl2312.rxcupboard.sample.CupboardDbHelper;
import nl.nl2312.rxcupboard.sample.R;
import nl.nl2312.rxcupboard.sample.model.Item;
import rx.Observable;
import rx.android.app.RxActivity;
import rx.android.lifecycle.LifecycleObservable;
import rx.android.view.OnClickEvent;
import rx.android.view.ViewActions;
import rx.android.view.ViewObservable;
import rx.android.widget.OnItemClickEvent;
import rx.android.widget.OnTextChangeEvent;
import rx.android.widget.WidgetObservable;
import rx.functions.Action1;
import rx.functions.Func1;

public class MainActivity extends RxActivity {

	private ListView itemsList;
	private EditText addEdit;
	private Button addButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

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
		rxBind(rxCupboard.query(Item.class).toList()).subscribe(new Action1<List<Item>>() {
			@Override
			public void call(List<Item> items) {
				adapter.add(items);
				itemsList.setAdapter(adapter);
			}
		}, toastErrorAction);

		// Add/remove items to/from the list view on any changes in the Item database table
		rxBind(rxCupboard.changes(Item.class)).subscribe(new Action1<DatabaseChange<Item>>() {
			@Override
			public void call(DatabaseChange<Item> databaseChange) {
				if (databaseChange instanceof DatabaseChange.DatabaseInsert) {
					adapter.add(databaseChange.entity());
				} else if (databaseChange instanceof DatabaseChange.DatabaseDelete) {
					adapter.remove(databaseChange.entity());
				}
			}
		}, toastErrorAction);

		// Remove an item from the database when it was clicked
		rxBind(WidgetObservable.itemClicks(itemsList).map(new Func1<OnItemClickEvent, Object>() {
			@Override
			public Object call(OnItemClickEvent onItemClickEvent) {
				// Return the object that was clicked
				return adapter.getItem(onItemClickEvent.position());
			}
		})).subscribe(rxCupboard.delete());

		// Enable the Add button only when text was entered
		rxBind(WidgetObservable.text(addEdit, true).map(new Func1<OnTextChangeEvent, Boolean>() {
			@Override
			public Boolean call(OnTextChangeEvent onTextChangeEvent) {
				// Emit whether there is now any text input
				return !TextUtils.isEmpty(onTextChangeEvent.view().getText());
			}
		}).distinctUntilChanged()).subscribe(ViewActions.setEnabled(addButton));

		// Allow adding of items when pressing the Add button
		rxBind(ViewObservable.clicks(addButton).map(new Func1<OnClickEvent, String>() {
			@Override
			public String call(OnClickEvent onClickEvent) {
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
		}).subscribe(rxCupboard.put(), toastErrorAction);

	}

	private Action1<Throwable> toastErrorAction = new Action1<Throwable>() {
		@Override
		public void call(Throwable throwable) {
			Toast.makeText(MainActivity.this, throwable.toString(), Toast.LENGTH_SHORT).show();
		}
	};

	private <T> Observable<T> rxBind(Observable<T> source) {
		return LifecycleObservable.bindActivityLifecycle(lifecycle(), source);
	}

}
