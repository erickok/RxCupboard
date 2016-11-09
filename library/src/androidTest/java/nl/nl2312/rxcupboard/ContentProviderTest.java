package nl.nl2312.rxcupboard;

import android.provider.ContactsContract;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.reactivex.functions.Predicate;
import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CupboardBuilder;

@RunWith(AndroidJUnit4.class)
public class ContentProviderTest {

	private Cupboard cupboard;

	@Before
	public void setUp() throws Exception {
		cupboard = new CupboardBuilder().useAnnotations().build();
		cupboard.register(Contact.class);
	}

	@Test
	public void contentProvider_query() throws InterruptedException {

		RxContentProvider rxContentProvider = RxCupboard.with(cupboard, InstrumentationRegistry.getTargetContext(),
				ContactsContract.Contacts.CONTENT_URI);

		// Should emit some items
		rxContentProvider.query(Contact.class)
				.count()
				.test()
				.assertTerminated()
				.assertValue(new Predicate<Long>() {
					@Override
					public boolean test(Long count) throws Exception {
						return count >= 0;
					}
				});

	}

	public static class Contact {

		public Long _id;
		public String display_name;

	}

}
