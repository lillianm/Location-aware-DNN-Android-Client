package edu.sv.cmu.datacollectiononline.handler;

import edu.sv.cmu.datacollectiononline.MainActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SwitchListener implements OnCheckedChangeListener{

	private MainActivity ctx;
	public SwitchListener(MainActivity ctx){
		this.ctx = ctx;
	}
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(isChecked){
			SharedPreferences prefs = ctx.getSharedPreferences(MainActivity.PREFS_NAME, MainActivity.MODE_PRIVATE);
			prefs.edit().putString(MainActivity.PREFS_MODE,MainActivity.DATACOLLECTIONMODE).commit();
			
			/* kill all background running threads before restart main activity*/
			if(ctx.t_write!=null)
				ctx.t_write.kill();
			if(ctx.t_poll!=null)
				ctx.t_poll.kill();
			if(ctx.t_audio!=null)
				ctx.t_audio.kill();
			if(ctx.t_context!=null)
				ctx.t_context.kill();
			Intent intent = new Intent(ctx, MainActivity.class);
			intent.putExtra(MainActivity.PREFS_MODE, MainActivity.CLIENTMODE);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

			ctx.finish();
			ctx.startActivity(intent);
		}
		else{
			
			SharedPreferences prefs = ctx.getSharedPreferences(MainActivity.PREFS_NAME, MainActivity.MODE_PRIVATE);
			prefs.edit().putString(MainActivity.PREFS_MODE,MainActivity.CLIENTMODE).commit();
			
			/* kill all background running threads before restart main activity*/
			if(ctx.t_write!=null)
				ctx.t_write.kill();
			if(ctx.t_poll!=null)
				ctx.t_poll.kill();
			if(ctx.t_audio!=null)
				ctx.t_audio.kill();
			if(ctx.t_context!=null)
				ctx.t_context.kill();

			
			Intent intent = new Intent(ctx, MainActivity.class);
			intent.putExtra(MainActivity.PREFS_MODE, MainActivity.DATACOLLECTIONMODE);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			ctx.finish();
			ctx.startActivity(intent);

		}

	}



}
