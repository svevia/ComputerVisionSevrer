FROM openjdk:8

RUN mkdir -p /opt/local
WORKDIR "/opt/local"

COPY target/application.jar /opt/local

RUN mkdir -p /opt/local/etc/classifiers && mkdir -p /opt/local/etc/refs/ &&\
    groupadd --gid 3000 www && useradd --gid 3000 --uid 3000 www && chown www:www -R /opt/local
COPY etc/refs/ /opt/local/etc/refs
COPY etc/services

USER www

VOLUME ["/opt/local/etc"]

CMD ["java", "-jar", "/opt/local/application.jar"]

HEALTHCHECK CMD curl --fail http://localhost:8080 || exit 1