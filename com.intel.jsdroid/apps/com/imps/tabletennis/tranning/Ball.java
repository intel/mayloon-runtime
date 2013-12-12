package com.imps.tabletennis.tranning;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

public class Ball {
	private float x;
	private float y;
	static  float r=Constant.BALL_SIZE/2;
	static  float d=Constant.BALL_SIZE;
	static  float vMax=Constant.V_MAX;
	float vx;
	float vy;
	float timeSpan=2*Constant.TIME_SPAN;
	float vAttenuation=Constant.V_ATTENUATION;
	static float vMin=Constant.V_MIN;
	Bitmap[] bitmaps;
	private int bmpIndex=0;
	float bmpIndexf=0;
	Table table;
	GameView gameView;
	boolean InHoleflag=false;
	private float rotateX;
	private float rotateY;
	private float angdeg=0;
	public Ball(Bitmap[] bitmaps,GameView gameView,float vx,float vy,float[] pos)
	{
		this.bitmaps=bitmaps;
		this.gameView=gameView;
		this.vx=vx;
		this.vy=vy;
		this.x=pos[0];
		this.y=pos[1];
		this.table=gameView.table;
		rotateX=bitmaps[0].getWidth()/2;
		rotateY=bitmaps[0].getHeight()/2;
	}
	public static void scale(float ratio){
		r = r*ratio;
		d = d*ratio;
		vMax = vMax*ratio;
		vMin = vMin*ratio;
	}
	public void drawSelf(Canvas canvas,Paint paint)
	{
		calAngle();
	
		Matrix m1=new Matrix();
		m1.setTranslate(x+Constant.X_OFFSET,y+Constant.Y_OFFSET);
		Matrix m2=new Matrix();
		m2.setRotate(angdeg, rotateX, rotateY);
		Matrix mz=new Matrix();
		mz.setConcat(m1, m2);
		//canvas.drawBitmap(bitmaps[bmpIndex], x+Constant.X_OFFSET, y+Constant.Y_OFFSET, paint);
		canvas.drawBitmap(bitmaps[bmpIndex], mz,paint);
	}
	public void calAngle()
	{
		if(vx==0&&vy==0)
		{
			bmpIndex=0;
			return;
		}
		float dirX=vx;
		float dirY=vy;		
		if(dirY>=0)
		{
			angdeg=(float)((Math.atan(-dirX/dirY)+Math.PI/2)*180/3.1415926);//(float) Math.toDegrees((Math.atan(-dirX/dirY)+Math.PI/2));
		}
		else if(dirY<0)
		{
			angdeg=(float)((Math.atan(-dirX/dirY)+Math.PI*3/2)*180/3.1415926);//(float) Math.toDegrees((Math.atan(-dirX/dirY)+Math.PI*3/2));
		}		
	}
	
	public float getX() {
		return x;
	}
	public float getY() {
		return y;
	}
	public boolean canGo(float tempX,float tempY)
	{
		boolean canGoFlag=true;

		for(Ball b:gameView.alBalls)
		{
			if(b!=this && CollisionUtil.collisionCalculate(new float[]{tempX,tempY}, this, b))
			{
				if(gameView.activity.isSoundOn()){
					gameView.playSound(GameView.HIT_SOUND, 0);
				}
				canGoFlag=false;
			}
		}
		if(canGoFlag==false){
			return false;
		}

		for(int i=0;i<Table.holeCenterPoints.length;i++)
		{
			float disWithHole=CollisionUtil.mould(
					new float[]{tempX+Ball.r-Table.holeCenterPoints[i][0],
							tempY+Ball.r-Table.holeCenterPoints[i][1]});
			if(Table.holeCenterPoints[i]==Table.N || Table.holeCenterPoints[i]==Table.Q||i==1||i==4){
				if(disWithHole<=Table.middleHoleR){
					InHoleflag=true;
					break;
				}
			}else{
				if(disWithHole<=Table.cornerHoleR){
					InHoleflag=true;
					break;
				}
			}
		}
		if(InHoleflag==true){
			if(gameView.activity.isSoundOn()){
				gameView.playSound(GameView.BALL_IN_SOUND, 0);
			}
			return true;
		}

		float center[]={tempX+r,tempY+r};
		boolean collisionWithCornerFlag=false;
		for(float[] p:Table.collisionPoints)
		{
			if(CollisionUtil.calcuDisSquare(center, p)<=r*r)
			{
				collisionWithCornerFlag=true;
				canGoFlag=false;
			
				vx=-vx;
				vy=-vy;
				break;
			}
		}
		if(!collisionWithCornerFlag)
		{
		
			if(tempX<=Table.lkx||tempX+d>=Table.efx)
			{
				vx=-vx;
				canGoFlag=false;
			}else if(tempY<=Table.ady||tempY+d>=Table.jgy){
				vy=-vy;
				canGoFlag=false;
			}
		}		
		return canGoFlag;		
	}
	public void go()
	{
		if(isStoped()||Math.sqrt(vx*vx+vy*vy)<vMin){
			vx=0;
			vy=0;
			bmpIndex=0;
			return;
		}
		float tempX=x+vx*timeSpan;
		float tempY=y+vy*timeSpan;
		if(this.canGo(tempX, tempY))
		{
			x=tempX;
			y=tempY;			
			//Log.d("Ball","go,x:"+x+",y:"+y);
		
			float v=(float) Math.sqrt(vx*vx+vy*vy);
		
			bmpIndexf=bmpIndexf+Constant.K*v;
			bmpIndex=(int)(bmpIndexf)%bitmaps.length;
		
			vx*=vAttenuation;
			vy*=vAttenuation;
		}else{
			//Log.d("Ball","can't go.Tempx:"+tempX+",tempY:"+tempY);
		}
	}
	
	public boolean isStoped()
	{
		return (vx==0 && vy==0);
	}
	
	public void changeVxy(float v,float angle)
	{
		Log.d("Ball","Angle is:"+angle+",v:"+v);
		angdeg = angle;
		double angrad;
		if(angle==0){
			//angrad = 1;
			angrad=0;
		}
		else angrad=(float)(angle*3.1415926/180);//Math.toRadians(angle);
		vx=(float) (v*Math.cos(angrad));
		vy=(float) (v*Math.sin(angrad));
		Log.d("Ball","vx:"+vx+",vy:"+vy);
	}
	
	public boolean isInHole(){
		return InHoleflag;
	}
	
	public void hide(){
		vx=vy=0;
		x=y=-100000;		
	}
	
	public boolean isHided(){
		return y==-100000;
	}
	
	public void reset(){
		vx=vy=0;
		
		for(float iy=Table.AllBallsPos[0][1]; iy>Table.ady; iy-=Ball.r)
		{
			boolean collisionflag=false;
			for(Ball b:gameView.alBalls)
			{
				if(b!=this && CollisionUtil.isTwoBallsCollided(
						new float[]{Table.AllBallsPos[0][0],iy}, b))
				{
					collisionflag=true;
					break;
				}
			}
			
			if(!collisionflag){
				x=Table.AllBallsPos[0][0];
				y=iy;
				InHoleflag=false;
				return;
			}
		}
	}
}
