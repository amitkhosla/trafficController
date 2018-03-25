package com.samples.sample1.annotatedFlow;

import java.util.List;

public class Person {
	List<Person> friends;
	List<Person> relatives;
	List<Person> colleagues;
	Long id;
	public Long getId() {
		return id;
	}
	public Person setId(Long id) {
		this.id = id;
		return this;
	}
	String name;
	Long age;
	String qualification;
	
	public String getName() {
		return name;
	}
	public Person setName(String name) {
		this.name = name;
		return this;
	}
	public Long getAge() {
		return age;
	}
	public Person setAge(Long age) {
		this.age = age;
		return this;
	}
	public String getQualification() {
		return qualification;
	}
	public Person setQualification(String qualification) {
		this.qualification = qualification;
		return this;
	}
	public List<Person> getFriends() {
		return friends;
	}
	public Person setFriends(List<Person> friends) {
		this.friends = friends;
		return this;
	}
	public List<Person> getRelatives() {
		return relatives;
	}
	public Person setRelatives(List<Person> relatives) {
		this.relatives = relatives;
		return this;
	}
	public List<Person> getColleagues() {
		return colleagues;
	}
	public Person setColleagues(List<Person> colleagues) {
		this.colleagues = colleagues;
		return this;
	}
	
}
