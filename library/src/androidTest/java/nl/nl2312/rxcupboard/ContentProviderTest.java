package nl.nl2312.rxcupboard;

import android.provider.ContactsContract;
import android.test.InstrumentationTestCase;
import android.util.Log;

import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CupboardBuilder;
import rx.functions.Action1;

public class ContentProviderTest extends InstrumentationTestCase {

	private Cupboard cupboard;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		cupboard = new CupboardBuilder().useAnnotations().build();
		cupboard.register(Contact.class);

	}

	public void testCursor() {

		RxContentProvider rxContentProvider = RxCupboard.with(cupboard, getInstrumentation().getContext(), ContactsContract.Contacts.CONTENT_URI);

		// Should emit some items
		rxContentProvider.query(Contact.class).doOnNext(new Action1<Contact>() {
			@Override
			public void call(Contact contact) {
				Log.println(Log.ASSERT, "ContentProviderTest", contact.display_name);
			}
		}).count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer count) {
				assertTrue(count > 0);
			}
		});

	}

	public static class Contact {

		public Long _id;
		public String display_name;

	}

}
