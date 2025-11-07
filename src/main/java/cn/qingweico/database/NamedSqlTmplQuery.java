package cn.qingweico.database;

import cn.qingweico.convert.VarType2Class;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author zqw
 * @date 2025/11/7
 */
@Slf4j
@Service
public class NamedSqlTmplQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;


    public NamedSqlTmplQuery(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Map<String, Object> paramsMap, String objType) {
        try {
            return (T) jdbcTemplate.queryForObject(sql, paramsMap, VarType2Class.change2Class(objType));
        } catch (IncorrectResultSizeDataAccessException ignored) {
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, MapSqlParameterSource mapSqlParameterSource, String objType) {
        try {
            return (T) jdbcTemplate.queryForObject(sql, mapSqlParameterSource, VarType2Class.change2Class(objType));
        } catch (IncorrectResultSizeDataAccessException ignored) {
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList(String sql, Map<String, Object> paramsMap) {
        try {
            return (List<T>) jdbcTemplate.queryForList(sql, paramsMap);
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList(String sql, MapSqlParameterSource mapSqlParameterSource) {
        try {
            return (List<T>) jdbcTemplate.queryForList(sql, mapSqlParameterSource);
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList(String sql, Map<String, Object> paramsMap, String objType) {
        try {
            return (List<T>) jdbcTemplate.queryForList(sql, paramsMap, VarType2Class.change2Class(objType));
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList(String sql, MapSqlParameterSource mapSqlParameterSource, String objType) {
        try {
            return (List<T>) jdbcTemplate.queryForList(sql, mapSqlParameterSource, VarType2Class.change2Class(objType));
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public Map<String, Object> queryForMap(String sql, Map<String, Object> paramsMap) {
        try {
            return jdbcTemplate.queryForMap(sql, paramsMap);
        } catch (IncorrectResultSizeDataAccessException ignored) {
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public Map<String, Object> queryForMap(String sql, MapSqlParameterSource mapSqlParameterSource) {
        try {
            return jdbcTemplate.queryForMap(sql, mapSqlParameterSource);
        } catch (IncorrectResultSizeDataAccessException ignored) {
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public int update(String sql, Map<String, Object> paramsMap) {
        return jdbcTemplate.update(sql, paramsMap);
    }

    public int update(String sql, MapSqlParameterSource mapSqlParameterSource) {
        return jdbcTemplate.update(sql, mapSqlParameterSource);
    }
}
