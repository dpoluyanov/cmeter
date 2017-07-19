Start infrastructure services (ClickHouse, Grafana) with simple `docker-compose up` command
and go to [http://localhost:3000](http://localhost:3000) in your browser.

Login as `admin/admin` into Grafana dashboard.

Then start as many application as you want with following command (don't forget to change instance-id for every new application)
```bash
./gradlew bootRun -Dclickhouse.metrics.instance-id=app1
```

And last, go to [http://localhost:3000/dashboard/db/sample-clickhouse-metrics-application?orgId=1](http://localhost:3000/dashboard/db/sample-clickhouse-metrics-application?orgId=1)
and enjoy metrics for you application like follow:

#### JVM Metrics
![jvm metrics](https://github.com/Camelion/spring-clickhouse-metrics/blob/master/sample/jvm_metrics.png)
####  Some application metrics
![app metrics](https://github.com/Camelion/spring-clickhouse-metrics/blob/master/sample/app_metrics.png)
