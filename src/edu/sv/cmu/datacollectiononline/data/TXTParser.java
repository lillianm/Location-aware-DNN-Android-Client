package edu.sv.cmu.datacollectiononline.data;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;

import edu.sv.cmu.datacollectiononline.MainActivity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

public class TXTParser {
	public static String filename = "file:///android_asset/06-10-script.txt";
	public static String TAG = "TXT Parser";
	private static ArrayList<String> promptData = new ArrayList<String>();
	private static MainActivity ctx;
	public TXTParser(MainActivity ctx){
		this.ctx = ctx;
		
	}
	public void populatePromptData(){

		if(promptData == null){
			promptData = new ArrayList<String>();
		}
		BufferedReader br;
		int id = 0;
		try { 
			AssetManager am = ctx.getAssets();
			InputStreamReader in = new InputStreamReader(am.open("06-10-script.txt"));
			//fr = new FileReader(filename);
			br = new BufferedReader(in);
			String thisline = null;
			while((thisline = br.readLine())!=null){
				if(thisline!=""){
					promptData.add(id+":"+thisline.toLowerCase());
					id++;
				}
			}
			br.close();
			//fr.close();
			System.out.println(id);
		} catch (FileNotFoundException e) {
			Log.e("TXTParser", "file not found");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	public String matchWordIndex(String prompt, String response, boolean[] moveToNext){
		//String text = "<font color=#cc0029>Erste Farbe</font> <font color=#ffcc00>zweite Farbe</font>";
		ArrayList<Integer> index = new ArrayList<Integer>();

		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		
		String[] words = (prompt.toLowerCase()).split(" ");
		response = response.toLowerCase().replaceAll("[\\.?!]$", "");
		String[] reco_words = response.split(" ");
		for(int i=0;i<reco_words.length;i++){
			map.put(reco_words[i].hashCode(), i);
		}

		StringBuilder builder = new StringBuilder();
		StringBuilder builder2 = new StringBuilder();
		for(String w:words){
			if (map.keySet().contains(w.hashCode())){
				index.add(map.get(w.hashCode()));
				builder.append("<font color=#4cc417>").append(w).append("</font>").append(" ");
			}
			else{
				builder.append("<font color=#ff2400>").append(w).append("</font>").append(" ");
			}
		}
		builder.append(" ");
		for(int i=0;i<reco_words.length;i++){
			if(index.contains(i)){
				builder2.append("<font color=#4cc417>").append(reco_words[i]).append("</font>").append(" ");
			}
			else{
				builder2.append("<font color=#2554c7>").append(reco_words[i]).append("</font>").append(" ");
			}
		}
		builder2.append(" ");
		if(index.size()/words.length < 1 ) 
			moveToNext[0] = false;
		else 
			moveToNext[0] = true;
		return builder.toString() + "<br>"+builder2.toString();//"<font color=#2554c7>" + response + "</font>";
	}

	public String getRandomePrompt(){
		int i = (int) (Math.random() * (promptData.size()));
		Log.e(TAG,i+"");
		return promptData.get(i); // this one includes the id;
	}




}
