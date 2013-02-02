package jp.dip.ysato.hibikiplayer;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class ProgramAdapter extends BaseAdapter {
	public static final String START = "START";
	private LayoutInflater layout;
	private Context context;
	private List<ProgramBean> program;
	public ProgramAdapter(Context context) {
		super();
		// TODO Auto-generated constructor stub
		layout = LayoutInflater.from(context);
		this.program = new ArrayList<ProgramBean>();
		this.context = context;
	}
	class PlayButtonListener implements OnClickListener {
		private Context context;
		private ProgramBean program;
		public PlayButtonListener(Context c, ProgramBean p) {
			context = c;
			program = p;
		}
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Bundle bundle = new Bundle();
			bundle.putString("program", program.title());
			bundle.putInt("no", program.no());
			bundle.putParcelable("image", program.image());
			bundle.putString("url", program.playlisturl(0));
			bundle.putString("comment", program.comment());
			Intent intent = new Intent(context, PlayerActivity.class);
			intent.putExtra("program", bundle);
			intent.setAction(START);
			context.startActivity(intent);
		}
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = layout.inflate(R.layout.programview, null);
		}
		ProgramBean p = (ProgramBean) getItem(position);
		if (p != null) {
			TextView title = (TextView) convertView.findViewById(R.id.title);
			title.setText(p.title());
			ImageView image = (ImageView) convertView.findViewById(R.id.programImage);
			image.setImageBitmap(p.image());
			TextView comment = (TextView) convertView.findViewById(R.id.comment);
			comment.setText(p.comment());
			ImageButton pbutton = (ImageButton) convertView.findViewById(R.id.playButton);
			pbutton.setOnClickListener(new PlayButtonListener(context, p));
		}
		return convertView;
	}
	public void setList(ArrayList<ProgramBean> programs) {
		// TODO Auto-generated method stub
		program.addAll(programs);
	}
	@Override
	public int getCount() {
		return program.size();
	}
	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return program.get(arg0);
	}
	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}
}
