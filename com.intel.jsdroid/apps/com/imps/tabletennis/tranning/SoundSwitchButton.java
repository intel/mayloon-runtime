package com.imps.tabletennis.tranning;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class SoundSwitchButton {
	
	Bitmap onBitmap;
	Bitmap offBitmap;
	float btnX;
	float btnY;	
	float btnWidth;
	float btnHeight;
	
	Bitmap onTextBitmap;
	Bitmap offTextBitmap;
	float textX;
	float textY;
	float textWidth;
	float textHeight;
	
	float space;
	private boolean isOn=false;
	public SoundSwitchButton(Bitmap onTextBitmap,Bitmap offTextBitmap,Bitmap onBitmap,Bitmap offBitmap,float textX,float textY,boolean isOn)
	{		
		this.onTextBitmap=onTextBitmap;
		this.offTextBitmap=offTextBitmap;
		this.onBitmap=onBitmap;
		this.offBitmap=offBitmap;
		this.textX=textX;
		this.textY=textY;
		this.textWidth=onTextBitmap.getWidth();
		this.textHeight=onTextBitmap.getWidth();
		
		this.btnX=textX+textWidth+space;
		this.btnY=textY;
		this.btnWidth=onBitmap.getWidth();
		this.btnHeight=onBitmap.getWidth();
		this.isOn=isOn;
	}
	public void drawSelf(Canvas canvas,Paint paint)
	{		
		if(isOn)
		{
			canvas.drawBitmap(onTextBitmap, textX, textY, paint);
			canvas.drawBitmap(onBitmap, btnX, btnY, paint);
		}
		else
		{
			canvas.drawBitmap(offTextBitmap, textX, textY, paint);
			canvas.drawBitmap(offBitmap, btnX, btnY, paint);
		}
	}
	
	public boolean isActionOnButtonOn(int pressX,int pressY)
	{
		return Constant.isPointInRect(pressX, pressY, btnX, btnY, btnWidth/2.0f, btnHeight);
	}
	
	public boolean isActionOnButtonOff(int pressX,int pressY)
	{
		return Constant.isPointInRect(pressX, pressY, btnX+btnWidth/2.0f, btnY, btnWidth/2.0f, btnHeight);
	}
	public void switchOn()
	{
		isOn=true;
	}
	public void switchOff()
	{
		isOn=false;
	}
}
