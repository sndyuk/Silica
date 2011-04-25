/**
 *    Copyright (C) 2011 sndyuk
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.silica.job;

/**
 * Job例外
 */
public class JobException extends Exception {

	private static final long serialVersionUID = 1912184985511813595L;
	
	public JobException(String s, Exception e) {
		super(s, e);
	}
	
	public JobException(String s) {
		super(s);
	}
	
	@SuppressWarnings("unused")
	private JobException(Exception e) {
	}
}
