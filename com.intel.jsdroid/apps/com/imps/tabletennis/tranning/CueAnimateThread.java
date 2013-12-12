package com.imps.tabletennis.tranning;

import android.util.Log;

public class CueAnimateThread extends Thread{
	GameView gameView;
	private boolean flag=true;
	private int sleepSpan=40;
	private boolean term = false;
	public CueAnimateThread(GameView gameView){
		this.gameView=gameView;
	}
	public void setFlag(boolean data){
		this.flag = data;
	}
	
	public void exit(){
		term = true;
	}
	@Override
	public void run(){
		/**
		 * @j2sNative
		 * console.log('CueAnimateThread<<GO!');
		 */{}
		while(!term){
			if(!flag){
				try{
	            	Thread.sleep(sleepSpan);
	            }
	            catch(Exception e){
	            	e.printStackTrace();
	            }
	            continue;
			}
			gameView.cue.setShowingAnimFlag(true);
			while(flag)
			{
				
				if(gameView.cue.changeDisWithBall() <= 0){
					gameView.cue.resetAnimValues();
					break;
				}
				try{
	            	Thread.sleep(sleepSpan);
	            }
	            catch(Exception e){
	            	e.printStackTrace();
	            }
			}
			Log.d("CueAnimateThread","run...");
			float v=Ball.vMax*(gameView.strengthBar.getCurrHeight()/gameView.strengthBar.getHeight());
			float angle=gameView.cue.getAngle();
			gameView.alBalls.get(0).changeVxy(v, angle);
			gameView.cue.setShowingAnimFlag(false);
			gameView.cue.setShowCueFlag(false);
			if(gameView.activity.isSoundOn()){
				gameView.playSound(GameView.SHOOT_SOUND, 0);
			}
			flag = false;
		}
	
	}
}
