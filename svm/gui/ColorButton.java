package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import svm.SVM;
import javax.swing.JColorChooser;

public class ColorButton extends Panel{
	Settings settings;
	int i;
	
	public ColorButton(Settings settings, int i){
		this.settings = settings;
		this.i = i;
	}	
	
	public void paint(Graphics g){
		g.drawImage(settings.svm.color,0,0,this);
	}
	
	public boolean handleEvent(Event e){
		if(e.id==Event.MOUSE_DOWN){
			Color selectColor = JColorChooser.showDialog(this, "Choose a color", Color.RED);
			if(selectColor != null){
				switch(i){
					case 1:
						settings.bkg_r.setText(""+selectColor.getRed());
						settings.bkg_g.setText(""+selectColor.getGreen());
						settings.bkg_b.setText(""+selectColor.getBlue());					
						break;
					case 2:
						settings.axis_r.setText(""+selectColor.getRed());
						settings.axis_g.setText(""+selectColor.getGreen());
						settings.axis_b.setText(""+selectColor.getBlue());					
						break;					
					case 3:
						settings.grid_r.setText(""+selectColor.getRed());
						settings.grid_g.setText(""+selectColor.getGreen());
						settings.grid_b.setText(""+selectColor.getBlue());					
						break;
					case 4:
						settings.class0_point_r.setText(""+selectColor.getRed());
						settings.class0_point_g.setText(""+selectColor.getGreen());
						settings.class0_point_b.setText(""+selectColor.getBlue());					
						break;
					case 5:
						settings.class1_point_r.setText(""+selectColor.getRed());
						settings.class1_point_g.setText(""+selectColor.getGreen());
						settings.class1_point_b.setText(""+selectColor.getBlue());					
						break;	
					case 6:
						settings.line_r.setText(""+selectColor.getRed());
						settings.line_g.setText(""+selectColor.getGreen());
						settings.line_b.setText(""+selectColor.getBlue());					
						break;	
					case 7:
						settings.string_r.setText(""+selectColor.getRed());
						settings.string_g.setText(""+selectColor.getGreen());
						settings.string_b.setText(""+selectColor.getBlue());					
						break;						
				}
			}
		}	
		return super.handleEvent(e);
	}

}

