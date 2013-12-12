package com.imps.tabletennis.tranning;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class StrengthBar {
	
	private Bitmap downBmp;
	private Bitmap pointerBmp;
	final float x=Constant.BAR_X+Constant.X_OFFSET;
	final float y=Constant.BAR_Y+Constant.Y_OFFSET;	
	private float width;
	private float height;	
	private float currHeight;
	private int extendX=50;
	private int extendY=10;
	
	private float rainbowWidth=Constant.RAINBOW_WIDTH;
	private float rainbowHeight=Constant.RAINBOW_HEIGHT;
	private float rainbowGap=Constant.RAINBOW_GAP;
	private float rainbowX;
	private float rainbowY;
	
	private float pointerWidth;
	private float pointerHeight;
	public StrengthBar(Bitmap downBmp,Bitmap pointerBmp)
	{
		this.downBmp=downBmp;
		this.pointerBmp=pointerBmp;
		this.width=downBmp.getWidth();
		this.height=downBmp.getHeight();
		currHeight=downBmp.getHeight();
		rainbowX=Constant.RAINBOW_X+Constant.X_OFFSET;;
		rainbowY=height+Constant.RAINBOW_Y+Constant.Y_OFFSET;
		pointerWidth=pointerBmp.getWidth();
		pointerHeight=pointerBmp.getHeight();
		System.out.println("StrengthBar<<position("+(x-extendX)+","+(y-extendY)+","+(width+2*extendX)+","+(height+2*extendY)+")");
	}
	
	public void drawSelf(Canvas canvas,Paint paint)
	{
		canvas.drawBitmap(downBmp, x, y,paint);
		
		int n=(int) (currHeight/(rainbowHeight+rainbowGap));
		n=(n>ColorUtil.result.length)? ColorUtil.result.length :n;
		for(int i=0;i<n && i<ColorUtil.result.length;i++)
		{
			int[] c=ColorUtil.getColor(i);
			paint.setARGB(255, c[0], c[1], c[2]);
			float yTemp=rainbowY-(i*(rainbowHeight+rainbowGap));
			canvas.drawRect(rainbowX, yTemp, rainbowX+rainbowWidth, yTemp+rainbowHeight, paint);
		}
		
		canvas.drawBitmap(pointerBmp, x-pointerWidth, rainbowY-((n-1)*(rainbowHeight+rainbowGap))-pointerHeight/2,paint);//���Ƶ��µ���
	}
	
	public void changeCurrHeight(float pressX, float pressY)
	{
		if(pressY<=y){
			currHeight=this.height;
		}
		else if(pressY>=y+this.height){
			currHeight=0;
		}
		else{
			currHeight=this.height-(pressY-y);
		}
	}
	
	public boolean isActionOnBar(float pressX,float pressY)
	{
		return Constant.isPointInRect(pressX, pressY, 
				x-extendX, y-extendY, width+2*extendX, height+2*extendY);
	}
	public float getCurrHeight() {
		return currHeight;
	}
	public float getHeight() {
		return height;
	}
	
}
