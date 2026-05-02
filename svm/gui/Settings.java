package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import svm.SVM;

public class Settings extends Dialog{
	
	public Color background_color;
	//public Color background_color_default = new Color(38, 104, 165); 
	public Color background_color_default = new Color(4, 48, 97); 
	
	public boolean axis, axis_default = true;
	public int axis_min, axis_min_default = -100000;
	public int axis_max, axis_max_default = 100000;
	public int axis_min_, axis_max_;
	public Color axis_color, axis_color_default = new Color(255,255,255);
	
	public boolean gradations, gradations_default = true;
	public int axis_gradations, axis_gradations_default = 10;
	
	public boolean grid, grid_default = true;
	public int grid_size, grid_size_default = 50;
	public Color grid_color, grid_color_default = new Color(150,150,150);
	
	public int point_radius, point_radius_default = 3;
	public Color class0_point_color, class0_point_color_default = new Color(255,255,0);
	public Color class1_point_color, class1_point_color_default = new Color(255,0,0);
	
	public Color line_color, line_color_default = new Color(0,255,0);
	
	public Color string_color, string_color_default = new Color(255,255,255);
	
	public Color button_color_default = new Color(96,125,139);
	public Color button_label_default = new Color(255,255,255);
	
	public int percentage_inputData, percentage_inputData_default = 80;
	public float learning_rate, learning_rate_default = 0.1f;

//=====================================================================	
	
	SVM svm;	
	
	Label bkg_color_label, bkg_r_label, bkg_g_label, bkg_b_label;
	TextField bkg_r, bkg_g, bkg_b; 

	Label axis_label, axis_min_label, axis_max_label, axis_color_label, axis_r_label, axis_g_label, axis_b_label; 
	Checkbox axis_cb;
	TextField axis_min_tf, axis_max_tf, axis_r, axis_g, axis_b; 
	
	Label gradations_label, axis_gradations_label; 
	Checkbox gradations_cb;
	TextField axis_gradations_tf; 	
	
	Label grid_label, grid_size_label, grid_color_label, grid_r_label, grid_g_label, grid_b_label;
	Checkbox grid_cb;
	TextField grid_size_tf, grid_r, grid_g, grid_b; 
	
	Label point_radius_label;
	TextField point_radius_tf; 	
	
	Label class0_point_color_label, class0_point_r_label, class0_point_g_label, class0_point_b_label;
	TextField class0_point_r, class0_point_g, class0_point_b; 	

	Label class1_point_color_label, class1_point_r_label, class1_point_g_label, class1_point_b_label;
	TextField class1_point_r, class1_point_g, class1_point_b;	
	
	Label line_color_label, line_r_label, line_g_label, line_b_label;
	TextField line_r, line_g, line_b; 

	Label string_color_label, string_r_label, string_g_label, string_b_label;
	TextField string_r, string_g, string_b; 

	ColorButton cb1,cb2,cb3,cb4,cb5,cb6,cb7;	
	
	Button def;
	 	
	String dir = ".\\", path;
	
	public Settings(SVM svm){
		super(svm, "Settings", true);
		this.svm = svm;
		setResizable(false);
		setBackground(background_color_default);
		setLayout(null);
		
		int y = 50;
		bkg_color_label = new Label("Background color:");
		bkg_color_label.setBounds(25,y,100,20);
		bkg_color_label.setForeground(Color.white);
		add(bkg_color_label);
		bkg_r_label = new Label("R");
		bkg_r_label.setBounds(140,y,20,20);
		bkg_r_label.setForeground(Color.white);
		add(bkg_r_label);		
		bkg_r = new TextField("");
		bkg_r.setBounds(160,y,30,20);
		add(bkg_r);			
		bkg_g_label = new Label("G");
		bkg_g_label.setBounds(200,y,20,20);
		bkg_g_label.setForeground(Color.white);
		add(bkg_g_label);		
		bkg_g = new TextField("");
		bkg_g.setBounds(220,y,30,20);
		add(bkg_g);	
		bkg_b_label = new Label("B");
		bkg_b_label.setBounds(260,y,20,20);
		bkg_b_label.setForeground(Color.white);
		add(bkg_b_label);		
		bkg_b = new TextField("");
		bkg_b.setBounds(280,y,30,20);
		add(bkg_b);	
		cb1 = new ColorButton(this, 1);
		cb1.setBounds(325,y-2,24,24);
		add(cb1);	
		
		y+=30;
		axis_label = new Label("Axis:");
		axis_label.setBounds(25,y,50,20);
		axis_label.setForeground(Color.white);
		add(axis_label);
		axis_cb = new Checkbox("");
		axis_cb.setBounds(160,y,20,20);
		add(axis_cb);		
		
		y+=30;
		axis_min_label = new Label("Axis min:");
		axis_min_label.setBounds(25,y,100,20);
		axis_min_label.setForeground(Color.white);
		add(axis_min_label);
		axis_min_tf = new TextField("");
		axis_min_tf.setBounds(160,y,90,20);
		add(axis_min_tf);		
		
		y+=30;
		axis_max_label = new Label("Axis max:");
		axis_max_label.setBounds(25,y,100,20);
		axis_max_label.setForeground(Color.white);
		add(axis_max_label);
		axis_max_tf = new TextField("");
		axis_max_tf.setBounds(160,y,90,20);
		add(axis_max_tf);			
		
		y+=30;
		axis_color_label = new Label("Axis color:");
		axis_color_label.setBounds(25,y,100,20);
		axis_color_label.setForeground(Color.white);
		add(axis_color_label);
		axis_r_label = new Label("R");
		axis_r_label.setBounds(140,y,20,20);
		axis_r_label.setForeground(Color.white);
		add(axis_r_label);		
		axis_r = new TextField("");
		axis_r.setBounds(160,y,30,20);
		add(axis_r);			
		axis_g_label = new Label("G");
		axis_g_label.setBounds(200,y,20,20);
		axis_g_label.setForeground(Color.white);
		add(axis_g_label);		
		axis_g = new TextField("");
		axis_g.setBounds(220,y,30,20);
		add(axis_g);	
		axis_b_label = new Label("B");
		axis_b_label.setBounds(260,y,20,20);
		axis_b_label.setForeground(Color.white);
		add(axis_b_label);		
		axis_b = new TextField("");
		axis_b.setBounds(280,y,30,20);
		add(axis_b);
		cb2 = new ColorButton(this, 2);
		cb2.setBounds(325,y-2,24,24);
		add(cb2);			
		
		y+=30;
		gradations_label = new Label("Gradations:");
		gradations_label.setBounds(25,y,100,20);
		gradations_label.setForeground(Color.white);
		add(gradations_label);
		gradations_cb = new Checkbox("");
		gradations_cb.setBounds(160,y,20,20);
		add(gradations_cb);			
		
		y+=30;
		axis_gradations_label = new Label("Axis gradations:");
		axis_gradations_label.setBounds(25,y,100,20);
		axis_gradations_label.setForeground(Color.white);
		add(axis_gradations_label);
		axis_gradations_tf = new TextField("");
		axis_gradations_tf.setBounds(160,y,30,20);
		add(axis_gradations_tf);
		
		y+=30;
		grid_label = new Label("Grid:");
		grid_label.setBounds(25,y,50,20);
		grid_label.setForeground(Color.white);
		add(grid_label);
		grid_cb = new Checkbox("");
		grid_cb.setBounds(160,y,20,20);
		add(grid_cb);
		
		y+=30;
		grid_size_label = new Label("Grid size:");
		grid_size_label.setBounds(25,y,100,20);
		grid_size_label.setForeground(Color.white);
		add(grid_size_label);
		grid_size_tf = new TextField("");
		grid_size_tf.setBounds(160,y,30,20);
		add(grid_size_tf);
		
		y+=30;
		grid_color_label = new Label("Grid color:");
		grid_color_label.setBounds(25,y,100,20);
		grid_color_label.setForeground(Color.white);
		add(grid_color_label);
		grid_r_label = new Label("R");
		grid_r_label.setBounds(140,y,20,20);
		grid_r_label.setForeground(Color.white);
		add(grid_r_label);		
		grid_r = new TextField("");
		grid_r.setBounds(160,y,30,20);
		add(grid_r);			
		grid_g_label = new Label("G");
		grid_g_label.setBounds(200,y,20,20);
		grid_g_label.setForeground(Color.white);
		add(grid_g_label);		
		grid_g = new TextField("");
		grid_g.setBounds(220,y,30,20);
		add(grid_g);	
		grid_b_label = new Label("B");
		grid_b_label.setBounds(260,y,20,20);
		grid_b_label.setForeground(Color.white);
		add(grid_b_label);		
		grid_b = new TextField("");
		grid_b.setBounds(280,y,30,20);
		add(grid_b);
		cb3 = new ColorButton(this, 3);
		cb3.setBounds(325,y-2,24,24);
		add(cb3);			
		
		y+=30;
		point_radius_label = new Label("Point radius (<9):");
		point_radius_label.setBounds(25,y,100,20);
		point_radius_label.setForeground(Color.white);
		add(point_radius_label);
		point_radius_tf = new TextField("");
		point_radius_tf.setBounds(160,y,30,20);
		add(point_radius_tf);	
		
		y+=30;
		class0_point_color_label = new Label("Class0 Point color:");
		class0_point_color_label.setBounds(25,y,110,20);
		class0_point_color_label.setForeground(Color.white);
		add(class0_point_color_label);
		class0_point_r_label = new Label("R");
		class0_point_r_label.setBounds(140,y,20,20);
		class0_point_r_label.setForeground(Color.white);
		add(class0_point_r_label);		
		class0_point_r = new TextField("");
		class0_point_r.setBounds(160,y,30,20);
		add(class0_point_r);			
		class0_point_g_label = new Label("G");
		class0_point_g_label.setBounds(200,y,20,20);
		class0_point_g_label.setForeground(Color.white);
		add(class0_point_g_label);		
		class0_point_g = new TextField("");
		class0_point_g.setBounds(220,y,30,20);
		add(class0_point_g);	
		class0_point_b_label = new Label("B");
		class0_point_b_label.setBounds(260,y,20,20);
		class0_point_b_label.setForeground(Color.white);
		add(class0_point_b_label);		
		class0_point_b = new TextField("");
		class0_point_b.setBounds(280,y,30,20);
		add(class0_point_b);
		cb4 = new ColorButton(this, 4);
		cb4.setBounds(325,y-2,24,24);
		add(cb4);			
		
		y+=30;
		class1_point_color_label = new Label("Class1 Point color:");
		class1_point_color_label.setBounds(25,y,110,20);
		class1_point_color_label.setForeground(Color.white);
		add(class1_point_color_label);
		class1_point_r_label = new Label("R");
		class1_point_r_label.setBounds(140,y,20,20);
		class1_point_r_label.setForeground(Color.white);
		add(class1_point_r_label);		
		class1_point_r = new TextField("");
		class1_point_r.setBounds(160,y,30,20);
		add(class1_point_r);			
		class1_point_g_label = new Label("G");
		class1_point_g_label.setBounds(200,y,20,20);
		class1_point_g_label.setForeground(Color.white);
		add(class1_point_g_label);		
		class1_point_g = new TextField("");
		class1_point_g.setBounds(220,y,30,20);
		add(class1_point_g);	
		class1_point_b_label = new Label("B");
		class1_point_b_label.setBounds(260,y,20,20);
		class1_point_b_label.setForeground(Color.white);
		add(class1_point_b_label);		
		class1_point_b = new TextField("");
		class1_point_b.setBounds(280,y,30,20);
		add(class1_point_b);
		cb5 = new ColorButton(this, 5);
		cb5.setBounds(325,y-2,24,24);
		add(cb5);			
		
		y+=30;
		line_color_label = new Label("Line color:");
		line_color_label.setBounds(25,y,110,20);
		line_color_label.setForeground(Color.white);
		add(line_color_label);
		line_r_label = new Label("R");
		line_r_label.setBounds(140,y,20,20);
		line_r_label.setForeground(Color.white);
		add(line_r_label);		
		line_r = new TextField("");
		line_r.setBounds(160,y,30,20);
		add(line_r);			
		line_g_label = new Label("G");
		line_g_label.setBounds(200,y,20,20);
		line_g_label.setForeground(Color.white);
		add(line_g_label);		
		line_g = new TextField("");
		line_g.setBounds(220,y,30,20);
		add(line_g);	
		line_b_label = new Label("B");
		line_b_label.setBounds(260,y,20,20);
		line_b_label.setForeground(Color.white);
		add(line_b_label);		
		line_b = new TextField("");
		line_b.setBounds(280,y,30,20);
		add(line_b);
		cb6 = new ColorButton(this, 6);
		cb6.setBounds(325,y-2,24,24);
		add(cb6);	
		
		y+=30;
		string_color_label = new Label("String color:");
		string_color_label.setBounds(25,y,110,20);
		string_color_label.setForeground(Color.white);
		add(string_color_label);
		string_r_label = new Label("R");
		string_r_label.setBounds(140,y,20,20);
		string_r_label.setForeground(Color.white);
		add(string_r_label);		
		string_r = new TextField("");
		string_r.setBounds(160,y,30,20);
		add(string_r);			
		string_g_label = new Label("G");
		string_g_label.setBounds(200,y,20,20);
		string_g_label.setForeground(Color.white);
		add(string_g_label);		
		string_g = new TextField("");
		string_g.setBounds(220,y,30,20);
		add(string_g);	
		string_b_label = new Label("B");
		string_b_label.setBounds(260,y,20,20);
		string_b_label.setForeground(Color.white);
		add(string_b_label);		
		string_b = new TextField("");
		string_b.setBounds(280,y,30,20);
		add(string_b);	
		cb7 = new ColorButton(this, 7);
		cb7.setBounds(325,y-2,24,24);
		add(cb7);			
		
		y+=60;
		def = new Button("Default Settings");
		def.setBounds(25,y,324,30);
		def.setBackground(button_color_default);
		def.setForeground(button_label_default);		
		add(def);
		
		loadDefaultSettings();
		loadSettings();
		
	}
	
	public void loadDefaultSettings(){
		background_color = background_color_default; 
		axis = axis_default;
		axis_min = axis_min_default;
		axis_max = axis_max_default;
		gradations = gradations_default;
		axis_gradations = axis_gradations_default;
		axis_color = axis_color_default;
		grid = grid_default;
		grid_size = grid_size_default;
		grid_color = grid_color_default;	
		point_radius = point_radius_default;
		class0_point_color = class0_point_color_default;
		class1_point_color = class1_point_color_default;	
		line_color = line_color_default;
		string_color = string_color_default;
		percentage_inputData = percentage_inputData_default;
		learning_rate = learning_rate_default;
	
		bkg_r.setText(""+background_color.getRed());
		bkg_g.setText(""+background_color.getGreen());
		bkg_b.setText(""+background_color.getBlue());		
		axis_cb.setState(axis);
		axis_min_tf.setText(""+axis_min);
		axis_max_tf.setText(""+axis_max);
		axis_r.setText(""+axis_color.getRed());
		axis_g.setText(""+axis_color.getGreen());
		axis_b.setText(""+axis_color.getBlue());
		gradations_cb.setState(gradations);
		axis_gradations_tf.setText(""+axis_gradations);
		grid_cb.setState(grid);
		grid_size_tf.setText(""+grid_size);
		grid_r.setText(""+grid_color.getRed());
		grid_g.setText(""+grid_color.getGreen());
		grid_b.setText(""+grid_color.getBlue());			
		point_radius_tf.setText(""+point_radius);
		class0_point_r.setText(""+class0_point_color.getRed());
		class0_point_g.setText(""+class0_point_color.getGreen());
		class0_point_b.setText(""+class0_point_color.getBlue());		
		class1_point_r.setText(""+class1_point_color.getRed());
		class1_point_g.setText(""+class1_point_color.getGreen());
		class1_point_b.setText(""+class1_point_color.getBlue());
		line_r.setText(""+line_color.getRed());
		line_g.setText(""+line_color.getGreen());
		line_b.setText(""+line_color.getBlue());				
		string_r.setText(""+string_color.getRed());
		string_g.setText(""+string_color.getGreen());
		string_b.setText(""+string_color.getBlue());		
	}
	
	public void loadSettings(){
		try {
			File file = new File("svm/settings");
			if(file.exists()){
				DataInputStream dis = new DataInputStream(new FileInputStream(file));
				String line = "";
				while ((line = dis.readLine()) != null){
					if(line.startsWith("background_color=")){
						try{
							String[] c = line.split("=")[1].split(" ");
							int r = Integer.parseInt(c[0]);
							int g = Integer.parseInt(c[1]);
							int b = Integer.parseInt(c[2]);
							if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
								background_color = new Color(r,g,b);
							continue;
						}
						catch(NumberFormatException e){return;}
					}					
					if(line.startsWith("axis=")){
						try{
							int a = Integer.parseInt(line.split("=")[1]);
							if(a==0 || a==1) axis = a==1;
							continue;
						}
						catch(NumberFormatException e){return;}
					}
					if(line.startsWith("axis_min=")){
						try{
							axis_min_ = Integer.parseInt(line.split("=")[1]);
							continue;
						}
						catch(NumberFormatException e){return;}
					}	
					if(line.startsWith("axis_max=")){
						try{
							axis_max_ = Integer.parseInt(line.split("=")[1]);
							continue;
						}
						catch(NumberFormatException e){return;}
					}	
					if(axis_min_ < axis_max_){
						axis_min = axis_min_;
						axis_max = axis_max_;
						axis_min_ = axis_max_;
						continue;
					}
					if(line.startsWith("axis_color=")){
						try{
							String[] c = line.split("=")[1].split(" ");
							int r = Integer.parseInt(c[0]);
							int g = Integer.parseInt(c[1]);
							int b = Integer.parseInt(c[2]);
							if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
								axis_color = new Color(r,g,b);
							continue;
						}
						catch(NumberFormatException e){return;}
					}						
					if(line.startsWith("gradations=")){
						try{
							int a = Integer.parseInt(line.split("=")[1]);
							if(a==0 || a==1) gradations = a==1;
							continue;
						}
						catch(NumberFormatException e){return;}
					}					
					if(line.startsWith("axis_gradations=")){
						try{
							int a = Integer.parseInt(line.split("=")[1]);
							if(a>0) axis_gradations = a;
							continue;
						}
						catch(NumberFormatException e){return;}
					}
					if(line.startsWith("grid=")){
						try{
							int a = Integer.parseInt(line.split("=")[1]);
							if(a==0 || a==1) grid = a==1;
							continue;
						}
						catch(NumberFormatException e){return;}
					}	
					if(line.startsWith("grid_size=")){
						try{
							int a = Integer.parseInt(line.split("=")[1]);
							if(a>0) grid_size = a;
							continue;
						}
						catch(NumberFormatException e){return;}
					}
					if(line.startsWith("grid_color=")){
						try{
							String[] c = line.split("=")[1].split(" ");
							int r = Integer.parseInt(c[0]);
							int g = Integer.parseInt(c[1]);
							int b = Integer.parseInt(c[2]);
							if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
								grid_color = new Color(r,g,b);
							continue;
						}
						catch(NumberFormatException e){return;}
					}	
					if(line.startsWith("point_radius=")){
						try{
							int a = Integer.parseInt(line.split("=")[1]);
							if(0<a && a<9) point_radius = a;
							continue;
						}
						catch(NumberFormatException e){return;}
					}					
					if(line.startsWith("class0_point_color=")){
						try{
							String[] c = line.split("=")[1].split(" ");
							int r = Integer.parseInt(c[0]);
							int g = Integer.parseInt(c[1]);
							int b = Integer.parseInt(c[2]);
							if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
								class0_point_color = new Color(r,g,b);
							continue;
						}
						catch(NumberFormatException e){return;}
					}					
					if(line.startsWith("class1_point_color=")){
						try{
							String[] c = line.split("=")[1].split(" ");
							int r = Integer.parseInt(c[0]);
							int g = Integer.parseInt(c[1]);
							int b = Integer.parseInt(c[2]);
							if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
								class1_point_color = new Color(r,g,b);
							continue;
						}
						catch(NumberFormatException e){return;}
					}							
					if(line.startsWith("line_color=")){
						try{
							String[] c = line.split("=")[1].split(" ");
							int r = Integer.parseInt(c[0]);
							int g = Integer.parseInt(c[1]);
							int b = Integer.parseInt(c[2]);
							if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
								line_color = new Color(r,g,b);
							continue;
						}
						catch(NumberFormatException e){return;}
					}	
					if(line.startsWith("string_color=")){
						try{
							String[] c = line.split("=")[1].split(" ");
							int r = Integer.parseInt(c[0]);
							int g = Integer.parseInt(c[1]);
							int b = Integer.parseInt(c[2]);
							if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
								string_color = new Color(r,g,b);
							continue;
						}
						catch(NumberFormatException e){return;}
					}
					if(line.startsWith("percentage=")){
						try{
							percentage_inputData = Integer.parseInt(line.split("=")[1]);
							continue;
						}
						catch(NumberFormatException e){return;}
					}
					if(line.startsWith("learning_rate=")){
						try{
							learning_rate = Float.parseFloat(line.split("=")[1]);
							continue;
						}
						catch(NumberFormatException e){return;}
					}					
					

				}
				dis.close();
			}
		} 
		catch (IOException e) {}	
		
		bkg_r.setText(""+background_color.getRed());
		bkg_g.setText(""+background_color.getGreen());
		bkg_b.setText(""+background_color.getBlue());		
		axis_cb.setState(axis);
		axis_min_tf.setText(""+axis_min);
		axis_max_tf.setText(""+axis_max);
		axis_r.setText(""+axis_color.getRed());
		axis_g.setText(""+axis_color.getGreen());
		axis_b.setText(""+axis_color.getBlue());
		gradations_cb.setState(gradations);
		axis_gradations_tf.setText(""+axis_gradations);
		grid_cb.setState(grid);
		grid_size_tf.setText(""+grid_size);
		grid_r.setText(""+grid_color.getRed());
		grid_g.setText(""+grid_color.getGreen());
		grid_b.setText(""+grid_color.getBlue());			
		point_radius_tf.setText(""+point_radius);
		class0_point_r.setText(""+class0_point_color.getRed());
		class0_point_g.setText(""+class0_point_color.getGreen());
		class0_point_b.setText(""+class0_point_color.getBlue());		
		class1_point_r.setText(""+class1_point_color.getRed());
		class1_point_g.setText(""+class1_point_color.getGreen());
		class1_point_b.setText(""+class1_point_color.getBlue());
		line_r.setText(""+line_color.getRed());
		line_g.setText(""+line_color.getGreen());
		line_b.setText(""+line_color.getBlue());				
		string_r.setText(""+string_color.getRed());
		string_g.setText(""+string_color.getGreen());
		string_b.setText(""+string_color.getBlue());			
	}
	
	void saveSettings(){
		try{
			int r = Integer.parseInt(bkg_r.getText());
			int g = Integer.parseInt(bkg_g.getText());
			int b = Integer.parseInt(bkg_b.getText());
			if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
				background_color = new Color(r,g,b);
		}
		catch(NumberFormatException e){}		
		axis = axis_cb.getState();
		try{axis_min_ = Integer.parseInt(axis_min_tf.getText());}
		catch(NumberFormatException e){}
		try{axis_max_ = Integer.parseInt(axis_max_tf.getText());}
		catch(NumberFormatException e){}	
		if(axis_min_ < axis_max_){
			axis_min = axis_min_;
			axis_max = axis_max_;
			axis_min_ = axis_max_;
		}			
		try{
			int r = Integer.parseInt(axis_r.getText());
			int g = Integer.parseInt(axis_g.getText());
			int b = Integer.parseInt(axis_b.getText());
			if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
				axis_color = new Color(r,g,b);
		}
		catch(NumberFormatException e){}
		gradations = gradations_cb.getState();
		try{
			int a = Integer.parseInt(axis_gradations_tf.getText());
			if(a>0) axis_gradations = a;
		}
		catch(NumberFormatException e){}	
		grid = grid_cb.getState();	
		try{
			int a = Integer.parseInt(grid_size_tf.getText());
			if(a>0) grid_size = a;
		}
		catch(NumberFormatException e){}	
		try{
			int r = Integer.parseInt(grid_r.getText());
			int g = Integer.parseInt(grid_g.getText());
			int b = Integer.parseInt(grid_b.getText());
			if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
				grid_color = new Color(r,g,b);
		}
		catch(NumberFormatException e){}
		try{
			int a = Integer.parseInt(point_radius_tf.getText());
			if(0<a && a<9) point_radius = a;
		}
		catch(NumberFormatException e){}	
		try{
			int r = Integer.parseInt(class0_point_r.getText());
			int g = Integer.parseInt(class0_point_g.getText());
			int b = Integer.parseInt(class0_point_b.getText());
			if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255){
				class0_point_color = new Color(r,g,b);
				if(svm.ind.classes != null) svm.ind.classes[0].color = class0_point_color;
			}
		}
		catch(NumberFormatException e){}
		try{
			int r = Integer.parseInt(class1_point_r.getText());
			int g = Integer.parseInt(class1_point_g.getText());
			int b = Integer.parseInt(class1_point_b.getText());
			if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255){
				class1_point_color = new Color(r,g,b);
				if(svm.ind.classes != null) svm.ind.classes[1].color = class1_point_color;
			}
		}
		catch(NumberFormatException e){}	
		try{
			int r = Integer.parseInt(line_r.getText());
			int g = Integer.parseInt(line_g.getText());
			int b = Integer.parseInt(line_b.getText());
			if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
				line_color = new Color(r,g,b);
		}
		catch(NumberFormatException e){}
		try{
			int r = Integer.parseInt(string_r.getText());
			int g = Integer.parseInt(string_g.getText());
			int b = Integer.parseInt(string_b.getText());
			if(0<=r && r<=255 && 0<=g && g<=255 && 0<=b && b<=255)
				string_color = new Color(r,g,b);
		}
		catch(NumberFormatException e){}

		BufferedWriter bw = null;
		File file = new File("svm/settings");
		if(file.exists()) file.delete();
		try{
			bw = new BufferedWriter(new FileWriter(file));
			bw.write("background_color="+background_color.getRed()+" "+background_color.getGreen()+" "+background_color.getBlue()+"\n");
			bw.write("axis="+(axis?1:0)+"\n");
			bw.write("axis_min="+axis_min+"\n");
			bw.write("axis_max="+axis_max+"\n");
			bw.write("axis_color="+axis_color.getRed()+" "+axis_color.getGreen()+" "+axis_color.getBlue()+"\n");
			bw.write("gradations="+(gradations?1:0)+"\n");
			bw.write("axis_gradations="+axis_gradations+"\n");	
			bw.write("grid="+(grid?1:0)+"\n");
			bw.write("grid_size="+grid_size+"\n");
			bw.write("grid_color="+grid_color.getRed()+" "+grid_color.getGreen()+" "+grid_color.getBlue()+"\n");
			bw.write("point_radius="+point_radius+"\n");
			bw.write("class0_point_color="+class0_point_color.getRed()+" "+class0_point_color.getGreen()+" "+class0_point_color.getBlue()+"\n");
			bw.write("class1_point_color="+class1_point_color.getRed()+" "+class1_point_color.getGreen()+" "+class1_point_color.getBlue()+"\n");
			bw.write("line_color="+line_color.getRed()+" "+line_color.getGreen()+" "+line_color.getBlue()+"\n");
			bw.write("string_color="+string_color.getRed()+" "+string_color.getGreen()+" "+string_color.getBlue()+"\n");
			bw.write("percentage="+percentage_inputData+"\n");
			bw.write("learning_rate="+learning_rate+"\n");
			
			bw.close();
		}
		catch(IOException e){e.printStackTrace();}
	
		svm.setBackground(background_color);	
		svm.design.repaint();		
	}
	
	public boolean handleEvent(Event e){
		if(e.id==Event.WINDOW_DESTROY){
			saveSettings();
			dispose();
		}else if(e.id==Event.ACTION_EVENT && e.target == def){
			loadDefaultSettings();
           	return true;			
		}else return false;	
		return super.handleEvent(e);
	}	
	
}