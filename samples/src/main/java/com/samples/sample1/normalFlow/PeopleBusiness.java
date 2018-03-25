package com.samples.sample1.normalFlow;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PeopleBusiness {

	@Inject
	CreatePerson creater;
	
	@Inject
	PeopleService service;
	
	public void createPeople(Long maxSize) throws IOException {
		for (Long l=0l;l<maxSize;l++) {
			Person person = creater.generateMockPersonChain(l, maxSize);
			service.savePerson(person);
		}
	}
	
	Map<Long, Person> map = new ConcurrentHashMap<>();
	
	public void buildGraph(Long max) throws IOException {
		for (Long pid = 0l; pid < max; pid++) {
			if (map.containsKey(pid)) {
				continue;
			} 
			Person person = service.getPerson(pid);
			map.put(person.getId(), person);
			for (Person p : person.getColleagues()) {
				map.putIfAbsent(p.getId(), p);
			}
			for (Person p : person.getFriends()) {
				map.putIfAbsent(p.getId(), p);
			}
			for (Person p : person.getRelatives()) {
				map.putIfAbsent(p.getId(), p);
			}
		}
	}
	
	public String retrieveGraph(Long id) throws IOException {
		Map<Long, Person> peopleMappedTilleNow = new HashMap<>();
		String output = getPersonDetails(id, peopleMappedTilleNow);
		return output;
	}

	private String getPersonDetails(Long id, Map<Long, Person> peopleMappedTilleNow) throws IOException {
		if (peopleMappedTilleNow.containsKey(id)) {
			return "{\"ID\":"+id + "}";
		}
		Person p = service.getPerson(id);
		peopleMappedTilleNow.put(id, p);
		StringBuilder sb = new StringBuilder();
		sb.append("{");
			sb.append("\"ID\":").append(p.getId()).append(",");
			sb.append("\"NAME\":\"").append(p.getName()).append("\",");
			sb.append("\"QUALIFICATION\":\"").append(p.getQualification()).append("\",");
			sb.append(getDetailsOfMapping("Friends",peopleMappedTilleNow, p.getFriends()));
			sb.append(getDetailsOfMapping("Relatives",peopleMappedTilleNow, p.getRelatives()));
			sb.append(getDetailsOfMapping("Colleagues",peopleMappedTilleNow, p.getColleagues()));
		sb.append("}");
		String output = sb.toString();
		return output;
	}

	private String getDetailsOfMapping(String relation, Map<Long, Person> peopleMappedTilleNow, List<Person> peopleMappingList) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("\"").append(relation).append("\":[");
			for (Person person : peopleMappingList) {
				sb.append(getPersonDetails(person.getId(), peopleMappedTilleNow)).append(",");
			}
			removeCharAtEnd(sb);
		sb.append("]");
		return sb.toString();
	}

	private void removeCharAtEnd(StringBuilder sb) {
		// TODO Auto-generated method stub
		int length = sb.length();
		if (sb.charAt(length-1) == ',') {
			sb.setLength(length-1);
		}
	}
	
	
}
