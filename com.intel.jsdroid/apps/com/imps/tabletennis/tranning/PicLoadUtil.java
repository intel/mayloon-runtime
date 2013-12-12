package com.imps.tabletennis.tranning;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

public class PicLoadUtil 
{

  
   public static Bitmap LoadBitmap(Resources res,int picId)
   {
	   Bitmap result=BitmapFactory.decodeResource(res, picId);
	   return result;
   }
   
  
   public static Bitmap scaleToFit(Bitmap bm,float ratio)
   {
   	float width = bm.getWidth();
   	float height = bm.getHeight();
   	
   	Matrix m1 = new Matrix(); 
   	m1.postScale(ratio, ratio);   	
   	Bitmap bmResult = Bitmap.createBitmap(bm, 0, 0, (int)width, (int)height, m1, true);       	
   	return bmResult;
   }
   
   public static Bitmap scaleToFitFullScreen(Bitmap bm,float wRatio,float hRatio)
   {
   	float width = bm.getWidth(); 
   	float height = bm.getHeight();
   	
   	Matrix m1 = new Matrix(); 
   	m1.postScale(wRatio, hRatio);   	
   	Bitmap bmResult = Bitmap.createBitmap(bm, 0, 0, (int)width, (int)height, m1, true);       	
   	return bmResult;
   }
}
