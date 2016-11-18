package nl.nl2312.rxcupboard2.sample.ui;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import nl.nl2312.rxcupboard2.OnDatabaseChange;
import nl.nl2312.rxcupboard2.RxCupboard;
import nl.nl2312.rxcupboard2.RxDatabase;
import nl.nl2312.rxcupboard2.sample.CupboardDbHelper;
import nl.nl2312.rxcupboard2.sample.R;
import nl.nl2312.rxcupboard2.sample.model.Item;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

public class MainActivity extends Activity {

	private CompositeDisposable subscriptions;
	private ListView itemsList;
	private EditText addEdit;
	private Button addButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		subscriptions = new CompositeDisposable();
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
		subscriptions.add(
				rxCupboard.query(Item.class)
						.subscribeOn(io())
						.toList()
						.observeOn(mainThread())
						.subscribe(items -> {
							adapter.add(items);
							itemsList.setAdapter(adapter);
						}, toastErrorAction));

		// Add/remove items to/from the list view on any changes in the Item database table
		subscriptions.add(
				rxCupboard.changes(Item.class)
						.subscribeOn(io())
						.observeOn(mainThread())
						.subscribe(new OnDatabaseChange<Item>() {
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
		subscriptions.add(
				listItemClicks.map(adapter::getItem)
						.subscribe(rxCupboard.delete()));

		// Enable the Add button only when text was entered
		subscriptions.add(
				addEditTextChanges.map(text -> !TextUtils.isEmpty(text))
						.distinctUntilChanged()
						.subscribe(enabled -> addButton.setEnabled(enabled)));

		// Allow adding of items when pressing the Add button
		subscriptions.add(
				addButtonClicks.map(click -> addEdit.getText().toString())
						.map(entry -> {
							// Create the pojo
							Item item = new Item();
							item.title = entry;
							return item;
						})
						.doOnNext(update -> addEdit.setText(null))
						.subscribe(rxCupboard.put(), toastErrorAction));
	}

	private Consumer<Throwable> toastErrorAction = throwable -> Toast.makeText(MainActivity.this, throwable.toString(), Toast.LENGTH_SHORT).show();

	private Flowable<Integer> listItemClicks = Flowable.create((FlowableEmitter<Integer> emitter) -> {
		AdapterView.OnItemClickListener onItemClicks = (adapterView, view, i, l) -> emitter.onNext(i);
		emitter.setCancellable(() -> itemsList.setOnItemClickListener(null));
		itemsList.setOnItemClickListener(onItemClicks);
	}, BackpressureStrategy.BUFFER);

	private Flowable<CharSequence> addEditTextChanges = Flowable.create((FlowableEmitter<CharSequence> emitter) -> {
		TextWatcher addEditWatcher = new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				emitter.onNext(charSequence);
			}

			@Override
			public void afterTextChanged(Editable editable) {
			}
		};
		emitter.setCancellable(() -> addEdit.removeTextChangedListener(addEditWatcher));
		addEdit.addTextChangedListener(addEditWatcher);
		// Emit initial value
		emitter.onNext(addEdit.getText());
	}, BackpressureStrategy.BUFFER);

	private Flowable<Object> addButtonClicks = Flowable.create((FlowableEmitter<Object> emitter) -> {
		View.OnClickListener onAddButtonClicks = view -> emitter.onNext(new Object());
		emitter.setCancellable(() -> addButton.setOnClickListener(null));
		addButton.setOnClickListener(onAddButtonClicks);
	}, BackpressureStrategy.BUFFER);

	@Override
	protected void onStop() {
		super.onStop();
		subscriptions.clear();
	}

}
