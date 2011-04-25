package com.silica.job;

public class TestJob implements Job<String> {

	private static final long serialVersionUID = 7928128350315086340L;

	@Override
	public String execute() throws JobException {
		return "success";
	}
}