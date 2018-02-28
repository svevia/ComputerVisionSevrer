FROM openjdk:8

RUN mkdir -p /opt/local
WORKDIR "/opt/local"

COPY target/application.jar /opt/local

RUN mkdir -p /opt/local/etc/classifiers && mkdir -p /opt/local/etc/refs/
COPY etc/refs/ /opt/local/etc/refs

USER www

VOLUME ["/opt/local/etc"]

CMD ["java", "-jar", "/opt/local/application.jar"]

