package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import svm.SVM;
import alg.*;

public class Options extends Dialog implements AdjustmentListener{
	SVM svm;
	public Scrollbar sb;
	Label sb_label1, sb_label2, sb_label3, sb_label4;
	Label eta_label;
	TextField eta_tf; 	

	public Options(SVM svm){
		super(svm, "Learning Options", true);
		this.svm = svm;
		setBackground(svm.settings.background_color_default);
		setForeground(svm.settings.string_color_default);
		setResizable(false);		
		resize(640,480);
		move((svm.res.width-640)/2,(svm.res.height-480)/2);				
		setLayout(null);
		
		int y = 50;
		sb_label1 = new Label("Percentage of Input Data:");
		sb_label1.setBounds(25,y,150,20);
		sb_label1.setForeground(Color.white);
		add(sb_label1);		
		
		sb = new Scrollbar(Scrollbar.HORIZONTAL, 80, 1, 20, 101);
		sb.setBounds(200,y,150,20);
		sb.setBackground(svm.settings.button_color_default);
		sb.setForeground(svm.settings.button_label_default);
		sb.addAdjustmentListener(this);
		add(sb);

		sb_label2 = new Label("");
		sb_label2.setBounds(360,y,150,20);
		sb_label2.setForeground(Color.white);
		add(sb_label2);	
		
		y+=30;
		sb_label3 = new Label("");
		sb_label3.setBounds(25,y,1000,20);
		sb_label3.setForeground(Color.white);
		add(sb_label3);
		
		y+=30;
		sb_label4 = new Label("The change of percentage will only be valid on new Load Input Data.");
		sb_label4.setBounds(25,y,1000,20);
		sb_label4.setForeground(Color.white);
		add(sb_label4);		

		y+=30;
		eta_label = new Label("Learning Rate \u03B7 :");
		eta_label.setBounds(25,y,100,20);
		eta_label.setForeground(Color.white);
		add(eta_label);
		eta_tf = new TextField("");
		eta_tf.setForeground(Color.black);
		eta_tf.setBounds(200,y,90,20);
		add(eta_tf);
		eta_tf.setText(""+svm.settings.learning_rate);
		
	}
	
	public void setValue(){
		if(10<=svm.settings.percentage_inputData && svm.settings.percentage_inputData<=100){
			//sb_label2.setText(sb.getValue()+"%");
			sb.setValue(svm.settings.percentage_inputData);
		}else{
			svm.settings.percentage_inputData = 80;
			svm.settings.saveSettings();
		}
		String s = sb.getValue() + "% of the input data will be used in learning, and " + (100-sb.getValue()) + "% will be used for classifier testing.";
		sb_label3.setText(s);
	}
	
	public boolean handleEvent(Event e){
		if(e.id==Event.WINDOW_DESTROY){
			svm.settings.percentage_inputData = sb.getValue();
			float learning_rate = svm.settings.learning_rate_default;
			try{
				learning_rate = Float.parseFloat(eta_tf.getText());
			}
			catch(NumberFormatException nfe){}
			svm.settings.learning_rate = learning_rate;
			svm.settings.saveSettings();			
			dispose();						
		}	
		return super.handleEvent(e);
	}	

	public void adjustmentValueChanged(AdjustmentEvent e){
		sb_label2.setText(sb.getValue()+"%");
		String s = sb.getValue() + "% of the input data will be used in learning, and " + (100-sb.getValue()) + "% will be used for classifier testing.";
		sb_label3.setText(s);
	}
	
}