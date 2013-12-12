package com.imps.tabletennis.tranning;

import android.util.Log;
import android.view.SurfaceHolder;

public class GameViewDrawThread extends Thread {
	private boolean flag = true;
	private int sleepSpan = 10;
	// Workaround: (variable is effected by other threads)
	private int cntDownNum = 0;
	GameView gameView;
	SurfaceHolder surfaceHolder;

	public GameViewDrawThread(GameView gameView) {
		this.gameView = gameView;
		this.surfaceHolder = gameView.getHolder();
	}
	
	public void run(){
		 /**
		 * @j2sNative
		 * console.log('GameViewDrawThread<<GO!');
		 */{}
        while (this.flag) {
            gameView.repaint();
            if(++this.cntDownNum >= 20)
            {
            	this.cntDownNum = 0;
            	gameView.timer.subtractTime(1);
            }
            try{
            	Thread.sleep(sleepSpan);
            }
            catch(Exception e){
            	e.printStackTrace();
            }
        }
	}
	
	public void setFlag(boolean flag) {
		this.flag = flag;
	}
}
