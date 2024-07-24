package com.rsmaxwell.diaries.response;

import java.sql.Connection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Context {

	private Connection db;
}
