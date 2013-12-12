package com.imps.tabletennis.tranning;

public class ScreenScaleUtil
{
	static final float sHpWidth=800;
	static final float sHpHeight=480;
	static final float whHpRatio=sHpWidth/sHpHeight;
	
	
	static final float sSpWidth=480;
	static final float sSpHeight=800;
	static final float whSpRatio=sSpWidth/sSpHeight;
	
	
	public static ScreenScaleResult calScale
	(
		float targetWidth,	
		float targetHeight	
	)
	{
		ScreenScaleResult result=null;
		ScreenOrien so=null;
		
	
		if(targetWidth>targetHeight)
		{
			so=ScreenOrien.HP;
		}
		else
		{
			so=ScreenOrien.SP;
		}
		
		System.out.println(so);
		
		
		
		if(so==ScreenOrien.HP)
		{
			
			float targetRatio=targetWidth/targetHeight;
			
			if(targetRatio>whHpRatio)
			{
		    
			    float ratio=targetHeight/sHpHeight;
			    float realTargetWidth=sHpWidth*ratio;
			    float lcuX=(targetWidth-realTargetWidth)/2.0f;
			    float lcuY=0;
			    result=new ScreenScaleResult((int)lcuX,(int)lcuY,ratio,so);	
			}
			else
			{
				
				float ratio=targetWidth/sHpWidth;
				float realTargetHeight=sHpHeight*ratio;
				float lcuX=0;
				float lcuY=(targetHeight-realTargetHeight)/2.0f;
				result=new ScreenScaleResult((int)lcuX,(int)lcuY,ratio,so);	
			}
		}
		
		
		if(so==ScreenOrien.SP)
		{
			
			float targetRatio=targetWidth/targetHeight;
			
			if(targetRatio>whSpRatio)
			{
					    
			    float ratio=targetHeight/sSpHeight;
			    float realTargetWidth=sSpWidth*ratio;
			    float lcuX=(targetWidth-realTargetWidth)/2.0f;
			    float lcuY=0;
			    result=new ScreenScaleResult((int)lcuX,(int)lcuY,ratio,so);	
			}
			else
			{
				
				float ratio=targetWidth/sSpWidth;
				float realTargetHeight=sSpHeight*ratio;
				float lcuX=0;
				float lcuY=(targetHeight-realTargetHeight)/2.0f;
				result=new ScreenScaleResult((int)lcuX,(int)lcuY,ratio,so);	
			}
			
		}		
		return result;
	}
}