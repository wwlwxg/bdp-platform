SELECT if(number % 2, tuple(0, 'Hello'), tuple(1, 'World')) AS x, count() FROM (SELECT number FROM system.numbers LIMIT 10) GROUP BY if(number % 2, tuple(0, 'Hello'), tuple(1, 'World')) ORDER BY x;
SELECT if(0, tuple(0), tuple(1)) AS x GROUP BY if(0, tuple(0), tuple(1));
