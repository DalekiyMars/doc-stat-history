package com.ITQGroup.doc_stat_history;

import org.springframework.boot.SpringApplication;

public class TestDocStatHistoryApplication {

	public static void main(String[] args) {
		SpringApplication.from(DocStatHistoryApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
