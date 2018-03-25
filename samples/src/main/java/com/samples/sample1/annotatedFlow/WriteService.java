package com.samples.sample1.annotatedFlow;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class WriteService {
	@Inject
	WriteInFile writer;
	
	public void writeInFile(Long id, Map<String,String> data) throws IOException {
		StringBuilder sb = new StringBuilder();
		data.entrySet().forEach(e-> {
			sb.append(e.getKey());
			String value = e.getValue();
			if (value != null) {
				sb.append("=").append(value);
			}
			sb.append("\n");
		});
		writer.writeFile(id+".txt", sb.toString());
	}
}
