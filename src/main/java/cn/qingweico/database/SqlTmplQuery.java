package cn.qingweico.database;

import cn.qingweico.convert.VarType2Class;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author zqw
 * @date 2025/11/7
 */
@Slf4j
public class SqlTmplQuery {

    private final JdbcTemplate jdbcTemplate;

    public SqlTmplQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, String objType) {

        try {
            return (T) jdbcTemplate.queryForObject(sql, VarType2Class.change2Class(objType));
        } catch (EmptyResultDataAccessException ignored) {
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
            return null;
        }

        return null;
    }


    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Object[] args, String objType) {

        try {
            return (T) jdbcTemplate.queryForObject(sql, VarType2Class.change2Class(objType), args);
        } catch (EmptyResultDataAccessException ignored) {
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
            return null;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList(String sql) throws DataAccessException {
        return (List<T>) jdbcTemplate.queryForList(sql);

    }

    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList(String sql, Object[] args) throws DataAccessException {
        return (List<T>) jdbcTemplate.queryForList(sql, args);

    }

    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList(String sql, String elmType) throws DataAccessException {
        return (List<T>) jdbcTemplate.queryForList(sql, VarType2Class.change2Class(elmType));

    }


    @SuppressWarnings("unchecked")
    public <T> List<T> queryForList(String sql, Object[] args, String elmType) throws DataAccessException {
        return (List<T>) jdbcTemplate.queryForList(sql, VarType2Class.change2Class(elmType), args);

    }


    public Map<String, Object> queryForMap(String sql) {
        try {
            return jdbcTemplate.queryForMap(sql);
        } catch (EmptyResultDataAccessException ignored) {
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return null;

    }

    public Map<String, Object> queryForMap(String sql, Object[] args) {
        try {
            return jdbcTemplate.queryForMap(sql, args);
        } catch (EmptyResultDataAccessException ignored) {
        } catch (DataAccessException e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return null;
    }

    public int update(String sql) throws DataAccessException {
        return jdbcTemplate.update(sql);
    }

    public int[] batchUpdate(String sql, List<Object[]> list) throws DataAccessException {
        return jdbcTemplate.batchUpdate(sql, list);
    }

    public int update(String sql, Object[] args) throws DataAccessException {
        return jdbcTemplate.update(sql, args);
    }

    public void execute(String sql) throws DataAccessException {
        jdbcTemplate.execute(sql);
    }
}
