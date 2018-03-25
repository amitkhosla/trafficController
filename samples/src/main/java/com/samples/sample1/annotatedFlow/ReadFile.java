package com.samples.sample1.annotatedFlow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.inject.Named;

@Named
public class ReadFile {
	public List<String> readFile(String path) throws IOException {
		return Files.readAllLines(Paths.get(path));
	}

	public boolean hasFile(String path) {
		return Files.exists(Paths.get(path));
	}
}
