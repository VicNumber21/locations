# FROM eclipse-temurin:11 as jre-build
FROM eclipse-temurin:11-jdk-alpine as jre-build
RUN $JAVA_HOME/bin/jlink \
  --add-modules java.base,jdk.unsupported,java.management \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress=2 \
  --output /javaruntime


FROM alpine
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
ENV LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_ALL=en_US.UTF-8
COPY --from=jre-build /javaruntime $JAVA_HOME

COPY target/lib/* /opt/app/lib/
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/opt/app/lib/*", "com.vportnov.locations.api.App"]