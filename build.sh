export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=1G"

nohup ./build/mvn -U -Phive -Phive-thriftserver -Phadoop-2.6 -Pyarn -DskipTests --settings=/Users/Ian1/.m2/settings-empty.xml clean install > mvn.log 2>&1 &
