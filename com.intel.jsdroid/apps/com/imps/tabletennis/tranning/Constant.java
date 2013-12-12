package com.imps.tabletennis.tranning;
public class Constant {
	public static ScreenScaleResult ssr;
	public static float wRatio;
	public static float hRatio;
	public static int SCREEN_WIDTH;
	public static int SCREEN_HEIGHT;

	public static float BOTTOM_WIDTH=85*2+168*2+95;
	public static float BOTTOM_HEIGHT=85*2+228;
	public static float EDGE_BIG=45;
	public static float EDGE_SMALL=40;
	public static float MIDDLE=95;
	public static float DIS_CORNER=20;
	public static float DIS_MIDDLE=30;
	
	public static float TABLE_X=20;
	public static float TABLE_Y=20;
	public static float HOLE_CENTER_REVISE=20;
	public static float CORNER_HOLE_R=23;
	public static float MIDDLE_HOLE_R=32f;

	public static float X_OFFSET;
	public static float Y_OFFSET;

	public static float BALL_SIZE=24;
	public static float X_OFFESET_BALL1=350;
	public static float GAP_BETWEEN_BALLS=3;
	public static float DIS_WITH_MAIN_BALL=238;
	public static float V_MAX=150;
	public static float K=1.3f/V_MAX;
	
	public static float TIME_SPAN=0.05f;
	public static float V_ATTENUATION=0.99f;//0.996f;
	public static float V_MIN=1.5f;
	
	public static float DIS_WITH_BALL=10;
	
	public static float BAR_X=686;
	public static float BAR_Y=90;
	public static float RAINBOW_WIDTH=37;
	public static float RAINBOW_HEIGHT=9.22f;
	public static float RAINBOW_GAP=1f;
	public static float RAINBOW_X=BAR_X+7.5f;
	public static float RAINBOW_Y=BAR_Y-17;
	
	public static float GO_BTN_X=674;
	public static float GO_BTN_Y=360;
	public static float LEFT_BTN_X=390;
	public static float LEFT_BTN_Y=420;
	public static float RIGHT_BTN_X=LEFT_BTN_X-200;
	public static float RIGHT_BTN_Y=LEFT_BTN_Y;
	public static float AIM_BTN_X=556;
	public static float AIM_BTN_Y=424;
	public static float CHOICE_BTN_Y0=180;
	public static float CHOICE_BTN_Y1=CHOICE_BTN_Y0+90;
	public static float CHOICE_BTN_Y2=CHOICE_BTN_Y1+90;
	public static float SOUND_BTN_Y1=180;
	public static float SOUND_BTN_Y2=320;
	
	static int screenWidthTest=800;
	static int screenHeightTest=480;
	static float bigWidth=200;
	static float smallWidth=130;
	static float bigHeight=161;
	static float smallHeight=(smallWidth/bigWidth)*bigHeight;
    
	static float selectX=screenWidthTest/2-bigWidth/2;
	static float selectY=screenHeightTest/2-60;
	static float span=10;
	static float slideSpan=30;
	 
	static float totalSteps=10;
	static float percentStep=1.0f/totalSteps;
	static int timeSpan=20;
	
	static float TIMER_END_X=765;
	static float TIMER_END_Y=30;
	static float RI_QI_X=230;
	static float DE_FEN_X=500;
	static float DE_FEN_Y=170;
	
	static float BMP_Y=150;
	static float HELP_Y=150;
	
	public static void initConst(int screenWidth,int screenHeight)
	{
		SCREEN_WIDTH=screenWidth;
		SCREEN_HEIGHT=screenHeight;
		
		wRatio=screenWidth/(float)screenWidthTest;
		hRatio=screenHeight/(float)screenHeightTest;
		
		ssr=ScreenScaleUtil.calScale(screenWidth, screenHeight);		
		X_OFFSET=ssr.lucX;
		Y_OFFSET=ssr.lucY;
		
		BOTTOM_WIDTH*=ssr.ratio;
		BOTTOM_HEIGHT*=ssr.ratio;
		EDGE_BIG*=ssr.ratio;
		EDGE_SMALL*=ssr.ratio;
		MIDDLE*=ssr.ratio;
		DIS_CORNER*=ssr.ratio;
		DIS_MIDDLE*=ssr.ratio;
		BALL_SIZE*=ssr.ratio;
		X_OFFESET_BALL1*=ssr.ratio;
		GAP_BETWEEN_BALLS*=ssr.ratio;
		DIS_WITH_MAIN_BALL*=ssr.ratio;
		V_MAX*=ssr.ratio;
		
		TABLE_X*=ssr.ratio;
		TABLE_Y*=ssr.ratio;
		HOLE_CENTER_REVISE*=ssr.ratio;
		CORNER_HOLE_R*=ssr.ratio;
		MIDDLE_HOLE_R*=ssr.ratio;
		
		DIS_WITH_BALL*=ssr.ratio;
		
		BAR_X*=ssr.ratio;
		BAR_Y*=ssr.ratio;
		RAINBOW_WIDTH*=ssr.ratio;
		RAINBOW_HEIGHT*=ssr.ratio;
		RAINBOW_GAP*=ssr.ratio;
		RAINBOW_X*=ssr.ratio;
		RAINBOW_Y*=ssr.ratio;

		GO_BTN_X*=ssr.ratio;
		GO_BTN_Y*=ssr.ratio;
		LEFT_BTN_X*=ssr.ratio;
		LEFT_BTN_Y*=ssr.ratio;
		RIGHT_BTN_X*=ssr.ratio;
		RIGHT_BTN_Y*=ssr.ratio;
		AIM_BTN_X*=ssr.ratio;
		AIM_BTN_Y*=ssr.ratio;
		CHOICE_BTN_Y0*=ssr.ratio;
		CHOICE_BTN_Y1*=ssr.ratio;
		CHOICE_BTN_Y2*=ssr.ratio;
		SOUND_BTN_Y1*=ssr.ratio;
		SOUND_BTN_Y2*=ssr.ratio;
		
		bigWidth*=ssr.ratio;
		smallWidth*=ssr.ratio;
		bigHeight*=ssr.ratio;
		smallHeight*=ssr.ratio;    
		selectX*=ssr.ratio;
		selectY*=ssr.ratio;
		span*=ssr.ratio;
		slideSpan*=ssr.ratio;
		
		TIMER_END_X*=ssr.ratio;
		TIMER_END_Y*=ssr.ratio;
		RI_QI_X*=ssr.ratio;
		DE_FEN_X*=ssr.ratio;
		DE_FEN_Y*=ssr.ratio;

		BMP_Y*=ssr.ratio;
		HELP_Y*=ssr.ratio;
	}
	public static boolean IsTwoRectCross
	(
			float xLeftTop1,float yLeftTop1,float length1,float width1,
			float xLeftTop2,float yLeftTop2,float length2,float width2
	)
	{
		if
		(
				isPointInRect(xLeftTop1,yLeftTop1,xLeftTop2,yLeftTop2,length2,width2)||	
				isPointInRect(xLeftTop1+length1,yLeftTop1,xLeftTop2,yLeftTop2,length2,width2)||	
				isPointInRect(xLeftTop1,yLeftTop1+width1,xLeftTop2,yLeftTop2,length2,width2)||	
				isPointInRect(xLeftTop1+length1,yLeftTop1+width1,xLeftTop2,yLeftTop2,length2,width2)||	
				
				isPointInRect(xLeftTop2,yLeftTop2,xLeftTop1,yLeftTop1,length1,width1)||	
				isPointInRect(xLeftTop2+length2,yLeftTop2,xLeftTop1,yLeftTop1,length1,width1)||	
				isPointInRect(xLeftTop2,yLeftTop2+width2,xLeftTop1,yLeftTop1,length1,width1)||	
				isPointInRect(xLeftTop2+length2,yLeftTop2+width2,xLeftTop1,yLeftTop1,length1,width1)	
		)
		{
			return true;
		}
		return false;
	}
	public static boolean isPointInRect
	(
			float pointx,float pointy,
			float xLeftTop,float yLeftTop,float length,float width
	)
	{
		if(
				pointx>=xLeftTop&&pointx<=xLeftTop+length&&
				pointy>=yLeftTop&&pointy<=yLeftTop+width
		  )
		  {
			  return true;
		  }
		return false;
	}
}
