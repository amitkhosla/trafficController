package com.samples.sample1.normalFlow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import com.samples.RandomGenerator;

@Named
public class CreatePerson {
	public Person generateMockPersonChain(Long id, Long maxId) {
		Map<Long, Person> peopleCreatedTillNow = new HashMap<>();
		return createChain(peopleCreatedTillNow, id, maxId);
	}

	private Person createChain(Map<Long, Person> peopleCreatedTillNow, Long id, Long maxId) {
		// TODO Auto-generated method stub
		Person pInMap = peopleCreatedTillNow.get(id);
		if (pInMap != null) {
			return pInMap;
		}
		Person p = new Person().setId(id);
		peopleCreatedTillNow.put(id, p);
		p.setAge(RandomGenerator.getRandomLong(100l))
			.setName(RandomGenerator.getRandomString(30))
			.setQualification(RandomGenerator.getRandomString(50));
		p.setColleagues(getListOfPeople(peopleCreatedTillNow, id, maxId));
		p.setRelatives(getListOfPeople(peopleCreatedTillNow, id, maxId));
		p.setFriends(getListOfPeople(peopleCreatedTillNow, id, maxId));
		return p;
	}

	private List<Person> getListOfPeople(Map<Long, Person> peopleCreatedTillNow, Long id, Long maxId) {
		int numberOfPeopleInCollection = RandomGenerator.getRandomInteger(maxId.intValue()/2);
		List<Person> people = new ArrayList<>();
		for (int i=0;i<numberOfPeopleInCollection;i++) {
			Long pId = RandomGenerator.getRandomLong(maxId);
			if (pId.equals(id)) {
				continue;
			}
			people.add(createChain(peopleCreatedTillNow, pId, maxId));
		}
		return people;
	}
}
