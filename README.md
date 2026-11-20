# P3---Networked-Producer-and-Consumer

**Compiling on Windows:**

If compiling for the first time: `mvnw compile dependency:copy-dependencies`

Otherwise: `mvnw.cmd compile`

**Compiling on Ubuntu:**

`chmod +x mvnw`

`./mvnw compile dependency:copy-dependencies`

**Running on Windows:**

Producer: `java -cp "target/classes;target/dependency/*" producer.ProducerMain`

Consumer: `java -Xmx4g -cp "target/classes;target/dependency/*" consumer.ConsumerMain`

**Running on Ubuntu:**

Producer: `java -cp "target/classes:target/dependency/*" producer.ProducerMain`

Consumer: `java -Xmx4g -cp "target/classes:target/dependency/*" consumer.ConsumerMain`