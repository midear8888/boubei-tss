rm -rf temp
rm -rf upload
rm -rf src/main/webapp/upload
rm -rf src/main/webapp/uploadFile

# release for demo.boubei.com
mvn clean install -Pdev -Dmaven.test.skip=true