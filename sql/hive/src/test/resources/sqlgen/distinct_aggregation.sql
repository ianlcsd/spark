-- This file is automatically generated by LogicalPlanToSQLSuite.
SELECT COUNT(DISTINCT id) FROM parquet_t0
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `count(DISTINCT id)` FROM (SELECT count(DISTINCT `gen_attr_1`) AS `gen_attr_0` FROM (SELECT `id` AS `gen_attr_1` FROM `default`.`parquet_t0`) AS gen_subquery_0) AS gen_subquery_1