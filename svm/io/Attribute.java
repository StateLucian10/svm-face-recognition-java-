package io;

public class Attribute{
	public String attribute_name = "";
	public String attribute_type = "float";
	public boolean available = true;
	public Statistic[] class_statistic;
	public Statistic attr_statistic;
	
	public Attribute(String attribute_name){
		this.attribute_name = attribute_name;
	}
	
	public Attribute(String attribute_name, String attribute_type){
		this(attribute_name);
		this.attribute_type = attribute_type;
	}	
	
	public Attribute(String attribute_name, String attribute_type, boolean available){
		this(attribute_name, attribute_type);
		this.available = available;
	}		
	
	public boolean isAvailable(){return available;}
	
}