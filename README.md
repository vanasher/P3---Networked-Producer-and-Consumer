# P3---Networked-Producer-and-Consumer

`chmod +x mvnw`
`./mvnw compile dependency:copy-dependencies`

`mvnw.cmd compile`

`java -cp "target/classes;target/dependency/*" producer.ProducerMain`

`java -Xmx4g -cp "target/classes;target/dependency/*" consumer.ConsumerMain`