FROM tomcat:9-jre8
MAINTAINER Mircea Dobreanu (github.com/mcdobr)
CMD ["catalina.sh", "run"]
COPY ./target/scraper.war $CATALINA_HOME/webapps
