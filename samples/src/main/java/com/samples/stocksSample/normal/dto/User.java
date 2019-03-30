package com.samples.stocksSample.normal.dto;

public class User {
	private Long id;
	private String name;
	private String userName;
	public Long getId() {
		return id;
	}
	public User setId(Long id) {
		this.id = id;
		return this;
	}
	public String getName() {
		return name;
	}
	public User setName(String name) {
		this.name = name;
		return this;
	}
	public String getUserName() {
		return userName;
	}
	public User setUserName(String userName) {
		this.userName = userName;
		return this;
	}
	
}
