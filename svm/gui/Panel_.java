package gui;

import java.awt.*;

public class Panel_ extends Panel {
    public Image im, im1;   

    public Panel_(Image im) {this.im = im;}
    
    public Panel_() {}

    public void update(Graphics g) {paint(g);}

    public void paint(Graphics g) {
            super.paint(g);
            Dimension dimension = size();
            im1 = createImage(dimension.width, dimension.height);
            pan(im1.getGraphics());
            g.drawImage(im1, 0, 0, this);
    }

    public void pan(Graphics g) {
    	Dimension dimension = size();
    	int w = dimension.width;
    	int h = dimension.height;       
        Color color = getBackground();
        g.setColor(color);
        g.fillRect(0, 0, w, h);   
        if(im!=null){             
	        for(int k = 0; k < w; k += im.getWidth(this))
	              	for(int l = 0; l < h; l += im.getHeight(this)) 
	                 	g.drawImage(im, k, l, this);
        }
        g.setColor(color.brighter());
        g.drawRect(1, 1, w - 2, h - 2);
        g.setColor(color.darker());
        g.drawRect(0, 0, w - 2, h - 2);
    }
}