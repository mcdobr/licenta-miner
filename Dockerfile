FROM tomcat:9-jre8
MAINTAINER Mircea Dobreanu (github.com/mcdobr)
CMD ["catalina.sh", "run"]
COPY ./target/scraper.war $CATALINA_HOME/webapps
COPY ./bookworm-751eb5181a7d.json /opt
ENV GOOGLE_APPLICATION_CREDENTIALS /opt/bookworm-751eb5181a7d.json
