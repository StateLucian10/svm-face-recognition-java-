package io;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class StatisticsPanel extends Panel implements ItemListener{
	InputData id;
	Box list;
	StatPanel sp;
	
	public StatisticsPanel(InputData id){
		this.id = id;
		setLayout(null);
	}
	
	public void populatesList(){
		int w = size().width, h = size().height;
		
		removeAll();
		list = Box.createVerticalBox();
		
		JScrollPane jsp = new JScrollPane(list);
		jsp.getViewport().setBackground(id.svm.settings.button_color_default);
		jsp.getViewport().setForeground(id.svm.settings.string_color_default);
		jsp.setBorder(null);
		
		JPanel jp = new JPanel(new BorderLayout());
		jp.setBackground(id.svm.settings.button_color_default);
		
		TitledBorder tb = BorderFactory.createTitledBorder("Attributes");
		tb.setTitleColor(id.svm.settings.string_color_default);
		tb.setTitleJustification(TitledBorder.CENTER);
		
		jp.setBorder(tb);	
		jp.add(jsp);	
		
		if(id.attributes!=null){
			for(int i=0; i<id.attributes.length; i++){
				JCheckBox c = new JCheckBox(id.attributes[i].attribute_name, false);
				c.setAlignmentX(Component.LEFT_ALIGNMENT);
				c.setBackground(id.svm.settings.button_color_default);
				c.setForeground(id.svm.settings.string_color_default);
				c.addItemListener(this);
				list.add(c);
			}	
		}
			
		jp.setBounds(5,5,200,h-10);
		add(jp);
		
		JPanel jps = new JPanel(new BorderLayout());
		jps.setBackground(id.svm.settings.button_color_default);
		
		TitledBorder tbs = BorderFactory.createTitledBorder("Attribute Statistics");
		tbs.setTitleColor(id.svm.settings.string_color_default);
		tbs.setTitleJustification(TitledBorder.CENTER);
		jps.setBorder(tbs);
		
		sp = new StatPanel(id);
		jps.add(sp);
			
		jps.setBounds(210,5,w-216,h-10);
		add(jps);
		
		revalidate();
	}
	
	public void itemStateChanged(ItemEvent ie) {		
		JCheckBox cb = (JCheckBox) ie.getSource();
		boolean status = cb.isSelected();
		Component[] cbs = list.getComponents();
		for(int i=0; i<cbs.length; i++){
			JCheckBox c = (JCheckBox)cbs[i];
			if(c!=cb) c.setSelected(false);
			else sp.viewStatistics(status, i);
		}
		cb.setSelected(status);
	}

}