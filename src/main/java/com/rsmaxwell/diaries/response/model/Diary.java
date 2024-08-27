package com.rsmaxwell.diaries.response.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "diary")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Diary {

	@Id
	@NonNull
	private String path;

}
