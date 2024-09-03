package com.rsmaxwell.diaries.response.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "page", uniqueConstraints = { @UniqueConstraint(columnNames = { "diary_id", "name" }) })
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
public class Page {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NonNull
	@ManyToOne
	@JoinColumn(name = "diary_id")
	private Diary diary;

	@NonNull
	private String name;
}
