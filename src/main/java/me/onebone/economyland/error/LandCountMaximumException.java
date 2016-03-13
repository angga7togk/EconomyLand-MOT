package me.onebone.economyland.error;

public class LandCountMaximumException extends LandCreationException{
	private static final long serialVersionUID = 1L;

	private int max;
	
	public LandCountMaximumException(String message, int max){
		super(message);
		
		this.max = max;
	}
	
	public int getMax(){
		return this.max;
	}
}
