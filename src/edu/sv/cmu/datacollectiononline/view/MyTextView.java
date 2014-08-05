package edu.sv.cmu.datacollectiononline.view;

import android.content.Context;
import android.view.Gravity;
import android.widget.TextView;

public class MyTextView extends TextView{

	public MyTextView(Context context,int textSize){
		super(context);
		this.setTextSize(textSize);
		this.setGravity(Gravity.CENTER_HORIZONTAL);

	}


	public MyTextView(Context context,int textSize, int textColor){
		super(context);
		this.setTextSize(textSize);
		this.setTextColor(textColor);
		this.setGravity(Gravity.CENTER_HORIZONTAL);
	}
	public MyTextView(Context context,int textSize, int textColor, int id){
		super(context);
		this.setTextSize(textSize);
		this.setTextColor(textColor);
		this.setId(id);
		this.setGravity(Gravity.CENTER_HORIZONTAL);

	}


	public MyTextView(Context context,int textSize, int textColor, String text){
		super(context);
		this.setTextSize(textSize);
		this.setTextColor(textColor);
		this.setText(text);
		this.setGravity(Gravity.CENTER_HORIZONTAL);


	}

	public void init(){
		/* Center horizontal */
		this.setGravity(0x01);
	}	

}
