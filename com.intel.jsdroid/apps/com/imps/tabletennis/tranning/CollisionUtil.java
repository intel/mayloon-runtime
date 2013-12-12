package com.imps.tabletennis.tranning; 

public class CollisionUtil
{
	
	public static float dotProduct(float[] vec1,float[] vec2)
	{
		return
			vec1[0]*vec2[0]+
			vec1[1]*vec2[1];		
	} 	
	
	public static float mould(float[] vec)
	{
		return (float)Math.sqrt(vec[0]*vec[0]+vec[1]*vec[1]);
	}
	
	
	public static float angle(float[] vec1,float[] vec2)
	{
		
		float dp=dotProduct(vec1,vec2);
		
		float m1=mould(vec1);
		float m2=mould(vec2);
		
		float acos=dp/(m1*m2);
		
		
		if(acos>1)
		{
			acos=1;
		}
		else if(acos<-1)
		{
			acos=-1;
		}
		
		return (float) Math.acos(acos);
	}
	
	public static float calcuDisSquare(float[] p1,float[] p2)
	{
		return (p1[0]-p2[0])*(p1[0]-p2[0])+(p1[1]-p2[1])*(p1[1]-p2[1]);
	}
	
	public static boolean isTwoBallsCollided(float ballaTempXY[],Ball ballb){
		float BAx=ballaTempXY[0]-ballb.getX();
		float BAy=ballaTempXY[1]-ballb.getY();		
		
		float mvBA=mould(new float[]{BAx,BAy});	
		return (mvBA<Ball.d);		
	}
	
	public static boolean collisionCalculate(float ballaTempXY[],Ball balla,Ball ballb)
	{		
		
		float BAx=ballaTempXY[0]-ballb.getX();
		float BAy=ballaTempXY[1]-ballb.getY();
		
		
		float mvBA=mould(new float[]{BAx,BAy});	
		if(mvBA>Ball.d){			
			return false;
		}

		float vB=(float)Math.sqrt(ballb.vx*ballb.vx+ballb.vy*ballb.vy);
	
		float vbCollX=0;
		float vbCollY=0;
		
		float vbVerticalX=0;
		float vbVerticalY=0;
		
		
		if(balla.vMin<vB)
		{
			
			float bAngle=angle
			(
				new float[]{ballb.vx,ballb.vy},
			    new float[]{BAx,BAy}
			);
			
			
			float vbColl=vB*(float)Math.cos(bAngle);
			vbCollX=(vbColl/mvBA)*BAx;
			vbCollY=(vbColl/mvBA)*BAy;
			vbVerticalX=ballb.vx-vbCollX;
			vbVerticalY=ballb.vy-vbCollY;
		}
	
		float vA=(float)Math.sqrt(balla.vx*balla.vx+balla.vy*balla.vy);
		
		float vaCollX=0;
		float vaCollY=0;
		
		float vaVerticalX=0;
		float vaVerticalY=0;
		
		
		if(balla.vMin<vA)
		{
			float aAngle=angle
			(
				new float[]{balla.vx,balla.vy},
			    new float[]{BAx,BAy}
			);			
			
		
			float vaColl=vA*(float)Math.cos(aAngle);
			
			
			vaCollX=(vaColl/mvBA)*BAx;
			vaCollY=(vaColl/mvBA)*BAy;
			
			
			vaVerticalX=balla.vx-vaCollX;
			vaVerticalY=balla.vy-vaCollY;
		}
		
		balla.vx=vaVerticalX+vbCollX;
		balla.vy=vaVerticalY+vbCollY;
		
		ballb.vx=vbVerticalX+vaCollX;
		ballb.vy=vbVerticalY+vaCollY;	
		
		//System.out.println("ball aaaaaaaaaaaaaaaaaaaaaaaaaaaa "+balla.vx+" ======= "+balla.vy);
		//System.out.println("ball bbbbbbbbbbbbbbbbbbbbbb "+ballb.vx+" ======= "+ballb.vy);
		return true;
	}	
}
   
