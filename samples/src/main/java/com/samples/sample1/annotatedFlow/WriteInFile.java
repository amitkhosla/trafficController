package com.samples.sample1.annotatedFlow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.inject.Named;

@Named
public class WriteInFile {
	public void writeFile(String path, String data) throws IOException {
		Files.write(Paths.get(path), data.getBytes());
	}
}
