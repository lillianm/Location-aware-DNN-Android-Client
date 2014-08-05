package edu.sv.cmu.datacollectiononline.view;

import edu.sv.cmu.datacollectiononline.R;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StatusView extends LinearLayout{
	public ImageView imgView;
	public TextView txtView;

	private Context ctx;




	public StatusView(Context context) {
		super(context);

		setOrientation(LinearLayout.HORIZONTAL);
		setGravity(Gravity.CENTER_VERTICAL);
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.status_view, this, true);

		txtView = (TextView) getChildAt(1);
		imgView = (ImageView) getChildAt(0);
	}


	public void init(){

		setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		setOrientation(LinearLayout.HORIZONTAL);

		imgView = new ImageView(ctx);
		imgView.setLayoutParams(new LayoutParams(50,50));
		txtView = new TextView(ctx);
		txtView.setTextSize(20);
		txtView.setText("haha");
		txtView.setTextColor(Color.BLACK);

	}
	public void setImage(int id){
		imgView.setImageResource(id);
	}
	public void setText(String msg){
		txtView.setText(msg);
	}
}


