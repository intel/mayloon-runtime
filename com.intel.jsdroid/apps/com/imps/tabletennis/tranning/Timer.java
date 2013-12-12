package com.imps.tabletennis.tranning;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class Timer {
	GameView gameView;
	private final int totalSecond=12*60;
	private Bitmap breakMarkBitmap;
	private Bitmap[] numberBitmaps;	
	private int leftSecond=totalSecond;
	float endX=Constant.TIMER_END_X;
	float endY=Constant.TIMER_END_Y;
	int numberWidth;
	int numberHeight;
	int breakMarkWidth;
	int breakMarkHeight;
	public Timer(GameView gameView,Bitmap breakMarkBitmap,Bitmap[] numberBitmaps)
	{
		this.gameView=gameView;
		this.breakMarkBitmap=breakMarkBitmap;
		this.numberBitmaps=numberBitmaps;
		numberWidth=numberBitmaps[0].getWidth();
		numberHeight=numberBitmaps[0].getHeight();		
		breakMarkWidth=breakMarkBitmap.getWidth();
		breakMarkHeight=breakMarkBitmap.getHeight();
	}
	
	public void drawSelf(Canvas canvas,Paint paint)
	{
		int second=this.leftSecond%60;
		int minute=this.leftSecond/60;		
		drawNumberBitmap(second,numberBitmaps,endX+Constant.X_OFFSET,endY+Constant.Y_OFFSET,canvas, paint);
		
		int secondLength=(second+"").length()<=1 ? (second+"").length()+1 : (second+"").length();
		float breakMarkX=endX-secondLength*numberWidth-breakMarkWidth;
		float breakMarkY=endY;
		canvas.drawBitmap(breakMarkBitmap, breakMarkX+Constant.X_OFFSET, breakMarkY+Constant.Y_OFFSET,paint);//����ʱ��ָ���ͼƬ
		
		float miniteEndX=breakMarkX;
		float miniteEndY=endY;
		drawNumberBitmap(minute,numberBitmaps,miniteEndX+Constant.X_OFFSET,miniteEndY+Constant.Y_OFFSET,canvas, paint);
	}
	
	public void subtractTime(int second)
	{
		if(this.leftSecond>0)
		{
			this.leftSecond-=second;
		}
		else
		{
			gameView.overGame();
		}
	}
	
	public void drawNumberBitmap(int number,Bitmap[] numberBitmaps,float endX,float endY,Canvas canvas,Paint paint)
	{
		String numberStr=number+"";
		if(number<10){
			numberStr="0"+numberStr;
		}
		for(int i=0;i<numberStr.length();i++)
		{
			char c=numberStr.charAt(i);
			canvas.drawBitmap
			(
					numberBitmaps[c-'0'], 
					endX-numberWidth*(numberStr.length()-i), 
					endY, 
					paint
			);
		}
	}
	public int getLeftSecond() {
		return leftSecond;
	}
}
