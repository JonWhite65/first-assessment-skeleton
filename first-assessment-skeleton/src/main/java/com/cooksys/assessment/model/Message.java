package com.cooksys.assessment.model;

public class Message {

	private String username;
	private String command;
	private String contents;
	//Attempt to distinguish between private messages and public messages
	private String type;
	private String time;
	private String[] userList;
	//Getters and setter methods for type variable
	
	public String[] getUserList() {
		return userList;
	}

	public void setUserList(String[] userList) {
		this.userList = userList;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	

	

}
