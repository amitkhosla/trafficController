package com.samples.sample1.normalFlow;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ReadService {
	@Inject
	ReadFile reader;
	
	public Map<String, String> getData(Long id) throws IOException {
		Map<String, String> map = new HashMap<>();
		List<String> data = reader.readFile(id + ".txt");
		for (String str : data) {
			String[] arr = str.split("=");
			if (arr.length == 1) {
				map.put(arr[0], "");
			} else {
				map.put(arr[0], arr[1]);
			}
		}
		return map;
	}

	public boolean hasPerson(Long id) {
		return reader.hasFile(id + ".txt");
	}
}
