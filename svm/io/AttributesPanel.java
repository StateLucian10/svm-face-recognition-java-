package io;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class AttributesPanel extends Panel implements ItemListener{
	InputData id;
	String title = "Available attributes in classification";
	Box list;
	
	public AttributesPanel(InputData id){
		this.id = id;
		setLayout(null);
	}
	
	public void populatesList(){
		removeAll();
		list = Box.createVerticalBox();
		
		JScrollPane jsp = new JScrollPane(list);
		jsp.getViewport().setBackground(id.svm.settings.button_color_default);
		jsp.getViewport().setForeground(id.svm.settings.string_color_default);
		jsp.setBorder(null);
		
		JPanel jp = new JPanel(new BorderLayout());
		jp.setBackground(id.svm.settings.button_color_default);
		
		TitledBorder tb = BorderFactory.createTitledBorder(title);
		tb.setTitleColor(id.svm.settings.string_color_default);
		tb.setTitleJustification(TitledBorder.CENTER);
		
		jp.setBorder(tb);	
		jp.add(jsp);	

		for(int i=0; i<id.attributes.length; i++){
			JCheckBox c = new JCheckBox(id.attributes[i].attribute_name, true);
			c.setSelected(id.attributes[i].available);
			c.setAlignmentX(Component.LEFT_ALIGNMENT);
			c.setBackground(id.svm.settings.button_color_default);
			c.setForeground(id.svm.settings.string_color_default);
			c.addItemListener(this);
			list.add(c);
		}			

		int h = size().height;
		jp.setBounds(5,5,220,h-10);		
		add(jp);
		revalidate();
	}
	
	public void itemStateChanged(ItemEvent ie) {		
		JCheckBox cb = (JCheckBox) ie.getSource();
		setAttributeStatus(cb.getText(), cb.isSelected());
	}
	
	public void setAttributeStatus(String attribute_name, boolean available){
		for(int i=0; i<id.attributes.length; i++){
			if(id.attributes[i].attribute_name.startsWith(attribute_name)){
				id.attributes[i].available = available;
				break;
			}	
		}
	}
	

}