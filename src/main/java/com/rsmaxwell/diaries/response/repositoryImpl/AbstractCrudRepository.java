package com.rsmaxwell.diaries.response.repositoryImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.diaries.response.repository.CrudRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public abstract class AbstractCrudRepository<T, ID> implements CrudRepository<T, ID> {

	private static final Logger log = LogManager.getLogger(AbstractCrudRepository.class);

	private EntityManager entityManager;

	public AbstractCrudRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	abstract public String getTable();

	abstract public String getPrimaryKeyField();

	abstract public <S extends T> Object getPrimaryKeyValueAsString(S entity);

	abstract public String convertPrimaryKeyValueToString(ID id);

	abstract public <S extends T> void setPrimaryKeyValue(S entity, Object value);

	abstract public List<String> getFields();

	abstract public <S extends T> List<String> getValues(S entity);

	abstract public T getObjectFromResult(Object[] result);

	public long count() {
		String sql = String.format("select count(*) from %s", getTable());
		Query query = entityManager.createNativeQuery(sql);
		Object object = query.getSingleResult();
		return ((Number) object).longValue();
	}

	public void delete(T entity) {
		String sql = String.format("delete from %s where %s = %s", getTable(), getPrimaryKeyField(), getPrimaryKeyValueAsString(entity));
		Query query = entityManager.createNativeQuery(sql);
		int count = query.executeUpdate();
		log.info(String.format("deleteAll --> count: %d", count));
	}

	public void deleteAll() {
		String sql = String.format("delete from %s", getTable());
		Query query = entityManager.createNativeQuery(sql);
		int count = query.executeUpdate();
		log.info(String.format("deleteAll --> count: %d", count));
	}

	public void deleteAll(Iterable<? extends T> entities) {
		for (T entity : entities) {
			delete(entity);
		}
	}

	public void deleteAllById(Iterable<? extends ID> ids) {
		for (ID id : ids) {
			deleteById(id);
		}
	}

	public void deleteById(ID id) {
		String sql = String.format("delete from %s where %s = %s", getTable(), getPrimaryKeyField(), convertPrimaryKeyValueToString(id));
		log.info(String.format("sql: %s", sql));

		Query query = entityManager.createNativeQuery(sql);
		int count = query.executeUpdate();
		log.info(String.format("deleteById --> count: %d", count));
	}

	public boolean existsById(ID id) {
		String sql = String.format("select exists(select 1 from %s where %s = %s)", getTable(), getPrimaryKeyField(), convertPrimaryKeyValueToString(id));
		log.debug(String.format("sql: %s", sql));

		Query query = entityManager.createNativeQuery(sql);
		return (Boolean) query.getSingleResult();
	}

	@SuppressWarnings("unchecked")
	public Iterable<T> findAll() {

		StringBuffer sql = new StringBuffer();
		sql.append("select ");
		sql.append(getPrimaryKeyField());

		for (String field : getFields()) {
			sql.append(", ");
			sql.append(field);
		}

		sql.append(" from ");
		sql.append(getTable());

		Query query = entityManager.createNativeQuery(sql.toString());
		List<Object[]> resultList = query.getResultList();

		List<T> list = new ArrayList<T>();
		for (Object[] result : resultList) {
			T x = getObjectFromResult(result);
			list.add(x);
		}

		return list;
	}

	@SuppressWarnings("unchecked")
	public Iterable<T> findAllById(Iterable<ID> ids) {

		List<T> list = new ArrayList<T>();
		for (ID id : ids) {

			StringBuffer sql = new StringBuffer();
			sql.append("select ");
			sql.append(getPrimaryKeyField());

			for (String field : getFields()) {
				sql.append(", ");
				sql.append(field);
			}

			sql.append(" from ");
			sql.append(getTable());
			sql.append(" where ");
			sql.append(getPrimaryKeyField());
			sql.append(" = ");
			sql.append(convertPrimaryKeyValueToString(id));

			Query query = entityManager.createNativeQuery(sql.toString());
			List<Object[]> resultList = query.getResultList();

			for (Object[] result : resultList) {
				T x = getObjectFromResult(result);
				list.add(x);
			}
		}

		return list;
	}

	@SuppressWarnings("unchecked")
	public Optional<T> findById(ID id) {

		List<T> list = new ArrayList<T>();

		StringBuffer sql = new StringBuffer();
		sql.append("select ");
		sql.append(getPrimaryKeyField());

		for (String field : getFields()) {
			sql.append(", ");
			sql.append(field);
		}

		sql.append(" from ");
		sql.append(getTable());
		sql.append(" where ");
		sql.append(getPrimaryKeyField());
		sql.append(" = ");
		sql.append(convertPrimaryKeyValueToString(id));

		Query query = entityManager.createNativeQuery(sql.toString());
		List<Object[]> resultList = query.getResultList();

		for (Object[] result : resultList) {
			T x = getObjectFromResult(result);
			list.add(x);
		}

		if (list.size() <= 0) {
			return Optional.empty();
		}

		T item = list.get(0);
		return Optional.of(item);
	}

	public <S extends T> S save(S entity) {

		String separator = "";
		StringBuffer fieldsBuffer = new StringBuffer();
		for (String field : getFields()) {
			fieldsBuffer.append(separator);
			fieldsBuffer.append(field);
			separator = ", ";
		}

		separator = "";
		StringBuffer valuesBuffer = new StringBuffer();
		for (String value : getValues(entity)) {
			valuesBuffer.append(separator);
			valuesBuffer.append("'");
			valuesBuffer.append(value);
			valuesBuffer.append("'");
			separator = ", ";
		}

		String sql = String.format("insert into %s ( %s ) values ( %s ) returning %s", getTable(), fieldsBuffer, valuesBuffer, getPrimaryKeyField());
		Query query = entityManager.createNativeQuery(sql);
		Object value = query.getSingleResult();
		log.info(String.format("save --> %s: %s", getPrimaryKeyField(), value.toString()));

		setPrimaryKeyValue(entity, value);

		return entity;
	}

	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		List<S> list = new ArrayList<S>();
		for (S entity : entities) {
			list.add(save(entity));
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getResultList(String sql) {
		Query query = entityManager.createNativeQuery(sql);
		return query.getResultList();
	}

	public Optional<T> findByField(String fieldName, String fieldValue) {

		List<T> list = new ArrayList<T>();

		StringBuffer sql = new StringBuffer();
		sql.append("select ");
		sql.append(getPrimaryKeyField());

		for (String field : getFields()) {
			sql.append(", ");
			sql.append(field);
		}

		sql.append(" from ");
		sql.append(getTable());
		sql.append(" where ");
		sql.append(fieldName);
		sql.append(" = ");
		sql.append("'");
		sql.append(fieldValue);
		sql.append("'");

		List<Object[]> resultList = getResultList(sql.toString());

		for (Object[] result : resultList) {
			T x = getObjectFromResult(result);
			list.add(x);
		}

		if (list.size() <= 0) {
			return Optional.empty();
		}

		T item = list.get(0);
		return Optional.of(item);
	}
}
