# P3---Networked-Producer-and-Consumer

## Tools Used

| Category | Tool 
|---|---|
| Programming language | Java 21 (JDK 17 or higher works) |
| Network communication | gRPC 1.68 + Protocol Buffers |
| Build tool | Maven, through the Maven Wrapper (`mvnw`, `mvnw.cmd`) |
| GUI | Browser page (HTML, CSS, JavaScript) |
| Virtual machine | Oracle VirtualBox |
| VM operating system | Ubuntu Desktop |

## Compiling on Windows:

If compiling for the first time: `mvnw compile dependency:copy-dependencies`

Otherwise: `mvnw.cmd compile`

## Compiling on Ubuntu:

`chmod +x mvnw`

`./mvnw compile dependency:copy-dependencies`

## Running on Windows:

Producer: `java -cp "target/classes;target/dependency/*" producer.ProducerMain`

Consumer: `java -Xmx4g -cp "target/classes;target/dependency/*" consumer.ConsumerMain`

## Running on Ubuntu:

Producer: `java -cp "target/classes:target/dependency/*" producer.ProducerMain`

Consumer: `java -Xmx4g -cp "target/classes:target/dependency/*" consumer.ConsumerMain`