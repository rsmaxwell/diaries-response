package com.rsmaxwell.diaries.response.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.rsmaxwell.diaries.response.model.Image;

public interface ImageRepository extends CrudRepository<Image, Long> {

	List<Image> findBypath(String firstName);

	Image findById(long id);
}