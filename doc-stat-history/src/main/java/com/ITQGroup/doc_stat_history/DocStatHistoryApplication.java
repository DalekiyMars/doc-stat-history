package com.ITQGroup.doc_stat_history;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DocStatHistoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocStatHistoryApplication.class, args);
	}

}
