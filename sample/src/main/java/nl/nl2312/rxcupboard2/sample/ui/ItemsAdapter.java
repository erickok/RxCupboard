package nl.nl2312.rxcupboard2.sample.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import nl.nl2312.rxcupboard2.sample.model.Item;

public class ItemsAdapter extends BaseAdapter {

	private final LayoutInflater layoutInflator;
	private final List<Item> items;

	public ItemsAdapter(Context context) {
		this.layoutInflator = LayoutInflater.from(context);
		this.items = new ArrayList<>();
	}

	public void add(Item item) {
		this.items.add(item);
		notifyDataSetChanged();
	}

	public void add(List<Item> items) {
		this.items.addAll(items);
		notifyDataSetChanged();
	}

	public void remove(Item item) {
		items.remove(item);
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Item getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return items.get(position)._id;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = layoutInflator.inflate(android.R.layout.simple_list_item_1, parent, false);
		}
		((TextView) convertView).setText(getItem(position).title);
		return convertView;
	}

}
