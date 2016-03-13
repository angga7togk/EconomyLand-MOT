package me.onebone.economyland.error;

import me.onebone.economyland.Land;

public class LandOverlapException extends LandCreationException{
	private static final long serialVersionUID = 1L;
	
	private Land land;

	public LandOverlapException(String message, Land land){
		super(message);
		
		this.land = land;
	}
	
	public Land overlappingWith(){
		return this.land;
	}
}
