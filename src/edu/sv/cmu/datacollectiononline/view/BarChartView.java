package edu.sv.cmu.datacollectiononline.view;
import edu.sv.cmu.datacollectiononline.util.Energy;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;



public class BarChartView extends View {
	Paint textPaint;
	Paint linePaint;
	Paint textPaint1;
	Paint textPaint2;
	float scaleFactor = 1;
	int size = 5;
	String[] tags = {"BG_avg", "SP_avg","BG max_avg","SP max_avg","Running"};
	int[] colors = {0xFFFFBB33, 0xFF218b21, 0xFFC5C5C5, 0xFF33b5e5,0xFFFF2400};
	/* boxPaints is 
	 * BackGround Speech
	 * avg, max_avg, max, Running */
	Paint[] boxPaints;
	Paint[] textPaints;
	Paint[] params;
	Energy bg_energy = new Energy();
	Energy speech_energy = new Energy();
	float runningAvg = 0;
	float[] energy_params = new float[5];
	public BarChartView(Context context) {
		super(context);
		initialise();
	}
	public BarChartView(Context context, int size){
		super(context);
		this.size = size;
		initialise();
	}
	public BarChartView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialise();
	}
	public BarChartView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialise();
	}
	void initialise() {
		boxPaints = new Paint[size];
		textPaints = new Paint[size];
		params = new Paint[size];
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		scaleFactor = metrics.density;
		for(int i=0;i<size;i++){
			boxPaints[i] = new Paint();
			textPaints[i] = new Paint();
			params[i] = new Paint();
			params[i].setColor(colors[i]);
			params[i].setTextSize(14*scaleFactor);
			textPaints[i].setColor(colors[i]);
			textPaints[i].setTextSize(14*scaleFactor);

		}
		boxPaints[0].setColor(0xFFFFBB33);
		boxPaints[1].setColor(0xFF218b21);
		boxPaints[2].setColor(0xFFC5C5C5);
		boxPaints[3].setColor(0xFF33b5e5);
		boxPaints[4].setColor(0xFFFF2400);

		textPaint = new Paint();
		linePaint = new Paint();
		textPaint1 = new Paint();
		textPaint2 = new Paint();
		linePaint.setStrokeWidth(1);
		linePaint.setColor(0xFFC5C5C5);
		textPaint.setColor(0xFFC5C5C5);
		textPaint.setTextSize(14*scaleFactor);
		
		textPaint1.setColor(0xFFFFBB33);
		textPaint2.setColor(0xFF218b21);
		textPaint1.setTextSize(14*scaleFactor);
		textPaint2.setTextSize(14*scaleFactor);
		
	}

	
	public void updateEnergy(Energy e, String mode){
		if(mode.equals("bg"))
			bg_energy = e;
		if(mode.equals("speech"))
			speech_energy = e;
		invalidate();
	}
	public void updateRunningAvg(int rAvg){
		runningAvg = rAvg;
		invalidate();
	}
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int fullWidth = getWidth();
		int fullHeight = (int) (getHeight());
		int padding = (int) (5*scaleFactor);
		int maxBarHeight = fullHeight-8*padding;
		float[] barHeights = new float[size];
		if(bg_energy!=null && speech_energy!=null){
			
			float max = Math.max(bg_energy.max_avg_energy, speech_energy.max_avg_energy);
			
			energy_params[0] = bg_energy.average_energy;
			energy_params[1] = speech_energy.average_energy;
			energy_params[2] = bg_energy.max_avg_energy;
			energy_params[3] = speech_energy.max_avg_energy;
			energy_params[4] = runningAvg;
			
			barHeights[0] = (float) bg_energy.average_energy/max * maxBarHeight;
			barHeights[1] = (float) speech_energy.average_energy/max * maxBarHeight;
			barHeights[2] = (float) bg_energy.max_avg_energy > speech_energy.max_avg_energy? maxBarHeight:bg_energy.max_avg_energy/max * maxBarHeight;

			//canvas.drawLine(padding, fullHeight-25*scaleFactor, fullWidth-padding , fullHeight-25*scaleFactor, linePaint);
			barHeights[3] = (float) bg_energy.max_avg_energy < speech_energy.max_avg_energy? maxBarHeight:speech_energy.max_avg_energy/max * maxBarHeight;
			barHeights[4] = (float) runningAvg/max * maxBarHeight;
			float quarter = (float) ((fullWidth-10*padding)*0.22);
			
			for(int i=0;i<size;i++){
				int barbottom = fullHeight - padding;
				float bartop = barbottom-barHeights[i];
				float val1pos = bartop-padding;
				if(i%2 == 0){
					canvas.drawRect(2*padding+i*quarter, bartop, (i+1)*quarter , barbottom, boxPaints[i]);
					canvas.drawText(tags[i], i*quarter, val1pos-3*padding,textPaints[i]);
					canvas.drawText(""+energy_params[i], 2*padding+i*quarter, val1pos, textPaints[i]);

				}
				if(i%2 == 1){
					canvas.drawRect(i*quarter, bartop, (i+1)*quarter-padding , barbottom, boxPaints[i]);
					canvas.drawText(tags[i], i*quarter, val1pos-3*padding, textPaints[i]);
					canvas.drawText(""+energy_params[i], i*quarter, val1pos, textPaints[i]);

				}
			}
		}
	}

}
