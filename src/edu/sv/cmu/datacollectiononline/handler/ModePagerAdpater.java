package edu.sv.cmu.datacollectiononline.handler;

import java.util.Locale;








import edu.sv.cmu.datacollectiononline.FragmentParamsWrapper;
import edu.sv.cmu.datacollectiononline.FragmentUserModeWrapper;
import edu.sv.cmu.datacollectiononline.MainActivity;
import edu.sv.cmu.datacollectiononline.R;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.View;

public class ModePagerAdpater extends FragmentPagerAdapter {
	
	private FragmentManager fm;
	public FragmentParamsWrapper pf_wrapper = new FragmentParamsWrapper();
	public FragmentUserModeWrapper usr_wrapper = new FragmentUserModeWrapper();
	public ModePagerAdpater(FragmentManager fm) {
		super(fm);
		this.fm = fm;
		if(pf_wrapper == null){
			fm.beginTransaction().attach(pf_wrapper).commit();
			fm.executePendingTransactions();
		}
		if(usr_wrapper == null){
			fm.beginTransaction().attach(usr_wrapper).commit();
			fm.executePendingTransactions();
		}

	}

	@Override
	public Fragment getItem(int position) {

		switch(position){
		case 0: {
			if(usr_wrapper == null){
				fm.beginTransaction().attach(usr_wrapper).commit();
				fm.executePendingTransactions();
			}

			return usr_wrapper;
		}
		case 1: {
			if(pf_wrapper == null){
				fm.beginTransaction().attach(pf_wrapper).commit();
				fm.executePendingTransactions();
			}
			return pf_wrapper;

		}
		}
		return (null);
	}

	@Override
	public int getCount() {

		return 2;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		Locale l = Locale.getDefault();
		switch (position) {
		case 0:
			return "STATUS/PROMPTS";
		case 1:
			return "PARAMETERS";
		}
		return null;

	}
}
