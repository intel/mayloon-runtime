package com.imps.tabletennis.tranning;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class VirtualButton {
	float x;
	float y;
	int width;
	int height;
	Bitmap downBmp;
	Bitmap upBmp;
	boolean isDown=false;
	private int extendSize=20;
	public VirtualButton(Bitmap downBmp,Bitmap upBmp,float x,float y)
	{
		this.downBmp=downBmp;
		this.upBmp=upBmp;
		this.x=x+Constant.X_OFFSET;
		this.y=y+Constant.Y_OFFSET;
		this.width=upBmp.getWidth();
		this.height=upBmp.getHeight();
		System.out.println("VirtualButton<<position("+(x-extendSize)+","+(y-extendSize)+","+(width+2*extendSize)+","+(height+2*extendSize)+")");
	}
	public void drawSelf(Canvas canvas,Paint paint)
	{
		if(isDown)
		{
			canvas.drawBitmap(downBmp, x, y, paint);
		}
		else
		{
			canvas.drawBitmap(upBmp, x, y, paint);
		}
	}
	public void pressDown()
	{
		isDown=true;
	}
	public void releaseUp()
	{
		isDown=false;
	}
	
	public boolean isActionOnButton(float pressX,float pressY)
	{
		return Constant.isPointInRect(pressX, pressY, 
				x-extendSize, y-extendSize, width+2*extendSize, height+2*extendSize);
	}
}
