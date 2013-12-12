package com.imps.tabletennis.tranning;
 
import java.util.ArrayList;

import android.util.Log;

public class BallGoThread extends Thread 
{
	GameView gameView;

	private boolean flag=true;		
	
	ArrayList<Ball> ballsToDelete=new ArrayList<Ball>();
	private int sleepSpan=/*80;//40;*/10;
	// Workaround: (required to ensure GameViewDrawThread timing function Works)
	private int cntDownNum = 0;
	public BallGoThread(GameView gameView)
	{
		this.gameView=gameView;		
	}
	public void run()
	{		/**
		 * @j2sNative
		 * console.log('BallGoThread<<GO!');
		 */{}
		while(flag)
		{
			//Log.d("BallGoThread","run...");
			ballsToDelete.clear();
			
			for(Ball b:gameView.alBalls){
				b.go();
				if(b.isInHole()){					
					if(b==gameView.alBalls.get(0)){
						b.hide();
					}
					else{
						ballsToDelete.add(b);
					}					
				}
			}
			gameView.alBalls.removeAll(ballsToDelete);
			
			boolean allBallsStoppedFlag=true;
			for(Ball b:gameView.alBalls){
				if(!b.isStoped()){
					allBallsStoppedFlag=false;
					break;
				}
			}
			if(allBallsStoppedFlag){
				if(gameView.alBalls.get(0).isHided()){
					gameView.alBalls.get(0).reset();
				}
				gameView.cue.setShowCueFlag(true);
				if(gameView.alBalls.size()<=1){
					gameView.overGame();
				}
			}
		
			try {
				Thread.sleep(sleepSpan);
			} catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
        Log.d("BallGoThread","thread end...");
	}
	public void setFlag(boolean flag) {
		this.flag = flag;
	}
}