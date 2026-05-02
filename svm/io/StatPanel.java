package io;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class StatPanel extends JPanel implements ItemListener, MouseListener, MouseMotionListener{
	InputData id;
	Image im;
	Graphics img;	
	JCheckBox jcbc0, jcbc1;
	JLabel label;
	boolean init = true;
	int ww, hh;	
	int Ox, Oy;
	int OxLength, OyLength;
	int rate = 10;	//rata de esantionare
	int zec = 1; 	// nr de zecimale in afisarea valorilor pe axa statisticilor
	boolean bc0=true, bc1=true;
	int index;
	boolean status;
	
	//Font f = new Font("Serif", 0, 16);
	//Font f = new Font("Tahoma", 0, 13);
	Font f = new Font("Helvetica", 0, 12);
	FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
	
	public StatPanel(InputData id){
		this.id = id;
		setLayout(null);
		setBackground(id.svm.settings.button_color_default);
		setForeground(id.svm.settings.string_color_default);
		
		jcbc0 = new JCheckBox("Class 0", true);
		jcbc0.setAlignmentX(Component.LEFT_ALIGNMENT);
		jcbc0.setBackground(id.svm.settings.button_color_default);
		jcbc0.setForeground(id.svm.settings.string_color_default);
		jcbc0.addItemListener(this);
		jcbc0.setBounds(20,3,100,20);
		add(jcbc0);			
			
		jcbc1 = new JCheckBox("Class 1", true);
		jcbc1.setAlignmentX(Component.LEFT_ALIGNMENT);
		jcbc1.setBackground(id.svm.settings.button_color_default);
		jcbc1.setForeground(id.svm.settings.string_color_default);
		jcbc1.addItemListener(this);
		jcbc1.setBounds(20,26,100,20);
		add(jcbc1);	

	}

	public void paint(Graphics g){update(g);}
	
	public void update(Graphics g){
		if(init){
			ww = size().width;
			hh = size().height;
			Ox = 30; Oy = hh-20;
			OxLength = ww - 2*Ox;
			OyLength = 250;
			im = createImage(ww, hh);
			img = im.getGraphics();	
			img.setFont(f);
			init = false;			
		}
		g.drawImage(im,0,0,this);
		jcbc0.repaint();
		jcbc1.repaint();
	}
	
	
	// pentru 2 clase (de modificat pt n clase!)
	public void drawGraph(int index){ 		
		int[] cc0 = new int[rate];
		int[] cc1 = new int[rate];
		float min = id.attributes[index].attr_statistic.min_value;
		float max = id.attributes[index].attr_statistic.max_value;	
		float min0 = id.attributes[index].class_statistic[0].min_value;
		float max0 = id.attributes[index].class_statistic[0].max_value;			
		float min1 = id.attributes[index].class_statistic[1].min_value;
		float max1 = id.attributes[index].class_statistic[1].max_value;			
		
		for(int i=0; i<id.V.length; i++){
			if(id.V[i].cl.Y == 0){
				float value = id.V[i].X[index];
				int k = (int)((value-min0)*rate/(max0-min0));
				if(k==rate)k--;
				cc0[k]++;
			}
		}
		for(int i=0; i<id.V.length; i++){
			if(id.V[i].cl.Y == 1){
				float value = id.V[i].X[index];
				int k = (int)((value-min1)*rate/(max1-min1));
				if(k==rate)k--;
				cc1[k]++;
			}
		}		

		int MIN0 = Integer.MAX_VALUE, MAX0 = Integer.MIN_VALUE;
		int MIN1 = Integer.MAX_VALUE, MAX1 = Integer.MIN_VALUE;
		for(int i=0; i<rate; i++){
			if(cc0[i] < MIN0) MIN0 = cc0[i];
			if(cc0[i] > MAX0) MAX0 = cc0[i];
			if(cc1[i] < MIN1) MIN1 = cc1[i];
			if(cc1[i] > MAX1) MAX1 = cc1[i];			
		}
		int r = OxLength/rate;
		float r0 = (float)OyLength/MAX0;
		float r1 = (float)OyLength/MAX1;
		int a = 260, b = 15, y = 15;
		if(bc0){
			for(int i=0; i<cc0.length; i++){
				img.setColor(id.svm.settings.class0_point_color);
				img.fillRect(Ox+i*r,Oy-(int)(r0*cc0[i]),r,(int)(r0*cc0[i]));	
			}
			for(int i=0; i<cc0.length; i++){
				img.setColor(id.svm.settings.button_color_default);	
				img.drawRect(Ox+i*r,Oy-(int)(r0*cc0[i]),r,(int)(r0*cc0[i]));	
			}
			img.setColor(Color.white);
			if(!bc1){
				for(int i=0; i<cc0.length; i++)						
					img.drawString(""+cc0[i],Ox+i*r,Oy-(int)(r0*cc0[i])-3);	
				img.drawString(id.attributes[index].class_statistic[0].statistic_name,a,b);
				img.drawString("Objects count: "+id.attributes[index].class_statistic[0].objects_count,a,b+y);
				img.drawString("Min value: "+min0,a,b+2*y);
				img.drawString("Max value: "+max0,a,b+3*y);
				img.drawString("Mean: "+id.attributes[index].class_statistic[0].mean,a,b+4*y);
				img.drawString("StdDev \u03C3 : "+id.attributes[index].class_statistic[0].stdDev,a,b+5*y);
				
				String s = roundAtK(min0,zec);
				int q = fm.stringWidth(s)/2;
				img.drawString(s,Ox-q,Oy+15);
				s = roundAtK(max0,zec);
				q = fm.stringWidth(s)/2;
				img.drawString(s,Ox+rate*r-q,Oy+15);
				s = roundAtK((min0+max0)/2,zec);
				q = fm.stringWidth(s)/2;
				img.drawString(s,Ox+rate*r/2-q,Oy+15);					
			}	
		}	
		if(bc1){
			for(int i=0; i<cc1.length; i++){
				img.setColor(id.svm.settings.class1_point_color);
				img.fillRect(Ox+i*r,Oy-(int)(r1*cc1[i]),r,(int)(r1*cc1[i]));	
			}
			for(int i=0; i<cc1.length; i++){
				img.setColor(id.svm.settings.button_color_default);	
				img.drawRect(Ox+i*r,Oy-(int)(r1*cc1[i]),r,(int)(r1*cc1[i]));	
			}
			img.setColor(Color.white);
			if(!bc0){
				for(int i=0; i<cc1.length; i++)	
					img.drawString(""+cc1[i],Ox+i*r,Oy-(int)(r1*cc1[i])-3);			
				img.drawString(id.attributes[index].class_statistic[1].statistic_name,a,b);
				img.drawString("Objects count: "+id.attributes[index].class_statistic[1].objects_count,a,b+y);
				img.drawString("Min value: "+min1,a,b+2*y);
				img.drawString("Max value: "+max1,a,b+3*y);
				img.drawString("Mean: "+id.attributes[index].class_statistic[1].mean,a,b+4*y);
				img.drawString("StdDev \u03C3 : "+id.attributes[index].class_statistic[1].stdDev,a,b+5*y);
				
				String s = roundAtK(min1,zec);
				int q = fm.stringWidth(s)/2;
				img.drawString(s,Ox-q,Oy+15);
				s = roundAtK(max1,zec);
				q = fm.stringWidth(s)/2;
				img.drawString(s,Ox+rate*r-q,Oy+15);
				s = roundAtK((min1+max1)/2,zec);
				q = fm.stringWidth(s)/2;
				img.drawString(s,Ox+rate*r/2-q,Oy+15);			
			}
		}
		if(bc0 && bc1){
			img.setColor(Color.white);
			for(int i=0; i<cc0.length; i++){
				int total = cc0[i] + cc1[i];
				boolean flag = r0*cc0[i] > r1*cc1[i];	
				if(flag)
					img.drawString(""+total,Ox+i*r,Oy-(int)(r0*cc0[i])-3);
				else
					img.drawString(""+total,Ox+i*r,Oy-(int)(r1*cc1[i])-3);
			}	
			
			min = id.attributes[index].attr_statistic.min_value;
			max = id.attributes[index].attr_statistic.max_value;
			img.drawString(id.attributes[index].attr_statistic.statistic_name,a,b);
			img.drawString("Objects count: "+id.attributes[index].attr_statistic.objects_count,a,b+y);
			img.drawString("Min value: "+min,a,b+2*y);
			img.drawString("Max value: "+max,a,b+3*y);
			img.drawString("Mean: "+id.attributes[index].attr_statistic.mean,a,b+4*y);
			img.drawString("StdDev \u03C3 : "+id.attributes[index].attr_statistic.stdDev,a,b+5*y);
				
			String s = roundAtK(min,zec);
			int q = fm.stringWidth(s)/2;
			img.drawString(s,Ox-q,Oy+15);
			s = roundAtK(max,zec);
			q = fm.stringWidth(s)/2;
			img.drawString(s,Ox+rate*r-q,Oy+15);
			s = roundAtK((min+max)/2,zec);
			q = fm.stringWidth(s)/2;
			img.drawString(s,Ox+rate*r/2-q,Oy+15);			
		}	
		//axis
		img.setColor(Color.white);		
		img.drawLine(Ox-10, Oy, Ox+OxLength, Oy);
		img.drawLine(Ox, Oy-3, Ox, Oy+3);
		img.drawLine(Ox+rate*r, Oy-3, Ox+rate*r, Oy+3);	
		img.drawLine(Ox+rate*r/2, Oy-3, Ox+rate*r/2, Oy+3);			
	}		

	public void viewStatistics(boolean status, int index){
		this.status = status;
		this.index = index;
		img.setColor(id.svm.settings.button_color_default);
		img.fillRect(0,0,ww,hh);
		if(status) drawGraph(index);
		repaint();
	}
	
	public static String roundAtK(float f, int k){
		String s = f + "";
		int i = s.indexOf(".");
		if (i+k >= s.length()-1) return s;
		return s.substring(0,i+k+1);
	}
	
	public void itemStateChanged(ItemEvent ie) {		
		JCheckBox cb = (JCheckBox) ie.getSource();
		if(cb == jcbc0) bc0 = cb.isSelected();
		if(cb == jcbc1) bc1 = cb.isSelected();
		viewStatistics(status, index);
	}	

	public void mouseClicked(MouseEvent me) {
		int x = me.getX(), y = me.getY();
	}

	public void mouseEntered(MouseEvent me) {
		int x = me.getX(), y = me.getY();
	}

	public void mouseExited(MouseEvent me) {
		int x = me.getX(), y = me.getY();
	}

	public void mouseMoved(MouseEvent me) {
		int x = me.getX(), y = me.getY();
	} 
		
	public void mousePressed(MouseEvent me) { 
		int x = me.getX(), y = me.getY();
		
	}
			
	public void mouseDragged(MouseEvent me) {
		int x = me.getX(), y = me.getY();
	}

	public void mouseReleased(MouseEvent me) {
		int x = me.getX(), y = me.getY(); 
	}		

}