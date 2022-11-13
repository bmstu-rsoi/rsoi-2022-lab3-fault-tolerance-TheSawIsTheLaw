FROM adoptopenjdk/openjdk11
ARG JAR_FILE=/build/libs/*.jar
COPY $JAR_FILE payment.jar
ENTRYPOINT ["java", "-jar", "/payment.jar"]