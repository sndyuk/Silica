package com.silica.rpc.pipe;

public class PipeException extends Exception {

	private static final long serialVersionUID = -6490607896125043064L;

	public PipeException(String s, Exception e) {
		super(s, e);
	}
	
	public PipeException(String s) {
		super(s);
	}
	
	public PipeException(Exception e) {
		super(e);
	}
}
