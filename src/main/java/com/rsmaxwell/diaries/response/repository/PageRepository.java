package com.rsmaxwell.diaries.response.repository;

import java.util.Optional;

import com.rsmaxwell.diaries.response.model.Page;

public interface PageRepository extends CrudRepository<Page, Long> {

	Optional<Page> findByPath(String path);

}