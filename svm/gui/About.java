package gui;

import java.awt.*;
import java.awt.event.*;
import svm.SVM;

public class About extends Dialog{
	SVM svm;
	Font font = new Font("Helvetica", 0, 12);
	FontMetrics fm = getFontMetrics(font);
	Font font1 = new Font("Helvetica", Font.BOLD, 18);
	FontMetrics fm1 = getFontMetrics(font1);	
	int w, h;
	Slider slider;
	
	public About(SVM svm){
		super(svm, "About", true);
		this.svm = svm;
		setBackground(svm.settings.background_color);
		setResizable(false);
		setLayout(null);
		
		slider = new Slider(this, 150, 50);
		slider.setBounds(20, 340, 150, 50);
		add(slider);
	}
	
	public boolean handleEvent(Event e){
		if(e.id==Event.WINDOW_DESTROY || e.id==Event.MOUSE_DOWN){
			dispose();
		}	
		return super.handleEvent(e);
	}	
	
	public void paint(Graphics g){
		w = size().width;
		h = size().height;
		g.setColor(Color.white);
		g.drawImage(svm.bkg,0,0,this);
		int i=0, j=0;
		String message = "";
		
		g.setFont(font);
		message = "SVM Simulator is a support application for the course";
		i = fm.stringWidth(message);
		g.drawString(message, w-i-10, 60);
		
		g.setFont(font1);
		message = "Artificial Intelligence";
		j = fm1.stringWidth(message);
		g.drawString(message, w-j-(i-j)/2-10, 90);		
		
		g.setFont(font);
		message = "Faculty of Mathematics";
		i = fm.stringWidth(message);
		g.drawString(message, w-i-10, 150);
		
		message = "\"Alexandru Ioan Cuza\" University";
		i = fm.stringWidth(message);
		g.drawString(message, w-i-10, 170);	

		message = "Ia\u015Fi, Romania";
		i = fm.stringWidth(message);
		g.drawString(message, w-i-10, 190);			
		
		message = "2026 \u00A9 D.Rusu";
		i = fm.stringWidth(message);
		g.drawString(message, w-i-10, h-10);
		
		message = "Students:";
		g.drawString(message, 10, 325);		
	}

}