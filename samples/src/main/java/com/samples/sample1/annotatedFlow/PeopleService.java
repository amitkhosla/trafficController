package com.samples.sample1.annotatedFlow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.util.StringUtils;

@Named
public class PeopleService {
	@Inject
	ReadService readService;
	
	@Inject
	WriteService writeService;
	
	public void savePerson(Person person) throws IOException {
		Map<Long, Boolean> writtenPeople = new ConcurrentHashMap<>();
		writePersonChain(person, writtenPeople, true);
	}

	private void writePersonChain(Person person, Map<Long, Boolean> writtenPeople, boolean update)
			throws IOException {
		if (writtenPeople.containsKey(person.getId())) {
			return;
		}
		if (!update) {
			if (readService.hasPerson(person.getId())) {
				return;
			}
		}
		Map<String, String> data = new ConcurrentHashMap<>();
		writtenPeople.put(person.getId(), true);
		data.put("name", person.getName());
		data.put("age", person.getAge().toString());
		data.put("qualification", person.getQualification());
		addRelatives(data, person.getRelatives(), writtenPeople);
		addFriends(data, person.getFriends(), writtenPeople);
		addColleagues(data, person.getColleagues(), writtenPeople);
		writeService.writeInFile(person.getId(), data);
	}
	
	private void addColleagues(Map<String, String> data, List<Person> colleagues, Map<Long, Boolean> writtenPeople) throws IOException {
		data.put("colleagues", getIdsFromList(colleagues, writtenPeople));
	}

	private String getIdsFromList(List<Person> people, Map<Long, Boolean> writtenPeople) throws IOException {
		StringBuilder sb = new StringBuilder("");
		int size = people.size();
		if (size > 0) {
			for (int i =0; i<size-1;i++) {
				Person person = people.get(i);
				sb.append(person.getId()).append(",");
				writePersonChain(person, writtenPeople, false);
			}
			Person person = people.get(size-1);
			sb.append(person.getId());
			writePersonChain(person, writtenPeople, false);
		}
		return sb.toString();
	}

	private void addFriends(Map<String, String> data, List<Person> friends, Map<Long, Boolean> writtenPeople) throws IOException {
		data.put("friends", getIdsFromList(friends, writtenPeople));
	}

	private void addRelatives(Map<String, String> data, List<Person> relatives, Map<Long, Boolean> writtenPeople) throws IOException {
		data.put("relatives", getIdsFromList(relatives, writtenPeople));
	}

	public Person getPerson(Long id) throws IOException {
		Map<Long, Person> peopleExtracted = new ConcurrentHashMap<>();
		Person p = getPerson(id, peopleExtracted);
		return p;
	}
	
	private Person getPerson(Long id, Map<Long, Person> peopleExtracted) throws IOException {
		Person person = peopleExtracted.get(id);
		if (person!= null) {
			return person;
		}
		Person p = new Person().setId(id);
		peopleExtracted.put(id, p);
		Map<String, String> personDetails = readService.getData(id);
		fillBasicDetails(id, p, personDetails);
		fillFriends(p, personDetails, peopleExtracted);
		fillColleagues(p, personDetails, peopleExtracted);
		fillRelatives(p, personDetails, peopleExtracted);
		return p;
	}
	private void fillRelatives(Person p, Map<String, String> personDetails, Map<Long, Person> peopleExtracted) throws NumberFormatException, IOException {
		p.setRelatives(extractPeople(personDetails.get("relatives"), peopleExtracted));
	}
	
	protected List<Person> extractPeople(String peopleString, Map<Long, Person> peopleExtracted) throws NumberFormatException, IOException {
		List<Person> people = new ArrayList<>();
		String[] peopleIds = peopleString.split(",");
		for (String personId : peopleIds) {
			if (StringUtils.isEmpty(personId)) {
				continue;
			}
			people.add(getPerson(Long.valueOf(personId), peopleExtracted));
		}
		return people;
	}
	private void fillColleagues(Person p, Map<String, String> personDetails, Map<Long, Person> peopleExtracted) throws NumberFormatException, IOException {
		p.setColleagues(extractPeople(personDetails.get("colleagues"), peopleExtracted));
	}
	private void fillFriends(Person p, Map<String, String> personDetails, Map<Long, Person> peopleExtracted) throws NumberFormatException, IOException {
		p.setFriends(extractPeople(personDetails.get("friends"), peopleExtracted));
	}
	protected Person fillBasicDetails(Long id, Person p, Map<String, String> personDetails) {
		return p.setName(personDetails.get("name"))
			.setAge(Long.valueOf(personDetails.get("age")))
			.setQualification(personDetails.get("qualification"));
	}
}
