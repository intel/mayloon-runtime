package com.imps.tabletennis.tranning;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;


public class Cue {
	float x;
	float y;
	float rotateX;
	float rotateY;
	private float angdeg=0;
	float width;
	float height;
	float disWithBall=Constant.DIS_WITH_BALL;
	Bitmap bitmap;
	Ball mainBall;
	private boolean showCueFlag=true;
	private final float angleSpanSlow=0.2f;
	private final float angleSpanFast=1f;
	private boolean aimFlag=true;	
	private final float lineLength=Table.tableAreaWidth;
	private final  float backSpan=3;
	private final float forwardSpan=10;
	private final float maxDis=50;
	private float span=backSpan;
	private boolean showingAnimFlag=false;
	private boolean PrintTrace = false;
	public Cue(Bitmap bitmap,Ball mainBall)
	{
		this.bitmap=bitmap;
		this.mainBall=mainBall;		
		this.width=bitmap.getWidth();
		this.height=bitmap.getHeight();		
	}
	public void drawSelf(Canvas canvas,Paint paint)
	{
		if(!showCueFlag){
			return;
		}	
		
		this.rotateX=this.width+this.disWithBall+Ball.r;
		this.rotateY=this.height/2;
		float startX=(float) (mainBall.getX()+Constant.X_OFFSET+Ball.r);
		float startY=(float) (mainBall.getY()+Constant.Y_OFFSET+Ball.r);
		float angle = 0.0f;
		if(mx!=startX)
		{
			angle =(float) (180.0/3.1415926* Math.atan((my-startY)/(mx-startX)));
			if(mx-startX<0)
			{
				angle += 180.0;
			}
		}
		
		x=mainBall.getX()-width-disWithBall;
		y=mainBall.getY()+Ball.r-height/2;
		
		Matrix m1=new Matrix();
		m1.setTranslate(x+Constant.X_OFFSET,y+Constant.Y_OFFSET);
		Matrix m2=new Matrix();
		m2.setRotate(angle, rotateX, rotateY);
		Matrix mz=new Matrix();
		mz.setConcat(m1, m2);					
		canvas.drawBitmap(bitmap, mz,paint);

		canvas.save();
		canvas.clipRect(Table.lkx+Constant.X_OFFSET, Table.ady+Constant.Y_OFFSET, Table.efx+Constant.X_OFFSET, Table.jgy+Constant.Y_OFFSET);	
//		float angrad=(float)(3.1415926*180/angdeg);//(float) Math.toRadians(angdeg);
		float angrad=(float)(3.1415926*angdeg/180.0);
		//float startX=(float) (mainBall.getX()+Constant.X_OFFSET+Ball.r+Ball.r*Math.cos(angrad));
		//float startY=(float) (mainBall.getY()+Constant.Y_OFFSET+Ball.r+Ball.r*Math.sin(angrad));
		
		
		float stopX=startX+(float)(lineLength*Math.cos(angrad));
		float stopY=startY+(float)(lineLength*Math.sin(angrad));
		//float stopX = 
		paint.setColor(Color.YELLOW);
		paint.setAlpha(240);
		//canvas.drawLine(startX, startY, stopX, stopY, paint);
		canvas.drawLine(startX, startY, mx, my, paint);
		if(PrintTrace)System.out.println("Cur line position("+startX+","+startY+")"+"to("+stopX+","+stopY+")");
		canvas.restore();
		paint.setAlpha(255);
		angdeg = angle;
		PrintTrace = false;
	}
	private float mx=Table.AllBallsPos[1][0]+Constant.X_OFFSET+Constant.BALL_SIZE/2;
	private float my=Table.AllBallsPos[1][1]+Constant.Y_OFFSET+Constant.BALL_SIZE/2;
	public void calcuAngle(float pressX,float pressY)
	{
		
		float dirX=pressX-(mainBall.getX()+Ball.r+Constant.X_OFFSET);
		float dirY=pressY-(mainBall.getY()+Ball.r+Constant.Y_OFFSET);
		if(!aimFlag){
			dirX = -dirX;
			dirY = -dirY;
		}
		mx = pressX;my = pressY;
		if(dirY>=0)
		{
			angdeg=(float)((Math.atan(-dirX/dirY)+Math.PI/2)*180/3.1415926);//(float) Math.toDegrees((Math.atan(-dirX/dirY)+Math.PI/2));
		}
		else if(dirY<0)
		{
			angdeg=(float)((Math.atan(-dirX/dirY)+Math.PI*3/2)*180/3.1415926);//(float) Math.toDegrees((Math.atan(-dirX/dirY)+Math.PI*3/2));

		}
		PrintTrace = true;
	}

	public void rotateLeftSlowly(){
		angdeg+=angleSpanSlow;
	}

	public void rotateRightSlowly(){
		angdeg-=angleSpanSlow;
	}
	
	public void rotateLeftFast(){
		angdeg+=angleSpanFast;
	}
	
	public void rotateRightFast(){
		angdeg-=angleSpanFast;
	}
	
	public float changeDisWithBall()
	{
		
		if(disWithBall>=maxDis){
			span=-forwardSpan;
		}
		disWithBall+=span;
		return disWithBall;
	}
	
	public void resetAnimValues(){
		disWithBall=Constant.DIS_WITH_BALL;
		span=backSpan;
	}
	public float getAngle() {
		return angdeg;
	}	
	public boolean isShowCueFlag() {
		return showCueFlag;
	}
	public void setShowCueFlag(boolean showCueFlag) {
		this.showCueFlag = showCueFlag;
	}
	public boolean isAimFlag() {
		return aimFlag;
	}
	public void setAimFlag(boolean aimFlag) {
		this.aimFlag = aimFlag;
	}
	public boolean isShowingAnimFlag() {
		return showingAnimFlag;
	}
	public void setShowingAnimFlag(boolean showingAnimFlag) {
		this.showingAnimFlag = showingAnimFlag;
	}
}
