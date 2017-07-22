# Micrometer ClickHouse Exporter

This artifact provides bridge between `micrometer` in spring applications and [ClickHouse](http://clickhouse.yandex) database.

# Getting started
## ClickHouse
It's easy to start ClickHouse database via docker:
```bash
docker run --name=clickhouse -p 8123:8123 yandex/clickhouse-server
```
or docker-compose:
```yaml
version: '3'
services: 
  clickhouse:
    image: yandex/clickhouse-server
    ports:
      - "8123:8123"
    restart: always
```

## Application
- For `Gradle` put following to your `build.gradle` file:
```groovy
dependencies {
    compile('com.github.camelion:micrometer-clickhouse-exporter:0.7.0.BUILD-SNAPSHOT')
}
``` 

 - or if you are `Maven` ninja-user, add next to your `pom.xml`:
```xml
<dependencies>
    <dependency>
        <groupId>com.github.camelion</groupId>
        <artifcatId>micrometer-clickhouse-exporter</artifcatId>
        <version>0.7.0.BUILD-SNAPSHOT</version>
    </dependency>
</dependencies>
```

- Then configure connection properties and table/instance properties for clickhouse server in `application.yml` or `application.properties`  file using following settings 
`application.yml`:
```yaml
clickhouse:
  metrics:
    table: my_awesome_metrics_table
    instance-id: "and some instanceId"
    datasource:
      url: jdbc:clickhouse://localhost:8123/default
      username: default
      password: ""
```

And last but not least is configuration of metrics measurement rate
```properties
# interval for 10 seconds
clickhouse.metrics.step=PT10S
```

That's all, `micrometer-clickhouse-exporter` will create necessary tables on next application launch and then you can register all metrics with `MeterRegistry` provided by `micrometer-core` module

Than you can setup ClickHouse as backend datasource for [Grafana](https://grafana.com) in conjunction with [clickhouse-datasource plugin](https://github.com/Vertamedia/clickhouse-grafana) provided by [Vertamedia](https://github.com/Vertamedia).

Simple example:
![grafana screen](https://github.com/Camelion/spring-clickhouse-metrics/blob/master/grafana.jpg)

### Sample application
Start infrastructure services (ClickHouse, Grafana) with simple `docker-compose up` command
and go to [http://localhost:3000](http://localhost:3000) in your browser.

Login as `admin/admin` into Grafana dashboard.

Then start as many application from `micrometer-clickhouse-spring/src/samples` as you want (don't forget to change instance-id for every new application)

And last, go to [http://localhost:3000/dashboard/db/sample-clickhouse-metrics-application?orgId=1](http://localhost:3000/dashboard/db/sample-clickhouse-metrics-application?orgId=1)
and enjoy metrics for you application like follow:

#### JVM Metrics
![jvm metrics](https://github.com/Camelion/spring-clickhouse-metrics/blob/master/sample/jvm_metrics.png)
####  Some application metrics
![app metrics](https://github.com/Camelion/spring-clickhouse-metrics/blob/master/sample/app_metrics.png)

**Enjoy and build better applications at easy.**