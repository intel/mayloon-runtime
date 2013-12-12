package com.imps.tabletennis.tranning;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.WindowManager;

public class GameActivity extends Activity {
	int currentView;
	MainMenuView mainMenuView;
    GameView gameView;
	ChoiceView choiceView;
	public int grade = 0;
	boolean coundDownModeFlag = true;
	private boolean backGroundMusicOn = false;
	private boolean soundOn = true;
	public static int initTime = 0;
    int currScore;
	int highestScore;

    SQLiteDatabase mDatabase;
    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            if(msg != null) {
                gotoGameView();
                return;
            }
        }
    };

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		//requestWindowFeature(Window.FEATURE_NO_TITLE); 
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN ,  
		              WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //setVolumeControlStream(AudioManager.STREAM_MUSIC);
        DisplayMetrics dm=new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm); 
        if(initTime==0) {
        	Constant.initConst(dm.heightPixels,dm.widthPixels);
        	initTime++;
        }
        System.out.println(dm.widthPixels+":"+dm.heightPixels);
        //gotoWellcomeView();
        gotoGameView();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == 4) {
            switch(currentView) {
                case WhatMessage.GOTO_WELLCOME_VIEW:
                    break;
                case WhatMessage.GOTO_MAIN_MENU_VIEW:
                    this.finish();
                    break;
                case WhatMessage.GOTO_HIGH_SCORE_VIEW:
                    gotoChoiceView();
                    break;
                case WhatMessage.GOTO_GAME_VIEW:
                case WhatMessage.GOTO_SOUND_CONTORL_VIEW:
                case WhatMessage.GOTO_WIN_VIEW:
                case WhatMessage.GOTO_FAIL_VIEW:
                case WhatMessage.GOTO_HELP_VIEW:
                case WhatMessage.GOTO_ABOUT_VIEW:
                case WhatMessage.GOTO_CHOICE_VIEW:
                    gotoMainMenuView();
                    break;
            }
            return true;
        }
		return super.onKeyDown(keyCode, e);
    }

    public void sendMessage(int what) {
        Message msg1 = myHandler.obtainMessage(what);
        myHandler.sendMessage(msg1);
    }
    public boolean isBackGroundMusicOn() {
		return backGroundMusicOn;
	}
	public void setBackGroundMusicOn(boolean backGroundMusicOn) {
		this.backGroundMusicOn = backGroundMusicOn;
	}

	public boolean isSoundOn() {
		return soundOn;
	}
	public void setSoundOn(boolean soundOn) {
		this.soundOn = soundOn;
	}

    private void gotoMainMenuView() {
        if (gameView != null) {
            gameView.stopAllThreads();
        }
        if (mainMenuView == null) {
            mainMenuView = new MainMenuView(this);
        }
        this.setContentView(mainMenuView);
        currentView = WhatMessage.GOTO_MAIN_MENU_VIEW;
    }

    private void gotoGameView() {
        if (gameView == null) {
            gameView = new GameView(this);
        }
        this.setContentView(gameView);
        currentView = WhatMessage.GOTO_GAME_VIEW;
    }

    private void gotoChoiceView() {
        if (choiceView == null) {
            choiceView = new ChoiceView(this);
        }
        this.setContentView(choiceView);
        currentView = WhatMessage.GOTO_CHOICE_VIEW;
    }
}