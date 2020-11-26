# first stage
FROM hseeberger/scala-sbt:11.0.6_1.3.9_2.13.1 AS build

RUN mkdir -p /usr/crw
WORKDIR /usr/crw

COPY . /usr/crw

RUN sbt buildOlx

# second stage
FROM openjdk:8-jre-alpine3.9

RUN mkdir -p /usr/src/crw
WORKDIR /usr/src/crw

COPY --from=build /usr/crw/bin/*.*  /usr/src/crw/

EXPOSE 8080

CMD ["java", "-jar", "olx.jar"]

