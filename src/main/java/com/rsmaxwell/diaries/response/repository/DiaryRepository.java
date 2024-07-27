package com.rsmaxwell.diaries.response.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.rsmaxwell.diaries.response.model.Diary;

public interface DiaryRepository extends CrudRepository<Diary, Long> {

	List<Diary> findByPath(String path);

	Diary findById(long id);
}