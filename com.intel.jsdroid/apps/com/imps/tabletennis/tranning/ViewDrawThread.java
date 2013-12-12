package com.imps.tabletennis.tranning;

import static com.imps.tabletennis.tranning.Constant.bigHeight;
import static com.imps.tabletennis.tranning.Constant.bigWidth;
import static com.imps.tabletennis.tranning.Constant.percentStep;
import static com.imps.tabletennis.tranning.Constant.smallHeight;
import static com.imps.tabletennis.tranning.Constant.smallWidth;
import static com.imps.tabletennis.tranning.Constant.span;
import static com.imps.tabletennis.tranning.Constant.timeSpan;
import static com.imps.tabletennis.tranning.Constant.totalSteps;

public class ViewDrawThread extends Thread{

	MainMenuView mv;
	int afterCurrentIndex;
	static boolean flag;
	public ViewDrawThread(MainMenuView mv,int afterCurrentIndex)
	{
		this.mv=mv;
		this.afterCurrentIndex=afterCurrentIndex;
	}
	@Override
	public void run()
	{
		
		for(int i=0;i<=totalSteps;i++)
		{
			mv.changePercent=percentStep*i;
			
			mv.init();		

			if(mv.anmiState==1)
			{
				mv.currentX=mv.currentX+(bigWidth+span)*mv.changePercent;
				mv.currentY=mv.currentY+(bigHeight-smallHeight)*mv.changePercent;
				mv.currentWidth=(int)(smallWidth+(bigWidth-smallWidth)*(1-mv.changePercent));
				mv.currentHeight=(int)(smallHeight+(bigHeight-smallHeight)*(1-mv.changePercent));
			
				mv.leftWidth=(int)(smallWidth+(bigWidth-smallWidth)*mv.changePercent);
				mv.leftHeight=(int)(smallHeight+(bigHeight-smallHeight)*mv.changePercent);				
			}
			else if(mv.anmiState==2)
			{
				mv.currentX=mv.currentX-(smallWidth+span)*mv.changePercent;
				mv.currentY=mv.currentY+(bigHeight-smallHeight)*mv.changePercent;
				mv.currentWidth=(int)(smallWidth+(bigWidth-smallWidth)*(1-mv.changePercent));
				mv.currentHeight=(int)(smallHeight+(bigHeight-smallHeight)*(1-mv.changePercent));

				mv.rightWidth=(int)(smallWidth+(bigWidth-smallWidth)*mv.changePercent);
				mv.rightHeight=(int)(smallHeight+(bigHeight-smallHeight)*mv.changePercent);					
			}			

			mv.tempxLeft=mv.currentX-(span+mv.leftWidth);			
			mv.tempyLeft=mv.currentY+(mv.currentHeight-mv.leftHeight);	

			mv.tempxRight=mv.currentX+(span+mv.currentWidth);
			mv.tempyRight=mv.currentY+(mv.currentHeight-mv.rightHeight);

			mv.repaint();			
			try
			{
				Thread.sleep(timeSpan);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		mv.anmiState=0;
	
		mv.currentIndex=afterCurrentIndex;
		mv.init();		
		
		mv.repaint(); 
	}

}
