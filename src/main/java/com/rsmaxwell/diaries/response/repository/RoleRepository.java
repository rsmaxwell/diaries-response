package com.rsmaxwell.diaries.response.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.rsmaxwell.diaries.response.model.Role;

public interface RoleRepository extends CrudRepository<Role, Long> {

	List<Role> findByName(String name);

	Role findById(long id);
}