# Spring ClickHouse Metrics

This artifact provides bridge between `spring-boot-actuator` metrics in spring-boot 1.5.x applications and [ClickHouse](http://clickhouse.yandex) database.

# Getting started
## ClickHouse
It's easy to start clickhouse database via docker:
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
    compile('com.github.camelion:spring-clickhouse-metrics:1.5.4.BUILD-SNAPSHOT')
}
``` 

 - or, if you are `Maven` ninja-user, add next to your `pom.xml`:
```xml
<dependencies>
    <dependency>
        <groupId>com.github.camelion</groupId>
        <artifcatId>spring-clickhouse-metrics</artifcatId>
        <version>1.5.4.BUILD-SNAPSHOT</version>
    </dependency>
</dependencies>
```

- Then, configure connection properties for clickhouse server in `application.yml` or `application.properties` file using following settings 
`application.yml`:
```yaml
clickhouse:
  datasource:
    driverClassName: ru.yandex.clickhouse.ClickHouseDriver
    url: jdbc:clickhouse://localhost:8123/default
    username: default
    password: ""
```

That's all, `spring-clickhouse-metrics` will create necessary tables on next application launch, and then you can write any metrics by using default `CounterService` and `GaugeService` provided by `spring-boot-actuator` module.

Than you can use ClickHouse as backend datasource for [Grafana](https://grafana.com) in conjunction with [clickhouse-datasource plugin](https://github.com/Vertamedia/clickhouse-grafana) provided by [Vertamedia](https://github.com/Vertamedia).

Simple example:
![grafana screen](https://github.com/Camelion/spring-clickhouse-metrics/blob/master/grafana.jpg)

**Enjoy and build better applications at easy.**