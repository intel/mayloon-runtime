package com.imps.tabletennis.tranning;
import com.imps.tabletennis.tranning.R;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import static com.imps.tabletennis.tranning.Constant.*;

public class MainMenuView extends SurfaceView implements SurfaceHolder.Callback
{
	GameActivity activity;
	Paint paint;
	Bitmap[] menu;
	Bitmap bj;
	int currentIndex=2;
	float mPreviousX;
	float mPreviousY;
	float changePercent=0;
	int anmiState=0;
	
	float currentWidth;
	float currentHeight;
	float currentX;
	float currentY;
			
	float leftWidth;	
	float leftHeight;
	float tempxLeft;
	float tempyLeft;
	
	float rightWidth;
	float rightHeight;
	float tempxRight;
	float tempyRight;
	
	static float initial_Width;
	static float initial_Height;
	
	final int ABOUT_VIEW=0;			
	final int HELP_VIEW=1;			
	final int START_VIEW=2;			
	final int SETUP_VIEW=3;			
	final int EXIT_VIEW=4;			
	
	public MainMenuView(GameActivity activity)
	{
		super(activity);
		this.activity=activity;
		this.getHolder().addCallback(this);
		paint=new Paint();
		paint.setAntiAlias(true);
		initBitmap(activity.getResources());
		
		init();
	}
	 @Override 
	    public boolean onTouchEvent(MotionEvent e) {
	    	
	    	if(anmiState!=0)
	    	{
	    		return true;
	    	}
	    	
	        float x = e.getX();
	        float y = e.getY();
	        
	        
	        switch (e.getAction()) 
	        {
	        	case MotionEvent.ACTION_DOWN:
	        	 
	        	  mPreviousX=x;
	        	  mPreviousY=y;
	            break;
	            case MotionEvent.ACTION_UP:
	             	
	              float dx=x- mPreviousX;
	              
	              if(dx<-slideSpan)
	              {
	            	  if(currentIndex<menu.length-1)
	            	  {
	            		  int afterCurrentIndex=currentIndex+1;
	            		  
	            		  anmiState=2;
	            		  new ViewDrawThread(this,afterCurrentIndex).start();
	            	  }
	              }
	              else if(dx>slideSpan)  
	              {
	            	  if(currentIndex>0)
	            	  {
	            		 
	            		  int afterCurrentIndex=currentIndex-1;
	            		  
	            		  anmiState=1;
	            		  new ViewDrawThread(this,afterCurrentIndex).start();
	            	  }            	  
	              }
					else
					{
						if(
			                Constant.isPointInRect(mPreviousX, mPreviousY, 
			                		selectX+Constant.X_OFFSET, selectY+Constant.Y_OFFSET, bigWidth, bigHeight)&&
			                Constant.isPointInRect(x, y, 
			                		selectX+Constant.X_OFFSET, selectY+Constant.Y_OFFSET, bigWidth, bigHeight)
						)							 
						{							
							switch(currentIndex)
							{								
								case ABOUT_VIEW:
									activity.sendMessage(WhatMessage.GOTO_ABOUT_VIEW);	
									break;
								case HELP_VIEW:
									activity.sendMessage(WhatMessage.GOTO_HELP_VIEW);		
									break;
								case START_VIEW:
									activity.sendMessage(WhatMessage.GOTO_CHOICE_VIEW); 
									break;
								case SETUP_VIEW:
									activity.sendMessage(WhatMessage.GOTO_SOUND_CONTORL_VIEW);
									break;
                                case EXIT_VIEW:
                                    activity.finish();
                                    break;
							}
						}
					}
				 break; 
	        }   
	        return true;        
	    }
	
	@Override
	public void onDraw(Canvas canvas)
	{
		//canvas.drawColor(Color.GRAY);
		
		canvas.drawBitmap(bj, 0, 0, paint);
		
		
		float ratioX=currentWidth/initial_Width;
		float ratioY=currentHeight/initial_Height;
		
		drawBitmap(canvas,currentX+Constant.X_OFFSET,currentY+Constant.Y_OFFSET,ratioX,ratioY,menu[currentIndex]);

		
		if(currentIndex>0)
		{				
			
			ratioX=leftWidth/initial_Width;
			ratioY=leftHeight/initial_Height;
			
			drawBitmap(canvas,tempxLeft+Constant.X_OFFSET, tempyLeft+Constant.Y_OFFSET,ratioX,ratioY,menu[currentIndex-1]);
		}			
		
		
		if(currentIndex<menu.length-1)
		{
			
			ratioX=rightWidth/initial_Width;
			ratioY=rightHeight/initial_Height;
		
			drawBitmap(canvas,tempxRight+Constant.X_OFFSET,tempyRight+Constant.Y_OFFSET,ratioX,ratioY,menu[currentIndex+1]);
		}
		
		for(int i=currentIndex-2;i>=0;i--)
		{	
			float tempx=tempxLeft-(span+smallWidth)*(currentIndex-1-i);
			if(tempx<-smallWidth)
			{
				break;
			}
			float tempy=selectY+(bigHeight-smallHeight);
			

			ratioX=smallWidth/initial_Width;
			ratioY=smallHeight/initial_Height;

			drawBitmap(canvas,tempx+Constant.X_OFFSET,tempy+Constant.Y_OFFSET,ratioX,ratioY,menu[i]);
		}
		
		for(int i=currentIndex+2;i<menu.length;i++)
		{
			float tempx=tempxRight+rightWidth+span+(span+smallWidth)*(i-(currentIndex+1)-1);			
			if(tempx>screenWidthTest)
			{
				break;
			}			
			float tempy=selectY+(bigHeight-smallHeight);	
			
			
			ratioX=smallWidth/initial_Width;
			ratioY=smallHeight/initial_Height;
			
			drawBitmap(canvas,tempx+Constant.X_OFFSET,tempy+Constant.Y_OFFSET,ratioX,ratioY,menu[i]);
		}
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) 
	{
		
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		repaint();
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		
	}
	public void repaint()
	{
		SurfaceHolder mHolder=this.getHolder();
		Canvas canvas=mHolder.lockCanvas();
		try
		{
			synchronized(mHolder)
			{
				onDraw(canvas);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}  
		finally
		{
			if(canvas!=null)
			{
				mHolder.unlockCanvasAndPost(canvas);
			}
		}
	}
	
	public void initBitmap(Resources r)
	{
		menu=new Bitmap[]{
				BitmapFactory.decodeResource(r, R.drawable.menu0),
				BitmapFactory.decodeResource(r, R.drawable.menu1),
				BitmapFactory.decodeResource(r, R.drawable.menu2),
				BitmapFactory.decodeResource(r, R.drawable.menu3),
				BitmapFactory.decodeResource(r, R.drawable.menu4),
		};
		
		bj=BitmapFactory.decodeResource(r, R.drawable.help);
	
		for(int i=0;i<menu.length;i++){
			menu[i]=PicLoadUtil.scaleToFit(menu[i], Constant.ssr.ratio);
		}
		bj=PicLoadUtil.scaleToFitFullScreen(bj, Constant.wRatio, Constant.hRatio);	
		initial_Width=menu[0].getWidth();
		initial_Height=menu[0].getHeight();
	}
	
	public void init()
	{
		currentWidth=bigWidth;
		currentHeight=bigHeight;
		currentX=selectX;
		currentY=selectY;
		rightWidth=smallWidth;
		leftWidth=smallWidth;	
		leftHeight=smallHeight;
		rightHeight=smallHeight;
		tempxLeft=currentX-(span+leftWidth);
		tempyLeft=currentY+(currentHeight-leftHeight);
		tempxRight=currentX+(span+currentWidth);
		tempyRight=currentY+(currentHeight-rightHeight);
	}
	public void drawBitmap
	(
		Canvas c,
		float x,float y,
		float xRatio,float yRatio,
		Bitmap bm
	)
	{
	
		Matrix m1=new Matrix();
		m1.setScale(xRatio, yRatio);
		
		Matrix m2=new Matrix();
		m2.setTranslate(x, y);
		
		Matrix mz=new Matrix();
		mz.setConcat(m2, m1);
	
		c.drawBitmap(bm, mz, paint);
	}	
}
